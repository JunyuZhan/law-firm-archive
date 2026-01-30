package com.lawfirm.application.finance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.finance.command.CreateFeeCommand;
import com.lawfirm.application.finance.command.CreatePaymentCommand;
import com.lawfirm.application.finance.dto.FeeDTO;
import com.lawfirm.application.finance.dto.PaymentDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** FeeAppService 单元测试 测试财务模块核心业务逻辑：收费管理、收款确认、金额计算 */
@ExtendWith(MockitoExtension.class)
class FeeAppServiceTest {

  @Mock private FeeRepository feeRepository;

  @Mock private PaymentRepository paymentRepository;

  @Mock private ContractRepository contractRepository;

  @Mock private ClientRepository clientRepository;

  @Mock private CommissionAppService commissionAppService;

  @Mock private MatterAppService matterAppService;

  @InjectMocks private FeeAppService feeAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");

    feeAppService.setMatterAppService(matterAppService);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  // ==================== 创建收费记录测试 ====================

  @Nested
  @DisplayName("创建收费记录测试")
  class CreateFeeTests {

    @Test
    @DisplayName("成功创建收费记录")
    void createFee_Success() {
      // Given
      CreateFeeCommand command = new CreateFeeCommand();
      command.setClientId(1L);
      command.setMatterId(1L);
      command.setFeeType("LAWYER_FEE");
      command.setFeeName("律师服务费");
      command.setAmount(new BigDecimal("10000.00"));
      command.setPlannedDate(LocalDate.now().plusDays(30));

      Client client = new Client();
      client.setId(1L);
      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(feeRepository.save(any(Fee.class))).thenReturn(true);

      // When
      FeeDTO result = feeAppService.createFee(command);

      // Then
      assertNotNull(result);
      assertEquals("LAWYER_FEE", result.getFeeType());
      assertEquals("律师服务费", result.getFeeName());
      assertEquals(new BigDecimal("10000.00"), result.getAmount());
      assertEquals(BigDecimal.ZERO, result.getPaidAmount());
      assertEquals("PENDING", result.getStatus());
      verify(feeRepository).save(any(Fee.class));
    }

    @Test
    @DisplayName("客户不存在时创建收费记录失败")
    void createFee_ClientNotFound() {
      // Given
      CreateFeeCommand command = new CreateFeeCommand();
      command.setClientId(999L);
      command.setAmount(new BigDecimal("10000.00"));

      when(clientRepository.getByIdOrThrow(eq(999L), anyString()))
          .thenThrow(new BusinessException("客户不存在"));

      // When & Then
      assertThrows(BusinessException.class, () -> feeAppService.createFee(command));
    }
  }

  // ==================== 创建收款记录测试 ====================

  @Nested
  @DisplayName("创建收款记录测试")
  class CreatePaymentTests {

    @Test
    @DisplayName("成功创建收款记录")
    void createPayment_Success() {
      // Given
      Fee fee =
          Fee.builder()
              .id(1L)
              .feeNo("SF2401010001")
              .amount(new BigDecimal("10000.00"))
              .paidAmount(BigDecimal.ZERO)
              .currency("CNY")
              .build();

      CreatePaymentCommand command = new CreatePaymentCommand();
      command.setFeeId(1L);
      command.setAmount(new BigDecimal("5000.00"));
      command.setPaymentMethod("BANK");
      command.setPaymentDate(LocalDate.now());

      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.save(any(Payment.class))).thenReturn(true);

      // When
      PaymentDTO result = feeAppService.createPayment(command);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("5000.00"), result.getAmount());
      assertEquals("BANK", result.getPaymentMethod());
      assertEquals("PENDING", result.getStatus());
      verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("收款金额超过待收金额时失败")
    void createPayment_AmountExceedsUnpaid() {
      // Given
      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("10000.00"))
              .paidAmount(new BigDecimal("8000.00"))
              .build();

      CreatePaymentCommand command = new CreatePaymentCommand();
      command.setFeeId(1L);
      command.setAmount(new BigDecimal("5000.00")); // 超过待收金额 2000

      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> feeAppService.createPayment(command));
      assertEquals("收款金额超过待收金额", exception.getMessage());
    }

    @Test
    @DisplayName("收款金额等于待收金额时成功")
    void createPayment_AmountEqualsUnpaid() {
      // Given
      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("10000.00"))
              .paidAmount(new BigDecimal("8000.00"))
              .currency("CNY")
              .build();

      CreatePaymentCommand command = new CreatePaymentCommand();
      command.setFeeId(1L);
      command.setAmount(new BigDecimal("2000.00")); // 正好等于待收金额
      command.setPaymentMethod("BANK");
      command.setPaymentDate(LocalDate.now());

      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.save(any(Payment.class))).thenReturn(true);

      // When
      PaymentDTO result = feeAppService.createPayment(command);

      // Then
      assertNotNull(result);
      assertEquals(new BigDecimal("2000.00"), result.getAmount());
    }
  }

  // ==================== 确认收款测试 ====================

  @Nested
  @DisplayName("确认收款测试")
  class ConfirmPaymentTests {

    @Test
    @DisplayName("成功确认收款并更新收费状态为部分收款")
    void confirmPayment_PartialPaid() {
      // Given
      Payment payment =
          Payment.builder()
              .id(1L)
              .paymentNo("SK2401010001")
              .feeId(1L)
              .amount(new BigDecimal("5000.00"))
              .status("PENDING")
              .build();

      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("10000.00"))
              .paidAmount(BigDecimal.ZERO)
              .build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);
      when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      // When
      feeAppService.confirmPayment(1L);

      // Then
      assertEquals("CONFIRMED", payment.getStatus());
      assertTrue(payment.getLocked());
      assertNotNull(payment.getLockedAt());
      assertEquals(new BigDecimal("5000.00"), fee.getPaidAmount());
      assertEquals("PARTIAL", fee.getStatus());
    }

    @Test
    @DisplayName("成功确认收款并更新收费状态为已收款")
    void confirmPayment_FullyPaid() {
      // Given
      Payment payment =
          Payment.builder()
              .id(1L)
              .paymentNo("SK2401010001")
              .feeId(1L)
              .amount(new BigDecimal("10000.00"))
              .status("PENDING")
              .build();

      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("10000.00"))
              .paidAmount(BigDecimal.ZERO)
              .build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);
      when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      // When
      feeAppService.confirmPayment(1L);

      // Then
      assertEquals("CONFIRMED", payment.getStatus());
      assertEquals(new BigDecimal("10000.00"), fee.getPaidAmount());
      assertEquals("PAID", fee.getStatus());
    }

    @Test
    @DisplayName("已确认的收款不能再次确认")
    void confirmPayment_AlreadyConfirmed() {
      // Given
      Payment payment = Payment.builder().id(1L).status("CONFIRMED").build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> feeAppService.confirmPayment(1L));
      assertEquals("当前状态不允许确认", exception.getMessage());
    }
  }

  // ==================== 收款锁定测试 ====================

  @Nested
  @DisplayName("收款锁定测试")
  class PaymentLockTests {

    @Test
    @DisplayName("检查收款记录是否已锁定 - 已锁定")
    void isPaymentLocked_True() {
      // Given
      Payment payment = Payment.builder().id(1L).locked(true).build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When
      boolean result = feeAppService.isPaymentLocked(1L);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("检查收款记录是否已锁定 - 未锁定")
    void isPaymentLocked_False() {
      // Given
      Payment payment = Payment.builder().id(1L).locked(false).build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When
      boolean result = feeAppService.isPaymentLocked(1L);

      // Then
      assertFalse(result);
    }

    @Test
    @DisplayName("已锁定的收款记录不能修改金额")
    void updatePaymentAmount_Locked() {
      // Given
      Payment payment = Payment.builder().id(1L).locked(true).status("PENDING").build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> feeAppService.updatePaymentAmount(1L, new BigDecimal("5000.00")));
      assertTrue(exception.getMessage().contains("已锁定"));
    }

    @Test
    @DisplayName("未锁定的待确认收款记录可以修改金额")
    void updatePaymentAmount_Success() {
      // Given
      Payment payment =
          Payment.builder()
              .id(1L)
              .locked(false)
              .status("PENDING")
              .amount(new BigDecimal("3000.00"))
              .build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);

      // When
      feeAppService.updatePaymentAmount(1L, new BigDecimal("5000.00"));

      // Then
      assertEquals(new BigDecimal("5000.00"), payment.getAmount());
      verify(paymentRepository).updateById(payment);
    }

    @Test
    @DisplayName("非待确认状态的收款记录不能修改金额")
    void updatePaymentAmount_NotPending() {
      // Given
      Payment payment = Payment.builder().id(1L).locked(false).status("CONFIRMED").build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> feeAppService.updatePaymentAmount(1L, new BigDecimal("5000.00")));
      assertTrue(exception.getMessage().contains("待确认"));
    }
  }

  // ==================== 取消收款测试 ====================

  @Nested
  @DisplayName("取消收款测试")
  class CancelPaymentTests {

    @Test
    @DisplayName("成功取消待确认的收款")
    void cancelPayment_Success() {
      // Given
      Payment payment =
          Payment.builder().id(1L).paymentNo("SK2401010001").status("PENDING").build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);

      // When
      feeAppService.cancelPayment(1L);

      // Then
      assertEquals("CANCELLED", payment.getStatus());
      verify(paymentRepository).updateById(payment);
    }

    @Test
    @DisplayName("已确认的收款不能取消")
    void cancelPayment_AlreadyConfirmed() {
      // Given
      Payment payment = Payment.builder().id(1L).status("CONFIRMED").build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> feeAppService.cancelPayment(1L));
      assertEquals("已确认的收款不能取消", exception.getMessage());
    }
  }

  // ==================== 金额计算精度测试 ====================

  @Nested
  @DisplayName("金额计算精度测试")
  class AmountPrecisionTests {

    @Test
    @DisplayName("小数金额计算精度正确")
    void amountPrecision_Decimal() {
      // Given
      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("10000.50"))
              .paidAmount(new BigDecimal("3333.33"))
              .build();

      Payment payment =
          Payment.builder()
              .id(1L)
              .feeId(1L)
              .amount(new BigDecimal("3333.33"))
              .status("PENDING")
              .build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);
      when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      // When
      feeAppService.confirmPayment(1L);

      // Then
      assertEquals(new BigDecimal("6666.66"), fee.getPaidAmount());
      assertEquals("PARTIAL", fee.getStatus());
    }

    @Test
    @DisplayName("大金额计算正确")
    void amountPrecision_LargeAmount() {
      // Given
      Fee fee =
          Fee.builder()
              .id(1L)
              .amount(new BigDecimal("99999999.99"))
              .paidAmount(BigDecimal.ZERO)
              .build();

      Payment payment =
          Payment.builder()
              .id(1L)
              .feeId(1L)
              .amount(new BigDecimal("99999999.99"))
              .status("PENDING")
              .build();

      when(paymentRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(payment);
      when(feeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(fee);
      when(paymentRepository.updateById(any(Payment.class))).thenReturn(true);
      when(feeRepository.updateById(any(Fee.class))).thenReturn(true);

      // When
      feeAppService.confirmPayment(1L);

      // Then
      assertEquals(new BigDecimal("99999999.99"), fee.getPaidAmount());
      assertEquals("PAID", fee.getStatus());
    }
  }
}
