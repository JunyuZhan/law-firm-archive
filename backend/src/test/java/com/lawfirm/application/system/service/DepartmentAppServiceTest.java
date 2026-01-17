package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateDepartmentCommand;
import com.lawfirm.application.system.command.UpdateDepartmentCommand;
import com.lawfirm.application.system.dto.DepartmentDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DepartmentAppService 单元测试
 *
 * 测试部门应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentAppService 部门服务测试")
class DepartmentAppServiceTest {

    private static final Long TEST_DEPT_ID = 10L;
    private static final Long TEST_USER_ID = 100L;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentAppService departmentAppService;

    @Nested
    @DisplayName("获取部门树测试")
    class GetDepartmentTreeTests {

        @Test
        @DisplayName("应该构建部门树结构")
        void getDepartmentTree_shouldBuildTreeStructure() {
            // Given
            Department rootDept = createTestDepartment(1L, "总部", 0L, 1);
            Department childDept1 = createTestDepartment(2L, "财务部", 1L, 2);
            Department childDept2 = createTestDepartment(3L, "法务部", 1L, 3);
            Department grandChildDept = createTestDepartment(4L, "会计组", 2L, 4);

            when(departmentRepository.findAll()).thenReturn(List.of(rootDept, childDept1, childDept2, grandChildDept));
            // Note: userRepository.listByIds 不会被调用，因为测试部门没有设置leaderId

            // When
            List<DepartmentDTO> result = departmentAppService.getDepartmentTree();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("总部");
            assertThat(result.get(0).getChildren()).hasSize(2);

            List<DepartmentDTO> children = result.get(0).getChildren();
            assertThat(children.get(0).getName()).isEqualTo("财务部");
            assertThat(children.get(0).getChildren()).hasSize(1);
            assertThat(children.get(0).getChildren().get(0).getName()).isEqualTo("会计组");
            assertThat(children.get(1).getName()).isEqualTo("法务部");
        }

        @Test
        @DisplayName("空结果时应返回空列表")
        void getDepartmentTree_shouldReturnEmptyWhenNoDepartments() {
            // Given
            when(departmentRepository.findAll()).thenReturn(new ArrayList<>());

            // When
            List<DepartmentDTO> result = departmentAppService.getDepartmentTree();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该加载部门负责人信息")
        void getDepartmentTree_shouldLoadLeaderInfo() {
            // Given
            Department dept = createTestDepartment(1L, "财务部", 0L, 1);
            dept.setLeaderId(TEST_USER_ID);

            User leader = createTestUser(TEST_USER_ID, "张三", "zhangsan");

            when(departmentRepository.findAll()).thenReturn(List.of(dept));
            when(userRepository.listByIds(anyList())).thenReturn(List.of(leader));

            // When
            List<DepartmentDTO> result = departmentAppService.getDepartmentTree();

            // Then
            assertThat(result.get(0).getLeaderName()).isEqualTo("张三");
        }
    }

    @Nested
    @DisplayName("获取所有部门测试")
    class GetAllDepartmentsTests {

        @Test
        @DisplayName("应该返回平铺的部门列表")
        void getAllDepartments_shouldReturnFlatList() {
            // Given
            Department dept1 = createTestDepartment(1L, "财务部", 0L, 1);
            Department dept2 = createTestDepartment(2L, "法务部", 0L, 2);

            when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));
            // Note: userRepository.listByIds 不会被调用，因为测试部门没有设置leaderId

            // When
            List<DepartmentDTO> result = departmentAppService.getAllDepartments();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("财务部");
            assertThat(result.get(1).getName()).isEqualTo("法务部");
        }

        @Test
        @DisplayName("空结果时应返回空列表")
        void getAllDepartments_shouldReturnEmptyWhenNoDepartments() {
            // Given
            when(departmentRepository.findAll()).thenReturn(new ArrayList<>());

            // When
            List<DepartmentDTO> result = departmentAppService.getAllDepartments();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("获取部门详情测试")
    class GetDepartmentByIdTests {

        @Test
        @DisplayName("应该获取部门详情")
        void getDepartmentById_shouldReturnDepartment() {
            // Given
            Department dept = createTestDepartment(1L, "财务部", 0L, 1);
            when(departmentRepository.getById(1L)).thenReturn(dept);
            // Note: userRepository.getById 不会被调用，因为测试部门没有设置leaderId

            // When
            DepartmentDTO result = departmentAppService.getDepartmentById(1L);

            // Then
            assertThat(result.getName()).isEqualTo("财务部");
            assertThat(result.getParentId()).isEqualTo(0L);
        }

        @Test
        @DisplayName("部门不存在时应抛出异常")
        void getDepartmentById_shouldThrowException_whenNotFound() {
            // Given
            when(departmentRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.getDepartmentById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("部门不存在");
        }
    }

    @Nested
    @DisplayName("创建部门测试")
    class CreateDepartmentTests {

        @Test
        @DisplayName("应该成功创建顶级部门")
        void createDepartment_shouldCreateTopLevelDepartment() {
            // Given
            CreateDepartmentCommand command = new CreateDepartmentCommand();
            command.setName("新部门");
            command.setParentId(0L);
            command.setSortOrder(1);

            // Note: parentId=0L 不满足 > 0 条件，所以不会调用 getById
            when(departmentRepository.save(any(Department.class))).thenReturn(true);

            // When
            DepartmentDTO result = departmentAppService.createDepartment(command);

            // Then
            assertThat(result.getName()).isEqualTo("新部门");
            assertThat(result.getParentId()).isEqualTo(0L);
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(departmentRepository).save(any(Department.class));
        }

        @Test
        @DisplayName("应该成功创建子部门")
        void createDepartment_shouldCreateChildDepartment() {
            // Given
            CreateDepartmentCommand command = new CreateDepartmentCommand();
            command.setName("子部门");
            command.setParentId(TEST_DEPT_ID);
            command.setSortOrder(1);

            Department parentDept = createTestDepartment(TEST_DEPT_ID, "父部门", 0L, 1);

            when(departmentRepository.getById(TEST_DEPT_ID)).thenReturn(parentDept);
            when(departmentRepository.save(any(Department.class))).thenReturn(true);

            // When
            DepartmentDTO result = departmentAppService.createDepartment(command);

            // Then
            assertThat(result.getName()).isEqualTo("子部门");
            assertThat(result.getParentId()).isEqualTo(TEST_DEPT_ID);
        }

        @Test
        @DisplayName("父部门不存在时应抛出异常")
        void createDepartment_shouldThrowException_whenParentNotFound() {
            // Given
            CreateDepartmentCommand command = new CreateDepartmentCommand();
            command.setName("子部门");
            command.setParentId(999L);

            when(departmentRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.createDepartment(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("父部门不存在");
        }
    }

    @Nested
    @DisplayName("更新部门测试")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("应该成功更新部门")
        void updateDepartment_shouldUpdateDepartment() {
            // Given
            Department dept = createTestDepartment(1L, "旧名称", 0L, 1);

            UpdateDepartmentCommand command = new UpdateDepartmentCommand();
            command.setId(1L);
            command.setName("新名称");
            command.setSortOrder(10);

            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(departmentRepository.updateById(any(Department.class))).thenReturn(true);

            // When
            DepartmentDTO result = departmentAppService.updateDepartment(command);

            // Then
            assertThat(result.getName()).isEqualTo("新名称");
            assertThat(dept.getSortOrder()).isEqualTo(10);
            verify(departmentRepository).updateById(dept);
        }

        @Test
        @DisplayName("部门不存在时应抛出异常")
        void updateDepartment_shouldThrowException_whenNotFound() {
            // Given
            UpdateDepartmentCommand command = new UpdateDepartmentCommand();
            command.setId(999L);

            when(departmentRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.updateDepartment(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("部门不存在");
        }

        @Test
        @DisplayName("不能将部门设置为自己的子部门")
        void updateDepartment_shouldThrowException_whenSelfParent() {
            // Given
            Department dept = createTestDepartment(1L, "部门", 0L, 1);

            UpdateDepartmentCommand command = new UpdateDepartmentCommand();
            command.setId(1L);
            command.setParentId(1L); // 设置自己为父部门

            when(departmentRepository.getById(1L)).thenReturn(dept);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.updateDepartment(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("不能将部门设置为自己的子部门");
        }

        @Test
        @DisplayName("不能形成循环引用")
        void updateDepartment_shouldThrowException_whenCyclicReference() {
            // Given
            Department parent = createTestDepartment(1L, "父部门", 0L, 1);
            Department child = createTestDepartment(2L, "子部门", 1L, 2);
            Department grandChild = createTestDepartment(3L, "孙部门", 2L, 3);

            UpdateDepartmentCommand command = new UpdateDepartmentCommand();
            command.setId(1L); // 尝试将父部门的父设置为孙部门
            command.setParentId(3L);

            when(departmentRepository.getById(1L)).thenReturn(parent);
            when(departmentRepository.getById(3L)).thenReturn(grandChild);
            when(departmentRepository.getById(2L)).thenReturn(child);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.updateDepartment(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("不能形成循环引用的部门结构");
        }
    }

    @Nested
    @DisplayName("删除部门测试")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("应该成功删除部门")
        void deleteDepartment_shouldDeleteDepartment() {
            // Given
            Department dept = createTestDepartment(1L, "待删除部门", 0L, 1);
            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(departmentRepository.hasChildren(1L)).thenReturn(false);
            when(userMapper.countByDepartmentId(1L)).thenReturn(0);
            when(departmentRepository.removeById(1L)).thenReturn(true);

            // When
            departmentAppService.deleteDepartment(1L);

            // Then
            verify(departmentRepository).removeById(1L);
        }

        @Test
        @DisplayName("部门不存在时应抛出异常")
        void deleteDepartment_shouldThrowException_whenNotFound() {
            // Given
            when(departmentRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.deleteDepartment(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("部门不存在");
        }

        @Test
        @DisplayName("有子部门时应抛出异常")
        void deleteDepartment_shouldThrowException_whenHasChildren() {
            // Given
            Department dept = createTestDepartment(1L, "父部门", 0L, 1);
            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(departmentRepository.hasChildren(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.deleteDepartment(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("该部门下存在子部门，无法删除");
        }

        @Test
        @DisplayName("有用户时应抛出异常")
        void deleteDepartment_shouldThrowException_whenHasUsers() {
            // Given
            Department dept = createTestDepartment(1L, "部门", 0L, 1);
            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(departmentRepository.hasChildren(1L)).thenReturn(false);
            when(userMapper.countByDepartmentId(1L)).thenReturn(5);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.deleteDepartment(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("该部门下存在用户，无法删除");
        }
    }

    @Nested
    @DisplayName("设置负责人测试")
    class SetLeaderTests {

        @Test
        @DisplayName("应该成功设置部门负责人")
        void setLeader_shouldSetLeader() {
            // Given
            Department dept = createTestDepartment(1L, "财务部", 0L, 1);
            User user = createTestUser(TEST_USER_ID, "张三", "zhangsan");

            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(userRepository.getById(TEST_USER_ID)).thenReturn(user);
            when(departmentRepository.updateById(any(Department.class))).thenReturn(true);

            // When
            departmentAppService.setLeader(1L, TEST_USER_ID);

            // Then
            assertThat(dept.getLeaderId()).isEqualTo(TEST_USER_ID);
            verify(departmentRepository).updateById(dept);
        }

        @Test
        @DisplayName("部门不存在时应抛出异常")
        void setLeader_shouldThrowException_whenDepartmentNotFound() {
            // Given
            when(departmentRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.setLeader(999L, TEST_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("部门不存在");
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void setLeader_shouldThrowException_whenUserNotFound() {
            // Given
            Department dept = createTestDepartment(1L, "财务部", 0L, 1);
            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(userRepository.getById(999L)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> departmentAppService.setLeader(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("负责人不存在");
        }

        @Test
        @DisplayName("应该支持取消负责人")
        void setLeader_shouldSupportNullLeader() {
            // Given
            Department dept = createTestDepartment(1L, "财务部", 0L, 1);
            dept.setLeaderId(TEST_USER_ID);

            when(departmentRepository.getById(1L)).thenReturn(dept);
            when(departmentRepository.updateById(any(Department.class))).thenReturn(true);

            // When
            departmentAppService.setLeader(1L, null);

            // Then
            assertThat(dept.getLeaderId()).isNull();
            verify(departmentRepository).updateById(dept);
        }
    }

    // ========== 辅助方法 ==========

    private Department createTestDepartment(Long id, String name, Long parentId, int sortOrder) {
        return Department.builder()
                .id(id)
                .name(name)
                .parentId(parentId)
                .sortOrder(sortOrder)
                .status("ACTIVE")
                .build();
    }

    private User createTestUser(Long id, String realName, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .realName(realName)
                .status("ACTIVE")
                .build();
    }
}
