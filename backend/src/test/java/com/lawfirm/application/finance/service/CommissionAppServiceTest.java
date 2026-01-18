package com.lawfirm.application.finance.service;

import com.lawfirm.application.finance.command.CreateCommissionRuleCommand;
import com.lawfirm.application.finance.command.ManualCalculateCommissionCommand;
import com.lawfirm.application.finance.command.UpdateCommissionRuleCommand;
import com.lawfirm.application.finance.dto.CommissionDTO;
import com.lawfirm.application.finance.dto.CommissionRuleDTO;
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
}
