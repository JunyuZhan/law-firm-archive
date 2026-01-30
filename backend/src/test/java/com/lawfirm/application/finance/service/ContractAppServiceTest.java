package com.lawfirm.application.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.*;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.service.ContractTemplateVariableService;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.constant.ContractStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractPaymentScheduleRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * ContractAppService 单元测试
 *
 * <p>测试合同应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractAppService 合同服务测试")
class ContractAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_DEPT_ID = 10L;
  private static final Long TEST_CLIENT_ID = 100L;
  private static final Long TEST_CONTRACT_ID = 1000L;
  private static final Long TEST_MATTER_ID = 200L;

  @Mock private ContractRepository contractRepository;

  @Mock private ContractMapper contractMapper;

  @Mock private ClientRepository clientRepository;

  @Mock private MatterRepository matterRepository;

  @Mock private ApprovalService approvalService;

  @Mock private ApprovalAppService approvalAppService;

  @Mock private ApproverService approverService;

  @Mock private ContractPaymentScheduleRepository paymentScheduleRepository;

  @Mock private ContractParticipantRepository participantRepository;

  @Mock private UserRepository userRepository;

  @Mock private DepartmentRepository departmentRepository;

  @Mock private UserMapper userMapper;

  @Mock private ContractTemplateRepository contractTemplateRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private ContractNumberGenerator contractNumberGenerator;

  @Mock private ContractTemplateVariableService contractTemplateVariableService;

  @Mock private SysConfigAppService sysConfigAppService;

  @Mock private CauseOfActionService causeOfActionService;

  @Mock private ApprovalMapper approvalMapper;

  @InjectMocks private ContractAppService contractAppService;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    // 设置 ContractTemplateVariableService 的默认 mock 行为（仅在需要时使用）
    lenient().when(contractTemplateVariableService.getFeeTypeName(anyString())).thenReturn("固定费用");
  }

  @Nested
  @DisplayName("分页查询合同测试")
  class ListContractsTests {

    @Test
    @DisplayName("应该分页查询合同列表")
    void listContracts_shouldReturnPagedResult() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");

        ContractQueryDTO query = new ContractQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        Page<Contract> page = new Page<>(1, 10);
        page.setRecords(List.of(contract));
        page.setTotal(1);

        @SuppressWarnings("unchecked")
        Page<Contract> pageParam1 = any(Page.class);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<Contract> wrapper1 = any(LambdaQueryWrapper.class);
        when(contractRepository.page(pageParam1, wrapper1)).thenReturn(page);
        when(clientRepository.listByIds(any())).thenReturn(new ArrayList<>());
        when(matterRepository.listByIds(any())).thenReturn(new ArrayList<>());

        // When
        PageResult<ContractDTO> result = contractAppService.listContracts(query);

        // Then
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getContractNo()).isEqualTo("CT2026001");
      }
    }

    @Test
    @DisplayName("空结果时应返回空分页")
    void listContracts_shouldReturnEmptyWhenNoResults() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDataScope).thenReturn("ALL");

        ContractQueryDTO query = new ContractQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        Page<Contract> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        page.setTotal(0);

        @SuppressWarnings("unchecked")
        Page<Contract> pageParam2 = any(Page.class);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<Contract> wrapper2 = any(LambdaQueryWrapper.class);
        when(contractRepository.page(pageParam2, wrapper2)).thenReturn(page);

        // When
        PageResult<ContractDTO> result = contractAppService.listContracts(query);

        // Then
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
      }
    }
  }

  @Nested
  @DisplayName("获取我的合同测试")
  class GetMyContractsTests {

    @Test
    @DisplayName("应该获取我的合同列表")
    void getMyContracts_shouldReturnMyContracts() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setCreatedBy(TEST_USER_ID);

        Page<Contract> page = new Page<>(1, 10);
        page.setRecords(List.of(contract));
        page.setTotal(1);

        when(participantRepository.findContractIdsByUserId(TEST_USER_ID))
            .thenReturn(new ArrayList<>());
        @SuppressWarnings("unchecked")
        Page<Contract> pageParam3 = any(Page.class);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<Contract> wrapper3 = any(LambdaQueryWrapper.class);
        when(contractRepository.page(pageParam3, wrapper3)).thenReturn(page);
        when(clientRepository.listByIds(any())).thenReturn(new ArrayList<>());
        when(matterRepository.listByIds(any())).thenReturn(new ArrayList<>());

        // When
        PageResult<ContractDTO> result = contractAppService.getMyContracts(new ContractQueryDTO());

        // Then
        assertThat(result.getRecords()).hasSize(1);
      }
    }
  }

  @Nested
  @DisplayName("创建合同测试")
  class CreateContractTests {

    @Test
    @DisplayName("应该成功创建合同")
    void createContract_shouldReturnContractDTO() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

        CreateContractCommand command = new CreateContractCommand();
        command.setClientId(TEST_CLIENT_ID);
        command.setContractType("CIVIL_PROXY");
        command.setFeeType("FIXED");
        command.setTotalAmount(new BigDecimal("10000"));
        command.setName("测试合同");

        Client client = createTestClient(TEST_CLIENT_ID, "测试客户", "ACTIVE");

        when(clientRepository.getByIdOrThrow(TEST_CLIENT_ID, "客户不存在")).thenReturn(client);
        when(contractRepository.save(any(Contract.class))).thenReturn(true);
        when(participantRepository.existsByContractIdAndUserId(anyLong(), anyLong()))
            .thenReturn(false);
        when(participantRepository.save(any(ContractParticipant.class))).thenReturn(true);

        // When
        ContractDTO result = contractAppService.createContract(command);

        // Then
        assertThat(result.getStatus()).isEqualTo(ContractStatus.DRAFT);
        assertThat(result.getClientId()).isEqualTo(TEST_CLIENT_ID);
        verify(contractRepository).save(any(Contract.class));
      }
    }

    @Test
    @DisplayName("客户未转正时应抛出异常")
    void createContract_shouldThrowException_whenClientNotActive() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

        CreateContractCommand command = new CreateContractCommand();
        command.setClientId(TEST_CLIENT_ID);

        Client client = createTestClient(TEST_CLIENT_ID, "潜在客户", "PENDING");

        when(clientRepository.getByIdOrThrow(TEST_CLIENT_ID, "客户不存在")).thenReturn(client);

        // When & Then
        assertThatThrownBy(() -> contractAppService.createContract(command))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只能为已转正的正式客户创建合同");
      }
    }

    @Test
    @DisplayName("风险代理比例超出范围时应抛出异常")
    void createContract_shouldThrowException_whenRiskRatioOutOfRange() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

        CreateContractCommand command = new CreateContractCommand();
        command.setClientId(TEST_CLIENT_ID);
        command.setRiskRatio(new BigDecimal("150")); // 超出100

        Client client = createTestClient(TEST_CLIENT_ID, "测试客户", "ACTIVE");
        when(clientRepository.getByIdOrThrow(TEST_CLIENT_ID, "客户不存在")).thenReturn(client);

        // When & Then
        assertThatThrownBy(() -> contractAppService.createContract(command))
            .isInstanceOf(BusinessException.class)
            .hasMessage("风险代理比例必须在0-100之间");
      }
    }
  }

  @Nested
  @DisplayName("更新合同测试")
  class UpdateContractTests {

    @Test
    @DisplayName("应该成功更新草稿状态合同")
    void updateContract_shouldUpdateDraftContract() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, null, "测试合同");
        contract.setStatus(ContractStatus.DRAFT);
        contract.setCreatedBy(TEST_USER_ID);

        UpdateContractCommand command = new UpdateContractCommand();
        command.setId(TEST_CONTRACT_ID);
        command.setName("更新后的合同");
        command.setTotalAmount(new BigDecimal("20000"));

        // 模拟安全上下文用于所有权验证
        mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
        when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

        // When
        ContractDTO result = contractAppService.updateContract(command);

        // Then
        assertThat(result.getName()).isEqualTo("更新后的合同");
        verify(contractRepository).updateById(contract);
      }
    }

    @Test
    @DisplayName("不能更新已审批通过的合同")
    void updateContract_shouldThrowException_whenContractActive() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy(TEST_USER_ID);

        UpdateContractCommand command = new UpdateContractCommand();
        command.setId(TEST_CONTRACT_ID);

        // 模拟安全上下文用于所有权验证（所有权验证在状态检查之前）
        mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

        // When & Then
        assertThatThrownBy(() -> contractAppService.updateContract(command))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只有草稿状态或被拒绝状态的合同可以直接修改");
      }
    }
  }

  @Nested
  @DisplayName("删除合同测试")
  class DeleteContractTests {

    @Test
    @DisplayName("应该成功删除草稿状态合同")
    void deleteContract_shouldDeleteDraftContract() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, null, "测试合同");
        contract.setStatus(ContractStatus.DRAFT);
        contract.setCreatedBy(TEST_USER_ID);

        // 模拟安全上下文用于所有权验证
        mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
        when(contractMapper.deleteById(TEST_CONTRACT_ID)).thenReturn(1);

        // When
        contractAppService.deleteContract(TEST_CONTRACT_ID);

        // Then
        verify(contractMapper).deleteById(TEST_CONTRACT_ID);
      }
    }

    @Test
    @DisplayName("不能删除已审批通过的合同")
    void deleteContract_shouldThrowException_whenContractActive() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy(TEST_USER_ID);

        // 模拟安全上下文用于所有权验证（所有权验证在状态检查之前）
        mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

        // When & Then
        assertThatThrownBy(() -> contractAppService.deleteContract(TEST_CONTRACT_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessage("只有草稿状态或已拒绝状态的合同可以删除，已审批通过的合同不能删除");
      }
    }
  }

  @Nested
  @DisplayName("获取合同详情测试")
  class GetContractByIdTests {

    @Test
    @DisplayName("应该获取合同详情")
    void getContractById_shouldReturnContractWithApprovals() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");

      ApprovalDTO approval = new ApprovalDTO();
      approval.setId(1L);
      approval.setStatus(ApprovalStatus.PENDING);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
      when(approvalAppService.getBusinessApprovals("CONTRACT", TEST_CONTRACT_ID))
          .thenReturn(List.of(approval));

      // When
      ContractDTO result = contractAppService.getContractById(TEST_CONTRACT_ID);

      // Then
      assertThat(result.getId()).isEqualTo(TEST_CONTRACT_ID);
      assertThat(result.getApprovals()).hasSize(1);
      assertThat(result.getCurrentApproval()).isNotNull();
    }

    @Test
    @DisplayName("合同不存在时应抛出异常")
    void getContractById_shouldThrowException_whenNotFound() {
      // Given
      when(contractRepository.getByIdOrThrow(999L, "合同不存在"))
          .thenThrow(new BusinessException("合同不存在"));

      // When & Then
      assertThatThrownBy(() -> contractAppService.getContractById(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("合同不存在");
    }
  }

  @Nested
  @DisplayName("提交审批测试")
  class SubmitForApprovalTests {

    @Test
    @DisplayName("应该成功提交审批")
    void submitForApproval_shouldSubmit() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, null, "测试合同");
        contract.setStatus(ContractStatus.DRAFT);
        contract.setTotalAmount(new BigDecimal("50000"));

        User approver = createTestUser(2L, "审批人");
        approver.setStatus("ACTIVE");

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
        when(contractNumberGenerator.generate(any(), any(), any())).thenReturn("CT2026001");
        when(approverService.findContractApprover(any())).thenReturn(2L);
        when(userRepository.getById(2L)).thenReturn(approver);
        when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

        // When
        contractAppService.submitForApproval(TEST_CONTRACT_ID, null);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING);
        assertThat(contract.getContractNo()).isEqualTo("CT2026001");
        verify(approvalService)
            .createApproval(any(), anyLong(), any(), any(), anyLong(), any(), any(), any());
      }
    }

    @Test
    @DisplayName("只能提交草稿或被拒绝状态的合同")
    void submitForApproval_shouldThrowException_whenCannotSubmit() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.ACTIVE);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

      // When & Then
      assertThatThrownBy(() -> contractAppService.submitForApproval(TEST_CONTRACT_ID, null))
          .isInstanceOf(BusinessException.class)
          .hasMessage("只有草稿状态或被拒绝状态的合同可以提交审批");
    }
  }

  @Nested
  @DisplayName("审批通过测试")
  class ApproveTests {

    @Test
    @DisplayName("应该成功审批通过合同")
    void approve_shouldApproveContract() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setStatus(ContractStatus.PENDING);

        // 模拟安全上下文用于事件发布
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getRealName).thenReturn("测试用户");

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
        when(approvalAppService.getBusinessApprovals("CONTRACT", TEST_CONTRACT_ID))
            .thenReturn(new ArrayList<>());
        when(contractRepository.updateById(any(Contract.class))).thenReturn(true);
        doNothing().when(eventPublisher).publishEvent(any());

        // When
        contractAppService.approve(TEST_CONTRACT_ID);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(contract.getEffectiveDate()).isNotNull();
        verify(eventPublisher).publishEvent(any());
      }
    }

    @Test
    @DisplayName("只能审批待审批状态的合同")
    void approve_shouldThrowException_whenNotPending() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.DRAFT);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

      // When & Then
      assertThatThrownBy(() -> contractAppService.approve(TEST_CONTRACT_ID))
          .isInstanceOf(BusinessException.class)
          .hasMessage("只有待审批状态可以审批");
    }
  }

  @Nested
  @DisplayName("审批拒绝测试")
  class RejectTests {

    @Test
    @DisplayName("应该成功拒绝合同")
    void reject_shouldRejectContract() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.PENDING);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
      when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

      // When
      contractAppService.reject(TEST_CONTRACT_ID, "资料不全");

      // Then
      assertThat(contract.getStatus()).isEqualTo(ContractStatus.REJECTED);
      assertThat(contract.getRemark()).isEqualTo("资料不全");
    }
  }

  @Nested
  @DisplayName("撤回审批测试")
  class WithdrawApprovalTests {

    @Test
    @DisplayName("创建者应该能撤回审批")
    void withdrawApproval_shouldWithdraw() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setStatus(ContractStatus.PENDING);
        contract.setCreatedBy(TEST_USER_ID);

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<com.lawfirm.domain.workbench.entity.Approval> wrapper4 =
            any(LambdaQueryWrapper.class);
        when(approvalMapper.selectList(wrapper4)).thenReturn(new ArrayList<>());
        when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

        // When
        contractAppService.withdrawApproval(TEST_CONTRACT_ID);

        // Then
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
      }
    }

    @Test
    @DisplayName("非创建者或签约人不能撤回")
    void withdrawApproval_shouldThrowException_whenNotCreator() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
        contract.setStatus(ContractStatus.PENDING);
        contract.setCreatedBy(999L);
        contract.setSignerId(888L); // 也不是签约人

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

        // When & Then
        assertThatThrownBy(() -> contractAppService.withdrawApproval(TEST_CONTRACT_ID))
            .isInstanceOf(BusinessException.class)
            .hasMessage("只有合同创建者或签约人才能撤回审批");
      }
    }
  }

  @Nested
  @DisplayName("终止合同测试")
  class TerminateTests {

    @Test
    @DisplayName("应该成功终止合同")
    void terminate_shouldTerminateContract() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.ACTIVE);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
      when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

      // When
      contractAppService.terminate(TEST_CONTRACT_ID, "双方协商终止");

      // Then
      assertThat(contract.getStatus()).isEqualTo(ContractStatus.TERMINATED);
      assertThat(contract.getRemark()).isEqualTo("双方协商终止");
    }

    @Test
    @DisplayName("只能终止生效中的合同")
    void terminate_shouldThrowException_whenNotActive() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.DRAFT);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);

      // When & Then
      assertThatThrownBy(() -> contractAppService.terminate(TEST_CONTRACT_ID, "原因"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("只有生效中的合同可以终止");
    }
  }

  @Nested
  @DisplayName("完成合同测试")
  class CompleteTests {

    @Test
    @DisplayName("应该成功完成合同")
    void complete_shouldCompleteContract() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.ACTIVE);

      when(contractRepository.getByIdOrThrow(TEST_CONTRACT_ID, "合同不存在")).thenReturn(contract);
      when(contractRepository.updateById(any(Contract.class))).thenReturn(true);

      // When
      contractAppService.complete(TEST_CONTRACT_ID);

      // Then
      assertThat(contract.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }
  }

  @Nested
  @DisplayName("获取已审批合同测试")
  class GetApprovedContractsTests {

    @Test
    @DisplayName("应该获取已审批合同列表")
    void getApprovedContracts_shouldReturnActiveContracts() {
      // Given
      Contract contract = createTestContract(TEST_CONTRACT_ID, "CT2026001", "测试合同");
      contract.setStatus(ContractStatus.ACTIVE);

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Contract> wrapper5 = any(LambdaQueryWrapper.class);
      when(contractRepository.list(wrapper5)).thenReturn(List.of(contract));
      when(participantRepository.findByContractId(TEST_CONTRACT_ID)).thenReturn(new ArrayList<>());

      // When
      List<ContractDTO> result = contractAppService.getApprovedContracts();

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getStatus()).isEqualTo(ContractStatus.ACTIVE);
    }
  }

  @Nested
  @DisplayName("获取可选审批人测试")
  class GetAvailableApproversTests {

    @Test
    @DisplayName("应该获取可选审批人列表")
    void getAvailableApprovers_shouldReturnApprovers() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        Department dept = createTestDepartment(TEST_DEPT_ID, "法务部", null);
        dept.setLeaderId(2L);

        User leader = createTestUser(2L, "部门负责人");

        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurity.when(SecurityUtils::getDepartmentId).thenReturn(TEST_DEPT_ID);

        when(departmentRepository.getById(TEST_DEPT_ID)).thenReturn(dept);
        when(userRepository.getById(2L)).thenReturn(leader);
        when(userMapper.selectUserIdsByRoleCode("TEAM_LEADER")).thenReturn(new ArrayList<>());
        when(userMapper.selectUserIdsByRoleCode("DIRECTOR")).thenReturn(new ArrayList<>());

        // When
        List<Map<String, Object>> result = contractAppService.getAvailableApprovers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("id")).isEqualTo(2L);
      }
    }
  }

  // ========== 辅助方法 ==========

  private Contract createTestContract(Long id, String contractNo, String name) {
    return Contract.builder()
        .id(id)
        .contractNo(contractNo)
        .name(name)
        .clientId(TEST_CLIENT_ID)
        .matterId(TEST_MATTER_ID)
        .contractType("CIVIL_PROXY")
        .feeType("FIXED")
        .totalAmount(new BigDecimal("10000"))
        .paidAmount(BigDecimal.ZERO)
        .currency("CNY")
        .signDate(LocalDate.now())
        .status(ContractStatus.DRAFT)
        .signerId(TEST_USER_ID)
        .departmentId(TEST_DEPT_ID)
        .archiveStatus("NOT_ARCHIVED")
        .conflictCheckStatus("NOT_REQUIRED")
        .build();
  }

  private Client createTestClient(Long id, String name, String status) {
    return Client.builder().id(id).name(name).status(status).build();
  }

  private User createTestUser(Long id, String realName) {
    User user = User.builder().username("user" + id).realName(realName).status("ACTIVE").build();
    user.setId(id);
    return user;
  }

  private Department createTestDepartment(Long id, String name, Long parentId) {
    Department department =
        Department.builder()
            .name(name)
            .parentId(parentId != null ? parentId : 0L)
            .status("ACTIVE")
            .build();
    department.setId(id);
    return department;
  }
}
