package com.lawfirm.application.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.finance.command.CreatePrepaymentCommand;
import com.lawfirm.application.finance.command.UsePrepaymentCommand;
import com.lawfirm.application.finance.dto.PrepaymentDTO;
import com.lawfirm.application.finance.dto.PrepaymentUsageDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Prepayment;
import com.lawfirm.domain.finance.entity.PrepaymentUsage;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PrepaymentRepository;
import com.lawfirm.domain.finance.repository.PrepaymentUsageRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentMapper;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentUsageMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** PrepaymentAppService 单元测试 测试预收款管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrepaymentAppService 预收款服务测试")
class PrepaymentAppServiceTest {

  private static final Long TEST_PREPAYMENT_ID = 100L;
  private static final Long TEST_CLIENT_ID = 200L;
  private static final Long TEST_CONTRACT_ID = 300L;
  private static final Long TEST_MATTER_ID = 400L;
  private static final Long TEST_FEE_ID = 500L;
  private static final Long TEST_USER_ID = 600L;

  @Mock private PrepaymentRepository prepaymentRepository;

  @Mock private PrepaymentUsageRepository usageRepository;

  @Mock private PrepaymentMapper prepaymentMapper;

  @Mock private PrepaymentUsageMapper usageMapper;

  @Mock private ClientRepository clientRepository;

  @Mock private ContractRepository contractRepository;

  @Mock private MatterRepository matterRepository;

  @Mock private FeeRepository feeRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private PrepaymentAppService prepaymentAppService;

  @Nested
  @DisplayName("创建预收款测试")
  class CreatePrepaymentTests {

    @Test
    @DisplayName("应该成功创建预收款")
    void createPrepayment_shouldSuccess() {
      // Given
      CreatePrepaymentCommand command = new CreatePrepaymentCommand();
      command.setClientId(TEST_CLIENT_ID);
      command.setAmount(new BigDecimal("10000.00"));
      command.setReceiptDate(LocalDate.now());
      command.setPaymentMethod("BANK");

      Client client = new Client();
      client.setId(TEST_CLIENT_ID);

      when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
      when(prepaymentRepository.save(any(Prepayment.class)))
          .thenAnswer(
              invocation -> {
                Prepayment prepayment = invocation.getArgument(0);
                prepayment.setId(TEST_PREPAYMENT_ID);
                prepayment.setPrepaymentNo("YS240118ABCD");
                return true;
              });

      // When
      PrepaymentDTO result = prepaymentAppService.createPrepayment(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
      assertThat(result.getStatus()).isEqualTo("PENDING");
      verify(prepaymentRepository).save(any(Prepayment.class));
    }

    @Test
    @DisplayName("应该成功创建带合同和项目的预收款")
    void createPrepayment_shouldSuccess_withContractAndMatter() {
      // Given
      CreatePrepaymentCommand command = new CreatePrepaymentCommand();
      command.setClientId(TEST_CLIENT_ID);
      command.setContractId(TEST_CONTRACT_ID);
      command.setMatterId(TEST_MATTER_ID);
      command.setAmount(new BigDecimal("5000.00"));
      command.setReceiptDate(LocalDate.now());

      Client client = new Client();
      client.setId(TEST_CLIENT_ID);
      Contract contract = new Contract();
      contract.setId(TEST_CONTRACT_ID);
      Matter matter = new Matter();
      matter.setId(TEST_MATTER_ID);

      when(clientRepository.getByIdOrThrow(eq(TEST_CLIENT_ID), anyString())).thenReturn(client);
      when(contractRepository.getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString()))
          .thenReturn(contract);
      when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
      when(prepaymentRepository.save(any(Prepayment.class)))
          .thenAnswer(
              invocation -> {
                Prepayment prepayment = invocation.getArgument(0);
                prepayment.setId(TEST_PREPAYMENT_ID);
                return true;
              });

      // When
      PrepaymentDTO result = prepaymentAppService.createPrepayment(command);

      // Then
      assertThat(result).isNotNull();
      verify(contractRepository).getByIdOrThrow(eq(TEST_CONTRACT_ID), anyString());
      verify(matterRepository).getByIdOrThrow(eq(TEST_MATTER_ID), anyString());
    }
  }

  @Nested
  @DisplayName("确认预收款测试")
  class ConfirmPrepaymentTests {

    @Test
    @DisplayName("应该成功确认预收款")
    void confirmPrepayment_shouldSuccess() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .prepaymentNo("YS240118ABCD")
              .status("PENDING")
              .build();

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      lenient().when(prepaymentRepository.updateById(any(Prepayment.class))).thenReturn(true);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        PrepaymentDTO result = prepaymentAppService.confirmPrepayment(TEST_PREPAYMENT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(prepayment.getStatus()).isEqualTo("ACTIVE");
        assertThat(prepayment.getConfirmerId()).isEqualTo(TEST_USER_ID);
      }
    }

    @Test
    @DisplayName("非待确认状态的预收款不能确认")
    void confirmPrepayment_shouldFail_whenNotPending() {
      // Given
      Prepayment prepayment = Prepayment.builder().id(TEST_PREPAYMENT_ID).status("ACTIVE").build();

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> prepaymentAppService.confirmPrepayment(TEST_PREPAYMENT_ID));
      assertThat(exception.getMessage()).contains("不允许确认");
    }
  }

  @Nested
  @DisplayName("使用预收款测试")
  class UsePrepaymentTests {

    @Test
    @DisplayName("应该成功使用预收款核销")
    void usePrepayment_shouldSuccess() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .prepaymentNo("YS240118ABCD")
              .clientId(TEST_CLIENT_ID)
              .amount(new BigDecimal("10000.00"))
              .usedAmount(BigDecimal.ZERO)
              .remainingAmount(new BigDecimal("10000.00"))
              .status("ACTIVE")
              .build();

      Fee fee =
          Fee.builder()
              .id(TEST_FEE_ID)
              .clientId(TEST_CLIENT_ID)
              .matterId(TEST_MATTER_ID)
              .amount(new BigDecimal("5000.00"))
              .paidAmount(BigDecimal.ZERO)
              .status("UNPAID")
              .build();

      UsePrepaymentCommand command = new UsePrepaymentCommand();
      command.setPrepaymentId(TEST_PREPAYMENT_ID);
      command.setFeeId(TEST_FEE_ID);
      command.setAmount(new BigDecimal("5000.00"));

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      when(feeRepository.getByIdOrThrow(eq(TEST_FEE_ID), anyString())).thenReturn(fee);
      when(usageRepository.save(any(PrepaymentUsage.class)))
          .thenAnswer(
              invocation -> {
                PrepaymentUsage usage = invocation.getArgument(0);
                usage.setId(1L);
                return true;
              });
      lenient().when(prepaymentRepository.updateById(any(Prepayment.class))).thenReturn(true);
      lenient().when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        PrepaymentUsageDTO result = prepaymentAppService.usePrepayment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(prepayment.getUsedAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(prepayment.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(fee.getPaidAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(fee.getStatus()).isEqualTo("PAID");
      }
    }

    @Test
    @DisplayName("核销金额超过剩余金额应该失败")
    void usePrepayment_shouldFail_whenAmountExceedsRemaining() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .remainingAmount(new BigDecimal("1000.00"))
              .status("ACTIVE")
              .build();

      Fee fee = new Fee();
      fee.setId(TEST_FEE_ID);

      UsePrepaymentCommand command = new UsePrepaymentCommand();
      command.setPrepaymentId(TEST_PREPAYMENT_ID);
      command.setFeeId(TEST_FEE_ID);
      command.setAmount(new BigDecimal("2000.00"));

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      when(feeRepository.getByIdOrThrow(eq(TEST_FEE_ID), anyString())).thenReturn(fee);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> prepaymentAppService.usePrepayment(command));
      assertThat(exception.getMessage()).contains("超过预收款剩余金额");
    }

    @Test
    @DisplayName("客户不一致应该失败")
    void usePrepayment_shouldFail_whenClientMismatch() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .clientId(TEST_CLIENT_ID)
              .remainingAmount(new BigDecimal("10000.00"))
              .status("ACTIVE")
              .build();

      Fee fee =
          Fee.builder()
              .id(TEST_FEE_ID)
              .clientId(999L) // 不同的客户ID
              .amount(new BigDecimal("5000.00"))
              .paidAmount(BigDecimal.ZERO)
              .build();

      UsePrepaymentCommand command = new UsePrepaymentCommand();
      command.setPrepaymentId(TEST_PREPAYMENT_ID);
      command.setFeeId(TEST_FEE_ID);
      command.setAmount(new BigDecimal("5000.00"));

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      when(feeRepository.getByIdOrThrow(eq(TEST_FEE_ID), anyString())).thenReturn(fee);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> prepaymentAppService.usePrepayment(command));
      assertThat(exception.getMessage()).contains("客户不一致");
    }

    @Test
    @DisplayName("核销后剩余金额为0应该更新状态为已用完")
    void usePrepayment_shouldUpdateStatusToUsed_whenRemainingZero() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .prepaymentNo("YS240118ABCD")
              .clientId(TEST_CLIENT_ID)
              .amount(new BigDecimal("5000.00"))
              .usedAmount(BigDecimal.ZERO)
              .remainingAmount(new BigDecimal("5000.00"))
              .status("ACTIVE")
              .build();

      Fee fee =
          Fee.builder()
              .id(TEST_FEE_ID)
              .clientId(TEST_CLIENT_ID)
              .matterId(TEST_MATTER_ID)
              .amount(new BigDecimal("5000.00"))
              .paidAmount(BigDecimal.ZERO)
              .status("UNPAID")
              .build();

      UsePrepaymentCommand command = new UsePrepaymentCommand();
      command.setPrepaymentId(TEST_PREPAYMENT_ID);
      command.setFeeId(TEST_FEE_ID);
      command.setAmount(new BigDecimal("5000.00"));

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      when(feeRepository.getByIdOrThrow(eq(TEST_FEE_ID), anyString())).thenReturn(fee);
      when(usageRepository.save(any(PrepaymentUsage.class)))
          .thenAnswer(
              invocation -> {
                PrepaymentUsage usage = invocation.getArgument(0);
                usage.setId(1L);
                return true;
              });
      lenient().when(prepaymentRepository.updateById(any(Prepayment.class))).thenReturn(true);
      lenient().when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        // When
        prepaymentAppService.usePrepayment(command);

        // Then
        assertThat(prepayment.getStatus()).isEqualTo("USED");
      }
    }
  }

  @Nested
  @DisplayName("退款测试")
  class RefundPrepaymentTests {

    @Test
    @DisplayName("应该成功退款")
    void refundPrepayment_shouldSuccess() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .prepaymentNo("YS240118ABCD")
              .status("ACTIVE")
              .usedAmount(BigDecimal.ZERO)
              .build();

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      lenient().when(prepaymentRepository.updateById(any(Prepayment.class))).thenReturn(true);

      // When
      PrepaymentDTO result = prepaymentAppService.refundPrepayment(TEST_PREPAYMENT_ID, "客户要求退款");

      // Then
      assertThat(result).isNotNull();
      assertThat(prepayment.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("已有核销记录的预收款不能退款")
    void refundPrepayment_shouldFail_whenHasUsage() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .status("ACTIVE")
              .usedAmount(new BigDecimal("1000.00"))
              .build();

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> prepaymentAppService.refundPrepayment(TEST_PREPAYMENT_ID, "退款"));
      assertThat(exception.getMessage()).contains("已有核销记录");
    }
  }

  @Nested
  @DisplayName("查询预收款测试")
  class QueryPrepaymentTests {

    @Test
    @DisplayName("应该成功获取预收款详情")
    void getPrepaymentById_shouldSuccess() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .prepaymentNo("YS240118ABCD")
              .clientId(TEST_CLIENT_ID)
              .amount(new BigDecimal("10000.00"))
              .build();

      when(prepaymentRepository.getByIdOrThrow(eq(TEST_PREPAYMENT_ID), anyString()))
          .thenReturn(prepayment);
      when(usageRepository.findByPrepaymentId(TEST_PREPAYMENT_ID))
          .thenReturn(Collections.emptyList());
      lenient().when(clientRepository.findById(TEST_CLIENT_ID)).thenReturn(new Client());

      // When
      PrepaymentDTO result = prepaymentAppService.getPrepaymentById(TEST_PREPAYMENT_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getPrepaymentNo()).isEqualTo("YS240118ABCD");
    }

    @Test
    @DisplayName("应该成功查询可用预收款")
    void getAvailablePrepayments_shouldSuccess() {
      // Given
      Prepayment prepayment =
          Prepayment.builder()
              .id(TEST_PREPAYMENT_ID)
              .clientId(TEST_CLIENT_ID)
              .status("ACTIVE")
              .remainingAmount(new BigDecimal("5000.00"))
              .build();

      when(prepaymentRepository.findActiveByClientId(TEST_CLIENT_ID))
          .thenReturn(Collections.singletonList(prepayment));

      // When
      List<PrepaymentDTO> result = prepaymentAppService.getAvailablePrepayments(TEST_CLIENT_ID);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }
  }
}
