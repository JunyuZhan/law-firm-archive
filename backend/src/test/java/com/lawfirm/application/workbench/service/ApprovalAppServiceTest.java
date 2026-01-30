package com.lawfirm.application.workbench.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lawfirm.application.workbench.command.ApproveCommand;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.dto.ApprovalQueryDTO;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** ApprovalAppService 单元测试 测试审批服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalAppService 审批服务测试")
class ApprovalAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_APPROVER_ID = 2L;
  private static final Long OTHER_USER_ID = 999L;

  @Mock private ApprovalRepository approvalRepository;

  @Mock private ApprovalMapper approvalMapper;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private com.lawfirm.application.matter.service.MatterAppService matterAppService;

  @Mock private com.lawfirm.application.finance.service.ExpenseAppService expenseAppService;

  @Mock
  private com.lawfirm.application.hr.service.RegularizationAppService regularizationAppService;

  @Mock private com.lawfirm.application.hr.service.ResignationAppService resignationAppService;

  @InjectMocks private ApprovalAppService approvalAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER"));
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询审批测试")
  class QueryApprovalTests {

    @Test
    @DisplayName("应该成功分页查询审批")
    void listApprovals_shouldSuccess() {
      // Given
      ApprovalQueryDTO query = new ApprovalQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Approval approval =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .businessType("EXPENSE")
              .applicantId(TEST_USER_ID)
              .approverId(TEST_APPROVER_ID)
              .status(ApprovalStatus.PENDING)
              .build();

      when(approvalMapper.selectApprovalPageWithPermission(
              any(), any(), any(), any(), eq(TEST_USER_ID), anyBoolean(), anyInt(), anyInt()))
          .thenReturn(Collections.singletonList(approval));
      when(approvalMapper.countApprovalWithPermission(
              any(), any(), any(), any(), eq(TEST_USER_ID), anyBoolean()))
          .thenReturn(1L);

      // When
      PageResult<ApprovalDTO> result = approvalAppService.listApprovals(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("异常时应返回空结果")
    void listApprovals_shouldReturnEmpty_whenException() {
      // Given
      ApprovalQueryDTO query = new ApprovalQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      when(approvalMapper.selectApprovalPageWithPermission(
              any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
          .thenThrow(new RuntimeException("Database error"));

      // When
      PageResult<ApprovalDTO> result = approvalAppService.listApprovals(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0L);
    }

    @Test
    @DisplayName("应该成功获取待审批列表")
    void getPendingApprovals_shouldSuccess() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.PENDING)
              .build();

      when(approvalMapper.selectPendingApprovals(TEST_USER_ID))
          .thenReturn(Collections.singletonList(approval));

      // When
      List<ApprovalDTO> result = approvalAppService.getPendingApprovals();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getApprovalNo()).isEqualTo("APP2024001");
    }

    @Test
    @DisplayName("应该成功获取我发起的审批")
    void getMyInitiatedApprovals_shouldSuccess() {
      // Given
      Approval approval =
          Approval.builder().id(1L).approvalNo("APP2024001").applicantId(TEST_USER_ID).build();

      when(approvalMapper.selectMyInitiatedApprovals(TEST_USER_ID))
          .thenReturn(Collections.singletonList(approval));

      // When
      List<ApprovalDTO> result = approvalAppService.getMyInitiatedApprovals();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getApprovalNo()).isEqualTo("APP2024001");
    }

    @Test
    @DisplayName("应该成功获取审批详情")
    void getApprovalById_shouldSuccess() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .applicantId(TEST_USER_ID)
              .approverId(TEST_APPROVER_ID)
              .build();

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When
      ApprovalDTO result = approvalAppService.getApprovalById(1L);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getApprovalNo()).isEqualTo("APP2024001");
    }

    @Test
    @DisplayName("无权查看他人的审批")
    void getApprovalById_shouldFail_whenNoPermission() {
      // Given
      Approval approval =
          Approval.builder().id(1L).applicantId(OTHER_USER_ID).approverId(OTHER_USER_ID).build();

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> approvalAppService.getApprovalById(1L));
      assertThat(exception.getMessage()).contains("无权查看");
    }

    @Test
    @DisplayName("管理员可以查看所有审批")
    void getApprovalById_shouldSuccess_whenAdmin() {
      // Given
      securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
      Approval approval =
          Approval.builder().id(1L).applicantId(OTHER_USER_ID).approverId(OTHER_USER_ID).build();

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When
      ApprovalDTO result = approvalAppService.getApprovalById(1L);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("审批操作测试")
  class ApproveTests {

    @Test
    @DisplayName("应该成功审批通过")
    void approve_shouldSuccess_whenApproved() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .businessType("EXPENSE")
              .businessId(100L)
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.PENDING)
              .build();

      ApproveCommand command = new ApproveCommand();
      command.setApprovalId(1L);
      command.setResult(ApprovalStatus.APPROVED);
      command.setComment("同意");

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);
      when(approvalRepository.updateById(any(Approval.class))).thenReturn(true);
      lenient().when(expenseAppService.approveExpense(any())).thenReturn(null);

      // When
      approvalAppService.approve(command);

      // Then
      assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
      assertThat(approval.getComment()).isEqualTo("同意");
      assertThat(approval.getApprovedAt()).isNotNull();
      verify(approvalRepository).updateById(approval);
      verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("应该成功审批拒绝")
    void approve_shouldSuccess_whenRejected() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .businessType("EXPENSE")
              .businessId(100L)
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.PENDING)
              .build();

      ApproveCommand command = new ApproveCommand();
      command.setApprovalId(1L);
      command.setResult(ApprovalStatus.REJECTED);
      command.setComment("不符合规定");

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);
      when(approvalRepository.updateById(any(Approval.class))).thenReturn(true);
      lenient().when(expenseAppService.approveExpense(any())).thenReturn(null);

      // When
      approvalAppService.approve(command);

      // Then
      assertThat(approval.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
      assertThat(approval.getComment()).isEqualTo("不符合规定");
    }

    @Test
    @DisplayName("拒绝时必须填写拒绝事由")
    void approve_shouldFail_whenRejectedWithoutComment() {
      // Given
      Approval approval =
          Approval.builder().id(1L).approverId(TEST_USER_ID).status(ApprovalStatus.PENDING).build();

      ApproveCommand command = new ApproveCommand();
      command.setApprovalId(1L);
      command.setResult(ApprovalStatus.REJECTED);
      command.setComment(null); // 未填写拒绝事由

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> approvalAppService.approve(command));
      assertThat(exception.getMessage()).contains("拒绝事由");
    }

    @Test
    @DisplayName("无权审批他人的审批")
    void approve_shouldFail_whenNotApprover() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approverId(OTHER_USER_ID) // 其他审批人
              .status(ApprovalStatus.PENDING)
              .build();

      ApproveCommand command = new ApproveCommand();
      command.setApprovalId(1L);
      command.setResult(ApprovalStatus.APPROVED);

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> approvalAppService.approve(command));
      assertThat(exception.getMessage()).contains("无权审批");
    }

    @Test
    @DisplayName("已处理的审批不能重复审批")
    void approve_shouldFail_whenAlreadyProcessed() {
      // Given
      Approval approval =
          Approval.builder()
              .id(1L)
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.APPROVED) // 已审批
              .build();

      ApproveCommand command = new ApproveCommand();
      command.setApprovalId(1L);
      command.setResult(ApprovalStatus.APPROVED);

      when(approvalRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(approval);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> approvalAppService.approve(command));
      assertThat(exception.getMessage()).contains("已处理");
    }
  }

  @Nested
  @DisplayName("批量审批测试")
  class BatchApproveTests {

    @Test
    @DisplayName("应该成功批量审批")
    void batchApprove_shouldSuccess() {
      // Given
      Approval approval1 =
          Approval.builder()
              .id(1L)
              .approvalNo("APP2024001")
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.PENDING)
              .build();
      Approval approval2 =
          Approval.builder()
              .id(2L)
              .approvalNo("APP2024002")
              .approverId(TEST_USER_ID)
              .status(ApprovalStatus.PENDING)
              .build();

      when(approvalRepository.findById(1L)).thenReturn(approval1);
      when(approvalRepository.findById(2L)).thenReturn(approval2);
      when(approvalRepository.updateById(any(Approval.class))).thenReturn(true);

      // When
      ApprovalAppService.BatchApproveResult result =
          approvalAppService.batchApprove(Arrays.asList(1L, 2L), ApprovalStatus.APPROVED, "批量通过");

      // Then
      assertThat(result.getTotal()).isEqualTo(2);
      assertThat(result.getSuccessCount()).isEqualTo(2);
      assertThat(result.getSkipCount()).isEqualTo(0);
      assertThat(approval1.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
      assertThat(approval2.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("批量审批时如果有一个失败应该全部失败")
    void batchApprove_shouldFail_whenOneInvalid() {
      // Given
      Approval approval1 =
          Approval.builder().id(1L).approverId(TEST_USER_ID).status(ApprovalStatus.PENDING).build();
      Approval approval2 =
          Approval.builder()
              .id(2L)
              .approverId(OTHER_USER_ID) // 无权审批
              .status(ApprovalStatus.PENDING)
              .build();

      when(approvalRepository.findById(1L)).thenReturn(approval1);
      when(approvalRepository.findById(2L)).thenReturn(approval2);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  approvalAppService.batchApprove(
                      Arrays.asList(1L, 2L), ApprovalStatus.APPROVED, ""));
      assertThat(exception.getMessage()).contains("无权审批");
    }

    @Test
    @DisplayName("空列表应该失败")
    void batchApprove_shouldFail_whenEmptyList() {
      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  approvalAppService.batchApprove(
                      Collections.emptyList(), ApprovalStatus.APPROVED, ""));
      assertThat(exception.getMessage()).contains("请选择");
    }
  }
}
