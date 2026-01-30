package com.lawfirm.application.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateSealApplicationCommand;
import com.lawfirm.application.document.dto.SealApplicationDTO;
import com.lawfirm.application.document.dto.SealApplicationQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.constant.SealApplicationStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.domain.document.entity.SealApplication;
import com.lawfirm.domain.document.repository.SealApplicationRepository;
import com.lawfirm.domain.document.repository.SealRepository;
import com.lawfirm.infrastructure.persistence.mapper.SealApplicationMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** SealApplicationAppService 单元测试 测试用印申请服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SealApplicationAppService 用印申请服务测试")
class SealApplicationAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_APPLICATION_ID = 100L;
  private static final Long TEST_SEAL_ID = 200L;
  private static final Long TEST_APPROVER_ID = 400L;

  @Mock private SealApplicationRepository applicationRepository;

  @Mock private SealRepository sealRepository;

  @Mock private SealApplicationMapper applicationMapper;

  @Mock private ApprovalService approvalService;

  @Mock private ApproverService approverService;

  @InjectMocks private SealApplicationAppService sealApplicationAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("测试用户");
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建用印申请测试")
  class CreateApplicationTests {

    @Test
    @DisplayName("应该成功创建用印申请")
    void createApplication_shouldSuccess() {
      // Given
      Seal seal =
          Seal.builder()
              .id(TEST_SEAL_ID)
              .name("公章")
              .status(SealApplicationStatus.SEAL_ACTIVE)
              .build();

      CreateSealApplicationCommand command = new CreateSealApplicationCommand();
      command.setSealId(TEST_SEAL_ID);
      command.setMatterId(500L);
      command.setDocumentName("合同文件");
      command.setCopies(2);
      command.setApproverId(TEST_APPROVER_ID);

      Map<String, Object> approver = new HashMap<>();
      approver.put("id", TEST_APPROVER_ID);

      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<SealApplication> wrapper1 = any(LambdaQueryWrapper.class);
      when(applicationRepository.count(wrapper1)).thenReturn(0L);
      when(applicationRepository.save(any(SealApplication.class)))
          .thenAnswer(
              invocation -> {
                SealApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                return true;
              });
      when(approverService.getSealApplicationAvailableApprovers(TEST_USER_ID))
          .thenReturn(Collections.singletonList(approver));
      lenient()
          .when(
              approvalService.createApproval(
                  anyString(),
                  anyLong(),
                  anyString(),
                  anyString(),
                  anyLong(),
                  anyString(),
                  anyString(),
                  any()))
          .thenReturn(null);

      // When
      SealApplicationDTO result = sealApplicationAppService.createApplication(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDocumentName()).isEqualTo("合同文件");
      assertThat(result.getStatus()).isEqualTo(SealApplicationStatus.PENDING);
      verify(approvalService)
          .createApproval(
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              anyLong(),
              anyString(),
              anyString(),
              any());
    }

    @Test
    @DisplayName("印章不可用时应该失败")
    void createApplication_shouldFail_whenSealInactive() {
      // Given
      Seal seal = Seal.builder().id(TEST_SEAL_ID).status("DISABLED").build();

      CreateSealApplicationCommand command = new CreateSealApplicationCommand();
      command.setSealId(TEST_SEAL_ID);

      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> sealApplicationAppService.createApplication(command));
      assertThat(exception.getMessage()).contains("印章不可用");
    }

    @Test
    @DisplayName("用印份数必须大于0")
    void createApplication_shouldFail_whenCopiesZero() {
      // Given
      Seal seal = Seal.builder().id(TEST_SEAL_ID).status(SealApplicationStatus.SEAL_ACTIVE).build();

      CreateSealApplicationCommand command = new CreateSealApplicationCommand();
      command.setSealId(TEST_SEAL_ID);
      command.setCopies(0);

      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> sealApplicationAppService.createApplication(command));
      assertThat(exception.getMessage()).contains("用印份数必须大于0");
    }

    @Test
    @DisplayName("用印份数不能超过100")
    void createApplication_shouldFail_whenCopiesExceedLimit() {
      // Given
      Seal seal = Seal.builder().id(TEST_SEAL_ID).status(SealApplicationStatus.SEAL_ACTIVE).build();

      CreateSealApplicationCommand command = new CreateSealApplicationCommand();
      command.setSealId(TEST_SEAL_ID);
      command.setCopies(101);

      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> sealApplicationAppService.createApplication(command));
      assertThat(exception.getMessage()).contains("不能超过100份");
    }

    @Test
    @DisplayName("审批人不能为空")
    void createApplication_shouldFail_whenApproverNull() {
      // Given
      Seal seal = Seal.builder().id(TEST_SEAL_ID).status(SealApplicationStatus.SEAL_ACTIVE).build();

      CreateSealApplicationCommand command = new CreateSealApplicationCommand();
      command.setSealId(TEST_SEAL_ID);
      command.setApproverId(null);

      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<SealApplication> wrapper2 = any(LambdaQueryWrapper.class);
      when(applicationRepository.count(wrapper2)).thenReturn(0L);
      when(applicationRepository.save(any(SealApplication.class)))
          .thenAnswer(
              invocation -> {
                SealApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                return true;
              });

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> sealApplicationAppService.createApplication(command));
      assertThat(exception.getMessage()).contains("审批人不能为空");
    }
  }

  @Nested
  @DisplayName("审批测试")
  class ApprovalTests {

    @Test
    @DisplayName("应该成功审批通过")
    void approve_shouldSuccess() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("SA001")
              .status(SealApplicationStatus.PENDING)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      lenient().when(applicationRepository.updateById(any(SealApplication.class))).thenReturn(true);

      // When
      SealApplicationDTO result = sealApplicationAppService.approve(TEST_APPLICATION_ID, "审批通过");

      // Then
      assertThat(result).isNotNull();
      assertThat(application.getStatus()).isEqualTo(SealApplicationStatus.APPROVED);
      assertThat(application.getApprovedBy()).isEqualTo(TEST_USER_ID);
      assertThat(application.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("只能审批待审批的申请")
    void approve_shouldFail_whenNotPending() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .status(SealApplicationStatus.APPROVED)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> sealApplicationAppService.approve(TEST_APPLICATION_ID, "审批通过"));
      assertThat(exception.getMessage()).contains("只能审批待审批");
    }

    @Test
    @DisplayName("应该成功审批拒绝")
    void reject_shouldSuccess() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .status(SealApplicationStatus.PENDING)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      lenient().when(applicationRepository.updateById(any(SealApplication.class))).thenReturn(true);

      // When
      SealApplicationDTO result = sealApplicationAppService.reject(TEST_APPLICATION_ID, "审批拒绝");

      // Then
      assertThat(result).isNotNull();
      assertThat(application.getStatus()).isEqualTo(SealApplicationStatus.REJECTED);
    }
  }

  @Nested
  @DisplayName("登记用印测试")
  class RegisterUsageTests {

    @Test
    @DisplayName("应该成功登记用印")
    void registerUsage_shouldSuccess() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .sealId(TEST_SEAL_ID)
              .status(SealApplicationStatus.APPROVED)
              .build();

      Seal seal = Seal.builder().id(TEST_SEAL_ID).keeperId(TEST_USER_ID).build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);
      lenient().when(applicationRepository.updateById(any(SealApplication.class))).thenReturn(true);

      // When
      SealApplicationDTO result =
          sealApplicationAppService.registerUsage(TEST_APPLICATION_ID, "已用印");

      // Then
      assertThat(result).isNotNull();
      assertThat(application.getStatus()).isEqualTo(SealApplicationStatus.USED);
      assertThat(application.getUsedBy()).isEqualTo(TEST_USER_ID);
      assertThat(application.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("只能对已批准的申请登记用印")
    void registerUsage_shouldFail_whenNotApproved() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .sealId(TEST_SEAL_ID)
              .status(SealApplicationStatus.PENDING)
              .build();

      Seal seal = Seal.builder().id(TEST_SEAL_ID).keeperId(TEST_USER_ID).build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      lenient().when(sealRepository.getByIdOrThrow(eq(TEST_SEAL_ID), anyString())).thenReturn(seal);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> sealApplicationAppService.registerUsage(TEST_APPLICATION_ID, "已用印"));
      assertThat(exception.getMessage()).contains("只能对已批准的申请");
    }

    @Test
    @DisplayName("不能重复登记用印")
    void registerUsage_shouldFail_whenAlreadyUsed() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .sealId(TEST_SEAL_ID)
              .status(SealApplicationStatus.USED)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> sealApplicationAppService.registerUsage(TEST_APPLICATION_ID, "已用印"));
      assertThat(exception.getMessage()).contains("已经登记过用印");
    }
  }

  @Nested
  @DisplayName("取消申请测试")
  class CancelApplicationTests {

    @Test
    @DisplayName("应该成功取消申请")
    void cancelApplication_shouldSuccess() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicantId(TEST_USER_ID)
              .status(SealApplicationStatus.PENDING)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);
      lenient().when(applicationRepository.updateById(any(SealApplication.class))).thenReturn(true);

      // When
      sealApplicationAppService.cancelApplication(TEST_APPLICATION_ID);

      // Then
      assertThat(application.getStatus()).isEqualTo(SealApplicationStatus.CANCELLED);
    }

    @Test
    @DisplayName("只能取消待审批的申请")
    void cancelApplication_shouldFail_whenNotPending() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicantId(TEST_USER_ID)
              .status(SealApplicationStatus.APPROVED)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> sealApplicationAppService.cancelApplication(TEST_APPLICATION_ID));
      assertThat(exception.getMessage()).contains("只能取消待审批");
    }

    @Test
    @DisplayName("只能取消自己的申请")
    void cancelApplication_shouldFail_whenNotOwner() {
      // Given
      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicantId(999L) // 其他用户
              .status(SealApplicationStatus.PENDING)
              .build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> sealApplicationAppService.cancelApplication(TEST_APPLICATION_ID));
      assertThat(exception.getMessage()).contains("只能取消自己的申请");
    }
  }

  @Nested
  @DisplayName("查询申请测试")
  class QueryApplicationTests {

    @Test
    @DisplayName("应该成功查询申请列表")
    void listApplications_shouldSuccess() {
      // Given
      SealApplicationQueryDTO query = new SealApplicationQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      SealApplication application =
          SealApplication.builder()
              .id(TEST_APPLICATION_ID)
              .applicationNo("SA001")
              .status(SealApplicationStatus.PENDING)
              .build();

      Page<SealApplication> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(application));
      page.setTotal(1);

      @SuppressWarnings("unchecked")
      Page<SealApplication> pageParam = any(Page.class);
      when(applicationMapper.selectApplicationPage(pageParam, any(), any(), any(), any(), any()))
          .thenReturn(page);

      // When
      PageResult<SealApplicationDTO> result = sealApplicationAppService.listApplications(query);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getApplicationNo()).isEqualTo("SA001");
    }

    @Test
    @DisplayName("应该成功获取申请详情")
    void getApplicationById_shouldSuccess() {
      // Given
      SealApplication application =
          SealApplication.builder().id(TEST_APPLICATION_ID).applicationNo("SA001").build();

      when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString()))
          .thenReturn(application);

      // When
      SealApplicationDTO result = sealApplicationAppService.getApplicationById(TEST_APPLICATION_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getApplicationNo()).isEqualTo("SA001");
    }
  }

  @Nested
  @DisplayName("保管人权限测试")
  class KeeperPermissionTests {

    @Test
    @DisplayName("应该成功检查是否是印章保管人")
    void isKeeperOfSeal_shouldReturnTrue() {
      // Given
      Seal seal = Seal.builder().id(TEST_SEAL_ID).keeperId(TEST_USER_ID).build();

      when(sealRepository.getById(TEST_SEAL_ID)).thenReturn(seal);

      // When
      boolean result = sealApplicationAppService.isKeeperOfSeal(TEST_SEAL_ID);

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("应该成功检查是否是任何印章的保管人")
    void isAnySealKeeper_shouldReturnTrue() {
      // Given
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Seal> wrapper3 = any(LambdaQueryWrapper.class);
      when(sealRepository.count(wrapper3)).thenReturn(1L);

      // When
      boolean result = sealApplicationAppService.isAnySealKeeper();

      // Then
      assertThat(result).isTrue();
    }
  }
}
