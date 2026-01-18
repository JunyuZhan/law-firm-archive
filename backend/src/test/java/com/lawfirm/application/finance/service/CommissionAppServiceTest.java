package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateCommissionRuleCommand;
import com.lawfirm.application.finance.command.ManualCalculateCommissionCommand;
import com.lawfirm.application.finance.command.UpdateCommissionRuleCommand;
import com.lawfirm.application.finance.dto.CommissionDTO;
import com.lawfirm.application.finance.dto.CommissionQueryDTO;
import com.lawfirm.application.finance.dto.CommissionRuleDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.constant.CommissionStatus;
import com.lawfirm.common.constant.PaymentStatus;
import com.lawfirm.domain.finance.entity.*;
import com.lawfirm.domain.finance.repository.*;
import com.lawfirm.infrastructure.persistence.mapper.CommissionDetailMapper;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CommissionAppService 单元测试
 * 测试提成管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionAppService 提成服务测试")
class CommissionAppServiceTest {

    private static final Long TEST_USER_ID = 1L;

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private CommissionDetailMapper commissionDetailMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractParticipantRepository contractParticipantRepository;

    @Mock
    private com.lawfirm.domain.client.repository.ClientRepository clientRepository;

    @Mock
    private com.lawfirm.domain.matter.repository.MatterRepository matterRepository;

    @InjectMocks
    private CommissionAppService commissionAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
        
        // Mock 权限检查需要的用户和角色
        User adminUser = new User();
        adminUser.setId(TEST_USER_ID);
        lenient().when(userRepository.findById(TEST_USER_ID)).thenReturn(adminUser);
        lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("ADMIN"));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("提成规则管理测试")
    class CommissionRuleTests {

        @Test
        @DisplayName("应该成功创建提成规则")
        void createCommissionRule_shouldSuccess() {
            // Given
            CreateCommissionRuleCommand command = new CreateCommissionRuleCommand();
            command.setRuleCode("RULE001");
            command.setRuleName("标准提成规则");
            command.setRuleType("STANDARD");
            command.setFirmRate(BigDecimal.valueOf(30));
            command.setLeadLawyerRate(BigDecimal.valueOf(40));
            command.setAssistLawyerRate(BigDecimal.valueOf(20));
            command.setSupportStaffRate(BigDecimal.valueOf(10));

            when(commissionRuleRepository.findByRuleCode("RULE001")).thenReturn(Optional.empty());
            lenient().doNothing().when(commissionRuleRepository).clearDefault();
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper ruleBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper.class);
            when(commissionRuleRepository.getBaseMapper()).thenReturn(ruleBaseMapper);
            when(ruleBaseMapper.insert(any(CommissionRule.class))).thenReturn(1);

            // When
            CommissionRuleDTO result = commissionAppService.createCommissionRule(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRuleCode()).isEqualTo("RULE001");
            assertThat(result.getRuleName()).isEqualTo("标准提成规则");
            verify(commissionRuleRepository.getBaseMapper()).insert(any(CommissionRule.class));
        }

        @Test
        @DisplayName("规则编码已存在应该失败")
        void createCommissionRule_shouldFail_whenCodeExists() {
            // Given
            CreateCommissionRuleCommand command = new CreateCommissionRuleCommand();
            command.setRuleCode("RULE001");

            CommissionRule existingRule = new CommissionRule();
            when(commissionRuleRepository.findByRuleCode("RULE001")).thenReturn(Optional.of(existingRule));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.createCommissionRule(command));
            assertThat(exception.getMessage()).contains("规则编码已存在");
        }

        @Test
        @DisplayName("应该成功更新提成规则")
        void updateCommissionRule_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .ruleCode("RULE001")
                    .ruleName("原名称")
                    .build();

            UpdateCommissionRuleCommand command = new UpdateCommissionRuleCommand();
            command.setRuleName("新名称");
            command.setFirmRate(BigDecimal.valueOf(35));

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);
            lenient().doNothing().when(commissionRuleRepository).clearDefault();
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper ruleBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper.class);
            when(commissionRuleRepository.getBaseMapper()).thenReturn(ruleBaseMapper);
            when(ruleBaseMapper.updateById(any(CommissionRule.class))).thenReturn(1);

            // When
            CommissionRuleDTO result = commissionAppService.updateCommissionRule(1L, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(rule.getRuleName()).isEqualTo("新名称");
            assertThat(rule.getFirmRate()).isEqualByComparingTo(BigDecimal.valueOf(35));
        }

        @Test
        @DisplayName("应该成功删除提成规则")
        void deleteCommissionRule_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .isDefault(false)
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);
            lenient().when(commissionRuleRepository.softDelete(1L)).thenReturn(true);

            // When
            commissionAppService.deleteCommissionRule(1L);

            // Then
            verify(commissionRuleRepository).softDelete(1L);
        }

        @Test
        @DisplayName("不能删除默认规则")
        void deleteCommissionRule_shouldFail_whenDefault() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .isDefault(true)
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.deleteCommissionRule(1L));
            assertThat(exception.getMessage()).contains("不能删除默认规则");
        }

        @Test
        @DisplayName("应该成功设为默认规则")
        void setDefaultRule_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);
            doNothing().when(commissionRuleRepository).setDefault(1L);

            // When
            commissionAppService.setDefaultRule(1L);

            // Then
            verify(commissionRuleRepository).setDefault(1L);
        }

        @Test
        @DisplayName("应该成功切换规则状态")
        void toggleRule_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .active(true)
                    .isDefault(false)
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);
            lenient().doNothing().when(commissionRuleRepository).clearDefault();
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper ruleBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionRuleMapper.class);
            when(commissionRuleRepository.getBaseMapper()).thenReturn(ruleBaseMapper);
            when(ruleBaseMapper.updateById(any(CommissionRule.class))).thenReturn(1);

            // When
            commissionAppService.toggleRule(1L);

            // Then
            assertThat(rule.getActive()).isFalse();
        }

        @Test
        @DisplayName("不能停用默认规则")
        void toggleRule_shouldFail_whenDefaultAndActive() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .active(true)
                    .isDefault(true)
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.toggleRule(1L));
            assertThat(exception.getMessage()).contains("不能停用默认规则");
        }
    }

    @Nested
    @DisplayName("手动计算提成测试")
    class ManualCalculateCommissionTests {

        @Test
        @DisplayName("应该成功手动计算提成")
        void manualCalculateCommission_shouldSuccess() {
            // Given
            Payment payment = Payment.builder()
                    .id(1L)
                    .feeId(100L)
                    .amount(BigDecimal.valueOf(10000))
                    .status(PaymentStatus.CONFIRMED)
                    .build();

            Fee fee = Fee.builder()
                    .id(100L)
                    .contractId(200L)
                    .matterId(300L)
                    .build();

            Contract contract = Contract.builder()
                    .id(200L)
                    .commissionRuleId(1L)
                    .build();

            ContractParticipant participant = ContractParticipant.builder()
                    .id(1L)
                    .contractId(200L)
                    .userId(10L)
                    .role("LEAD_LAWYER")
                    .commissionRate(BigDecimal.valueOf(40))
                    .build();

            User user = new User();
            user.setId(10L);
            user.setRealName("律师A");

            ManualCalculateCommissionCommand command = new ManualCalculateCommissionCommand();
            command.setPaymentId(1L);
            ManualCalculateCommissionCommand.ParticipantCommission pc = 
                    new ManualCalculateCommissionCommand.ParticipantCommission();
            pc.setParticipantId(1L);
            pc.setCommissionRate(BigDecimal.valueOf(40));
            command.setParticipants(Collections.singletonList(pc));

            when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
            when(feeRepository.getByIdOrThrow(eq(100L), anyString())).thenReturn(fee);
            when(contractRepository.findById(200L)).thenReturn(contract);
            when(commissionRepository.findByPaymentId(1L)).thenReturn(Collections.emptyList());
            when(contractParticipantRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(participant);
            when(userRepository.findById(10L)).thenReturn(user);
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            when(commissionBaseMapper.insert(any(Commission.class))).thenReturn(1);
            when(commissionDetailMapper.insert(any(CommissionDetail.class))).thenReturn(1);

            // When
            List<CommissionDTO> result = commissionAppService.manualCalculateCommission(command);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCommissionAmount()).isEqualByComparingTo(BigDecimal.valueOf(4000)); // 10000 * 40%
            verify(commissionRepository.getBaseMapper()).insert(any(Commission.class));
        }

        @Test
        @DisplayName("未确认的收款不能计算提成")
        void manualCalculateCommission_shouldFail_whenPaymentNotConfirmed() {
            // Given
            Payment payment = Payment.builder()
                    .id(1L)
                    .status(PaymentStatus.PENDING)
                    .build();

            ManualCalculateCommissionCommand command = new ManualCalculateCommissionCommand();
            command.setPaymentId(1L);

            when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.manualCalculateCommission(command));
            assertThat(exception.getMessage()).contains("已确认的收款");
        }

        @Test
        @DisplayName("没有关联合同的收款不能计算提成")
        void manualCalculateCommission_shouldFail_whenNoContract() {
            // Given
            Payment payment = Payment.builder()
                    .id(1L)
                    .feeId(100L)
                    .status(PaymentStatus.CONFIRMED)
                    .build();

            Fee fee = Fee.builder()
                    .id(100L)
                    .contractId(null) // 没有关联合同
                    .build();

            ManualCalculateCommissionCommand command = new ManualCalculateCommissionCommand();
            command.setPaymentId(1L);

            when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
            when(feeRepository.getByIdOrThrow(eq(100L), anyString())).thenReturn(fee);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.manualCalculateCommission(command));
            assertThat(exception.getMessage()).contains("没有关联合同");
        }

        @Test
        @DisplayName("已存在提成记录不能重复计算")
        void manualCalculateCommission_shouldFail_whenAlreadyExists() {
            // Given
            Payment payment = Payment.builder()
                    .id(1L)
                    .feeId(100L)
                    .status(PaymentStatus.CONFIRMED)
                    .build();

            Fee fee = Fee.builder()
                    .id(100L)
                    .contractId(200L)
                    .build();

            Contract contract = Contract.builder()
                    .id(200L)
                    .build();

            Commission existingCommission = Commission.builder()
                    .id(1L)
                    .build();

            ManualCalculateCommissionCommand command = new ManualCalculateCommissionCommand();
            command.setPaymentId(1L);

            when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
            when(feeRepository.getByIdOrThrow(eq(100L), anyString())).thenReturn(fee);
            when(contractRepository.findById(200L)).thenReturn(contract);
            when(commissionRepository.findByPaymentId(1L)).thenReturn(Collections.singletonList(existingCommission));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.manualCalculateCommission(command));
            assertThat(exception.getMessage()).contains("已存在提成记录");
        }
    }

    @Nested
    @DisplayName("查询提成规则测试")
    class QueryCommissionRuleTests {

        @Test
        @DisplayName("应该成功获取提成规则详情")
        void getCommissionRule_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .ruleCode("RULE001")
                    .ruleName("标准规则")
                    .build();

            when(commissionRuleRepository.findById(1L)).thenReturn(rule);

            // When
            CommissionRuleDTO result = commissionAppService.getCommissionRule(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRuleCode()).isEqualTo("RULE001");
        }

        @Test
        @DisplayName("规则不存在应该失败")
        void getCommissionRule_shouldFail_whenNotFound() {
            // Given
            when(commissionRuleRepository.findById(1L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.getCommissionRule(1L));
            assertThat(exception.getMessage()).contains("提成规则不存在");
        }

        @Test
        @DisplayName("应该成功查询提成规则列表")
        void listCommissionRules_shouldSuccess() {
            // Given
            CommissionRule rule = CommissionRule.builder()
                    .id(1L)
                    .ruleCode("RULE001")
                    .active(true)
                    .build();

            when(commissionRuleRepository.findActiveRules()).thenReturn(Collections.singletonList(rule));

            // When
            List<CommissionRuleDTO> result = commissionAppService.listCommissionRules();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRuleCode()).isEqualTo("RULE001");
        }
    }

    @Nested
    @DisplayName("查询提成记录测试")
    class QueryCommissionTests {

        @Test
        @DisplayName("应该成功分页查询提成记录")
        void listCommissions_shouldSuccess() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
            lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("ADMIN"));
            
            CommissionQueryDTO query = new CommissionQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            Commission commission = Commission.builder()
                    .id(1L)
                    .commissionNo("COM2024001")
                    .paymentId(100L)
                    .matterId(200L)
                    .status(CommissionStatus.CALCULATED)
                    .build();

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Commission> page = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            page.setRecords(Collections.singletonList(commission));
            page.setTotal(1L);

            when(commissionRepository.page(any(), any())).thenReturn(page);

            // When
            PageResult<CommissionDTO> result = commissionAppService.listCommissions(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1L);
        }

        @Test
        @DisplayName("普通用户只能查看自己的提成")
        void listCommissions_shouldFilterByUserId_whenNotAdmin() {
            // Given
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("LAWYER"));
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("LAWYER"));

            CommissionQueryDTO query = new CommissionQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);
            query.setUserId(999L); // 尝试查看他人的提成

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.listCommissions(query));
            assertThat(exception.getMessage()).contains("无权查看");
        }

        @Test
        @DisplayName("应该成功获取提成记录详情")
        void getCommission_shouldSuccess() {
            // Given
            Commission commission = Commission.builder()
                    .id(1L)
                    .commissionNo("COM2024001")
                    .paymentId(100L)
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);
            lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("ADMIN"));
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            lenient().when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            lenient().when(commissionBaseMapper.countByCommissionIdAndUserId(1L, TEST_USER_ID)).thenReturn(1);

            // When
            CommissionDTO result = commissionAppService.getCommission(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("普通用户无权查看他人的提成详情")
        void getCommission_shouldFail_whenNoPermission() {
            // Given
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("LAWYER"));
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("LAWYER"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            when(commissionBaseMapper.countByCommissionIdAndUserId(1L, TEST_USER_ID)).thenReturn(0);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.getCommission(1L));
            assertThat(exception.getMessage()).contains("无权查看");
        }

        @Test
        @DisplayName("应该成功查询用户的提成总额")
        void getUserTotalCommission_shouldSuccess() {
            // Given
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("ADMIN"));
            when(commissionRepository.sumCommissionByUserId(TEST_USER_ID)).thenReturn(BigDecimal.valueOf(50000));

            // When
            BigDecimal result = commissionAppService.getUserTotalCommission(TEST_USER_ID);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50000));
        }

        @Test
        @DisplayName("普通用户只能查看自己的提成总额")
        void getUserTotalCommission_shouldFail_whenNotSelf() {
            // Given
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("LAWYER"));
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("LAWYER"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.getUserTotalCommission(999L));
            assertThat(exception.getMessage()).contains("无权查看");
        }
    }

    @Nested
    @DisplayName("提成汇总测试")
    class CommissionSummaryTests {

        @Test
        @DisplayName("应该成功获取全所提成汇总")
        void getCommissionSummary_shouldSuccess() {
            // Given
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("ADMIN"));
            when(commissionRepository.sumTotalCommission(anyString(), anyString())).thenReturn(BigDecimal.valueOf(100000));
            when(commissionRepository.sumCommissionByStatus(eq(CommissionStatus.APPROVED), anyString(), anyString()))
                    .thenReturn(BigDecimal.valueOf(80000));
            when(commissionRepository.sumCommissionByStatus(eq(CommissionStatus.PAID), anyString(), anyString()))
                    .thenReturn(BigDecimal.valueOf(50000));
            when(commissionRepository.countCommissions(anyString(), anyString())).thenReturn(100L);
            when(commissionRepository.sumCommissionByUser(anyString(), anyString())).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = commissionAppService.getCommissionSummary("2024-01-01", "2024-12-31");

            // Then
            assertThat(result).isNotNull();
            assertThat((BigDecimal) result.get("totalCommission")).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat((BigDecimal) result.get("approvedCommission")).isEqualByComparingTo(BigDecimal.valueOf(80000));
            assertThat((BigDecimal) result.get("paidCommission")).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(result.get("totalCount")).isEqualTo(100L);
        }

        @Test
        @DisplayName("非管理层无权查看提成汇总")
        void getCommissionSummary_shouldFail_whenNotManagement() {
            // Given
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("LAWYER"));
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("LAWYER"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.getCommissionSummary("2024-01-01", "2024-12-31"));
            assertThat(exception.getMessage()).contains("只有管理层");
        }
    }

    @Nested
    @DisplayName("提成审批测试")
    class ApproveCommissionTests {

        @Test
        @DisplayName("应该成功审批通过提成")
        void approveCommission_shouldSuccess_whenApproved() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
            lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("DIRECTOR"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .commissionNo("COM2024001")
                    .status(CommissionStatus.CALCULATED)
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            when(commissionBaseMapper.updateById(any(Commission.class))).thenReturn(1);

            // When
            CommissionDTO result = commissionAppService.approveCommission(1L, true, "审批通过");

            // Then
            assertThat(result).isNotNull();
            assertThat(commission.getStatus()).isEqualTo(CommissionStatus.APPROVED);
            assertThat(commission.getApprovedBy()).isEqualTo(TEST_USER_ID);
            assertThat(commission.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("应该成功审批驳回提成")
        void approveCommission_shouldSuccess_whenRejected() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
            lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("DIRECTOR"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .status(CommissionStatus.CALCULATED)
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            when(commissionBaseMapper.updateById(any(Commission.class))).thenReturn(1);

            // When
            CommissionDTO result = commissionAppService.approveCommission(1L, false, "审批驳回");

            // Then
            assertThat(result).isNotNull();
            assertThat(commission.getStatus()).isEqualTo(CommissionStatus.CALCULATED);
        }

        @Test
        @DisplayName("只有已计算的提成才能审批")
        void approveCommission_shouldFail_whenNotCalculated() {
            // Given
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(true);
            lenient().when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("DIRECTOR"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .status(CommissionStatus.APPROVED) // 已审批
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.approveCommission(1L, true, ""));
            assertThat(exception.getMessage()).contains("只有已计算的提成才能审批");
        }
    }

    @Nested
    @DisplayName("提成发放测试")
    class IssueCommissionTests {

        @Test
        @DisplayName("应该成功确认提成已发放")
        void issueCommission_shouldSuccess() {
            // Given
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("FINANCE"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .commissionNo("COM2024001")
                    .commissionAmount(BigDecimal.valueOf(5000))
                    .status(CommissionStatus.APPROVED)
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);
            
            com.lawfirm.infrastructure.persistence.mapper.CommissionMapper commissionBaseMapper = 
                    mock(com.lawfirm.infrastructure.persistence.mapper.CommissionMapper.class);
            when(commissionRepository.getBaseMapper()).thenReturn(commissionBaseMapper);
            when(commissionBaseMapper.updateById(any(Commission.class))).thenReturn(1);

            // When
            CommissionDTO result = commissionAppService.issueCommission(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(commission.getStatus()).isEqualTo(CommissionStatus.PAID);
            assertThat(commission.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("只有已审批的提成才能发放")
        void issueCommission_shouldFail_whenNotApproved() {
            // Given
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("FINANCE"));

            Commission commission = Commission.builder()
                    .id(1L)
                    .status(CommissionStatus.CALCULATED) // 未审批
                    .build();

            when(commissionRepository.findById(1L)).thenReturn(commission);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.issueCommission(1L));
            assertThat(exception.getMessage()).contains("只有已审批的提成才能发放");
        }

        @Test
        @DisplayName("非财务人员无权确认发放")
        void issueCommission_shouldFail_whenNotFinance() {
            // Given
            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("LAWYER"));
            when(userRepository.findRoleCodesByUserId(TEST_USER_ID)).thenReturn(Arrays.asList("LAWYER"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> commissionAppService.issueCommission(1L));
            assertThat(exception.getMessage()).contains("只有财务人员");
        }
    }
}
