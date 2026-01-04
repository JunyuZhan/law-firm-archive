package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateDepartmentCommand;
import com.lawfirm.application.system.command.UpdateDepartmentCommand;
import com.lawfirm.application.system.dto.DepartmentDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentAppService {

    private final DepartmentRepository departmentRepository;
    private final com.lawfirm.infrastructure.persistence.mapper.UserMapper userMapper;
    private final UserRepository userRepository;

    /**
     * 获取部门树
     */
    public List<DepartmentDTO> getDepartmentTree() {
        List<Department> allDepts = departmentRepository.findAll();
        
        // 转换为DTO
        List<DepartmentDTO> dtoList = allDepts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        // 构建树形结构
        return buildTree(dtoList);
    }

    /**
     * 获取部门列表（平铺）
     */
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取部门详情
     */
    public DepartmentDTO getDepartmentById(Long id) {
        Department dept = departmentRepository.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return toDTO(dept);
    }

    /**
     * 创建部门
     */
    @Transactional
    public DepartmentDTO createDepartment(CreateDepartmentCommand command) {
        // 验证父部门
        if (command.getParentId() != null && command.getParentId() > 0) {
            Department parent = departmentRepository.getById(command.getParentId());
            if (parent == null) {
                throw new BusinessException("父部门不存在");
            }
        }
        
        Department dept = Department.builder()
                .name(command.getName())
                .parentId(command.getParentId() != null ? command.getParentId() : 0L)
                .sortOrder(command.getSortOrder())
                .leaderId(command.getLeaderId())
                .status("ACTIVE")
                .build();
        
        departmentRepository.save(dept);
        
        log.info("创建部门成功: {}", dept.getName());
        return toDTO(dept);
    }

    /**
     * 更新部门
     */
    @Transactional
    public DepartmentDTO updateDepartment(UpdateDepartmentCommand command) {
        Department dept = departmentRepository.getById(command.getId());
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        
        // 不能将部门设置为自己的子部门
        if (command.getParentId() != null && command.getParentId().equals(command.getId())) {
            throw new BusinessException("不能将部门设置为自己的子部门");
        }
        
        if (StringUtils.hasText(command.getName())) {
            dept.setName(command.getName());
        }
        if (command.getParentId() != null) {
            dept.setParentId(command.getParentId());
        }
        if (command.getSortOrder() != null) {
            dept.setSortOrder(command.getSortOrder());
        }
        if (command.getLeaderId() != null) {
            dept.setLeaderId(command.getLeaderId());
        }
        if (StringUtils.hasText(command.getStatus())) {
            dept.setStatus(command.getStatus());
        }
        
        departmentRepository.updateById(dept);
        
        log.info("更新部门成功: {}", dept.getName());
        return toDTO(dept);
    }

    /**
     * 删除部门
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        
        // 检查是否有子部门
        if (departmentRepository.hasChildren(id)) {
            throw new BusinessException("该部门下存在子部门，无法删除");
        }
        
        // 检查是否有用户
        int userCount = userMapper.countByDepartmentId(id);
        if (userCount > 0) {
            throw new BusinessException("该部门下存在用户，无法删除");
        }
        
        departmentRepository.removeById(id);
        
        log.info("删除部门成功: {}", dept.getName());
    }

    /**
     * 设置部门负责人
     */
    @Transactional
    public void setLeader(Long deptId, Long leaderId) {
        Department dept = departmentRepository.getById(deptId);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        
        if (leaderId != null) {
            User leader = userRepository.getById(leaderId);
            if (leader == null) {
                throw new BusinessException("负责人不存在");
            }
        }
        
        dept.setLeaderId(leaderId);
        departmentRepository.updateById(dept);
        
        log.info("设置部门负责人: deptId={}, leaderId={}", deptId, leaderId);
    }

    /**
     * 构建树形结构
     */
    private List<DepartmentDTO> buildTree(List<DepartmentDTO> dtoList) {
        Map<Long, DepartmentDTO> dtoMap = dtoList.stream()
                .collect(Collectors.toMap(DepartmentDTO::getId, d -> d));
        
        List<DepartmentDTO> roots = new ArrayList<>();
        
        for (DepartmentDTO dto : dtoList) {
            if (dto.getParentId() == null || dto.getParentId() == 0) {
                roots.add(dto);
            } else {
                DepartmentDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }
        
        return roots;
    }

    private DepartmentDTO toDTO(Department dept) {
        DepartmentDTO dto = DepartmentDTO.builder()
                .id(dept.getId())
                .name(dept.getName())
                .parentId(dept.getParentId())
                .sortOrder(dept.getSortOrder())
                .leaderId(dept.getLeaderId())
                .status(dept.getStatus())
                .createdAt(dept.getCreatedAt())
                .updatedAt(dept.getUpdatedAt())
                .build();
        
        // 查询负责人名称
        if (dept.getLeaderId() != null) {
            User leader = userRepository.getById(dept.getLeaderId());
            if (leader != null) {
                dto.setLeaderName(leader.getRealName());
            }
        }
        
        return dto;
    }
}
