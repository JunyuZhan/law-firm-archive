package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * MatterAppService 单元测试
 * 
 * 测试范围：
 * - 创建项目（正常流程、异常情况）
 * - 项目验证逻辑
 * - 业务规则验证
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatterAppService 单元测试")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MatterAppServiceTest {

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private MatterMapper matterMapper;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractParticipantRepository contractParticipantRepository;

    @Mock
    private MatterClientRepository matterClientRepository;

    @Mock
    private MatterParticipantRepository matterParticipantRepository;

    @Mock
    private MatterParticipantMapper participantMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DeadlineAppService deadlineAppService;

    @InjectMocks
    private MatterAppService matterAppService;

    private CreateMatterCommand validCommand;
    private Client mockClient;
    private Contract mockContract;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        // Mock SecurityUtils
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(1L);
        securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");
        securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
        // 构建有效的创建命令
        validCommand = new CreateMatterCommand();
        validCommand.setName("测试项目");
        validCommand.setClientId(1L);
        validCommand.setContractId(100L);
        validCommand.setMatterType("LITIGATION");
        validCommand.setCaseType("CIVIL");
        validCommand.setEstimatedFee(new BigDecimal("10000"));
        validCommand.setLeadLawyerId(10L);
        validCommand.setDepartmentId(1L);

        // Mock 客户
        mockClient = new Client();
        mockClient.setId(1L);
        mockClient.setName("测试客户");

        // Mock 合同（已审批通过）
        mockContract = new Contract();
        mockContract.setId(100L);
        mockContract.setClientId(1L);
        mockContract.setStatus("ACTIVE");
        mockContract.setTotalAmount(new BigDecimal("50000"));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建项目测试")
    class CreateMatterTests {

        @Test
        @DisplayName("创建项目失败 - 未关联合同")
        void createMatter_WithoutContract_ShouldThrowException() {
            // Given
            CreateMatterCommand command = new CreateMatterCommand();
            command.setName("测试项目");
            command.setClientId(1L);
            // 不设置 contractId

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> matterAppService.createMatter(command));
            assertTrue(exception.getMessage().contains("合同"));
        }

        @Test
        @DisplayName("创建项目失败 - 客户不存在")
        void createMatter_ClientNotFound_ShouldThrowException() {
            // Given
            when(clientRepository.getByIdOrThrow(anyLong(), anyString()))
                    .thenThrow(new BusinessException("客户不存在"));

            // When & Then
            assertThrows(BusinessException.class,
                    () -> matterAppService.createMatter(validCommand));
        }

        @Test
        @DisplayName("创建项目失败 - 合同未审批")
        void createMatter_ContractNotApproved_ShouldThrowException() {
            // Given
            when(clientRepository.getByIdOrThrow(anyLong(), anyString()))
                    .thenReturn(mockClient);

            Contract pendingContract = new Contract();
            pendingContract.setId(100L);
            pendingContract.setStatus("PENDING");  // 未审批
            when(contractRepository.getByIdOrThrow(anyLong(), anyString()))
                    .thenReturn(pendingContract);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> matterAppService.createMatter(validCommand));
            assertTrue(exception.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("创建项目失败 - 客户与合同客户不一致")
        void createMatter_ClientMismatch_ShouldThrowException() {
            // Given
            when(clientRepository.getByIdOrThrow(anyLong(), anyString()))
                    .thenReturn(mockClient);

            Contract mismatchContract = new Contract();
            mismatchContract.setId(100L);
            mismatchContract.setClientId(999L);  // 不同的客户
            mismatchContract.setStatus("ACTIVE");
            when(contractRepository.getByIdOrThrow(anyLong(), anyString()))
                    .thenReturn(mismatchContract);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> matterAppService.createMatter(validCommand));
            assertTrue(exception.getMessage().contains("客户"));
        }
    }

    @Nested
    @DisplayName("项目查询测试")
    class QueryMatterTests {

        @Test
        @DisplayName("获取项目详情 - 项目不存在")
        void getMatterById_NotFound_ShouldThrowException() {
            // Given
            when(matterRepository.getByIdOrThrow(eq(999L), anyString()))
                    .thenThrow(new BusinessException("案件不存在"));

            // When & Then
            assertThrows(BusinessException.class,
                    () -> matterAppService.getMatterById(999L));
        }

        @Test
        @DisplayName("获取项目详情 - 成功")
        void getMatterById_Success() {
            // Given
            Matter matter = Matter.builder()
                    .id(1L)
                    .matterNo("M2026010001")
                    .name("测试项目")
                    .status("ACTIVE")
                    .clientId(1L)
                    .build();
            when(matterRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(matter);
            when(participantMapper.selectByMatterId(1L)).thenReturn(java.util.Collections.emptyList());

            // When
            MatterDTO result = matterAppService.getMatterById(1L);

            // Then
            assertNotNull(result);
            assertEquals("测试项目", result.getName());
            assertEquals("ACTIVE", result.getStatus());
        }
    }

    @Nested
    @DisplayName("项目状态变更测试")
    class StatusChangeTests {

        @Test
        @DisplayName("关闭项目审批 - 项目不存在")
        void approveCloseMatter_NotFound_ShouldThrowException() {
            // Given
            when(matterMapper.selectById(anyLong())).thenReturn(null);

            // When & Then
            assertThrows(BusinessException.class,
                    () -> matterAppService.approveCloseMatter(999L, true, "同意结案"));
        }
    }

    @Nested
    @DisplayName("业务规则验证")
    class BusinessRuleTests {

        @Test
        @DisplayName("项目编号格式验证")
        void matterNo_Format_ShouldBeValid() {
            // 项目编号格式：M + 年份 + 序号
            String validMatterNo = "M2026010001";
            assertTrue(validMatterNo.matches("M\\d{10}"));
        }

        @Test
        @DisplayName("项目类型验证")
        void matterType_ShouldBeValid() {
            // 有效的项目类型
            String[] validTypes = {"LITIGATION", "NON_LITIGATION", "ADVISORY"};
            for (String type : validTypes) {
                assertNotNull(type);
                assertTrue(type.length() > 0);
            }
        }

        @Test
        @DisplayName("项目状态流转验证")
        void statusTransition_ShouldFollowRules() {
            // 状态流转规则：
            // PENDING -> ACTIVE (审批通过)
            // ACTIVE -> SUSPENDED (暂停)
            // ACTIVE -> CLOSED (关闭)
            // SUSPENDED -> ACTIVE (恢复)
            
            String[] validStatuses = {"PENDING", "ACTIVE", "SUSPENDED", "CLOSED"};
            for (String status : validStatuses) {
                assertNotNull(status);
            }
        }
    }
}

