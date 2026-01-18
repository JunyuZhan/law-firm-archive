package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.application.admin.command.CreateLetterApplicationCommand;
import com.lawfirm.application.admin.dto.LetterApplicationDTO;
import com.lawfirm.application.admin.dto.LetterTemplateDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.common.constant.LetterStatus;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.entity.LetterTemplate;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import com.lawfirm.domain.admin.repository.LetterTemplateRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.LetterApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LetterAppService 单元测试
 * 测试出函管理服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LetterAppService 出函服务测试")
class LetterAppServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_TEMPLATE_ID = 100L;
    private static final Long TEST_APPLICATION_ID = 200L;
    private static final Long TEST_MATTER_ID = 300L;
    private static final Long TEST_CLIENT_ID = 400L;

    @Mock
    private LetterTemplateRepository templateRepository;

    @Mock
    private LetterApplicationRepository applicationRepository;

    @Mock
    private LetterApplicationMapper applicationMapper;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private MatterClientRepository matterClientRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationAppService notificationAppService;

    @Mock
    private SysConfigAppService sysConfigAppService;

    @Mock
    private ApprovalService approvalService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CauseOfActionService causeOfActionService;

    @Mock
    private MatterAppService matterAppService;

    @Mock
    private com.lawfirm.application.admin.util.LetterTemplateFormatter letterTemplateFormatter;

    @InjectMocks
    private LetterAppService letterAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() throws Exception {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getRealName).thenReturn("测试用户");
        securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(10L);

        // 通过反射设置matterAppService
        java.lang.reflect.Field field = LetterAppService.class.getDeclaredField("matterAppService");
        field.setAccessible(true);
        field.set(letterAppService, matterAppService);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("模板管理测试")
    class TemplateManagementTests {

        @Test
        @DisplayName("应该成功创建模板")
        void createTemplate_shouldSuccess() {
            // Given
            when(templateRepository.save(any(LetterTemplate.class))).thenAnswer(invocation -> {
                LetterTemplate template = invocation.getArgument(0);
                template.setId(TEST_TEMPLATE_ID);
                template.setTemplateNo("LT2024001");
                return true;
            });

            // When
            LetterTemplateDTO result = letterAppService.createTemplate("律师函模板", "LAWYER_LETTER", "内容", "描述");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("律师函模板");
            verify(templateRepository).save(any(LetterTemplate.class));
        }

        @Test
        @DisplayName("应该成功更新模板")
        void updateTemplate_shouldSuccess() {
            // Given
            LetterTemplate template = LetterTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("原名称")
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateRepository.updateById(any(LetterTemplate.class))).thenReturn(true);

            // When
            LetterTemplateDTO result = letterAppService.updateTemplate(TEST_TEMPLATE_ID, "新名称", null, null, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(template.getName()).isEqualTo("新名称");
            verify(templateRepository).updateById(template);
        }

        @Test
        @DisplayName("应该成功切换模板状态")
        void toggleTemplateStatus_shouldSuccess() {
            // Given
            LetterTemplate template = LetterTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .status(LetterStatus.TEMPLATE_ACTIVE)
                    .build();

            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(templateRepository.updateById(any(LetterTemplate.class))).thenReturn(true);

            // When
            letterAppService.toggleTemplateStatus(TEST_TEMPLATE_ID);

            // Then
            assertThat(template.getStatus()).isEqualTo(LetterStatus.TEMPLATE_DISABLED);
            verify(templateRepository).updateById(template);
        }

        @Test
        @DisplayName("应该成功获取启用的模板列表")
        void listActiveTemplates_shouldSuccess() {
            // Given
            LetterTemplate template = LetterTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .name("模板1")
                    .status(LetterStatus.TEMPLATE_ACTIVE)
                    .build();

            when(templateRepository.list(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(template));

            // When
            List<LetterTemplateDTO> result = letterAppService.listActiveTemplates();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("模板1");
        }
    }

    @Nested
    @DisplayName("创建出函申请测试")
    class CreateApplicationTests {

        @Test
        @DisplayName("应该成功创建出函申请")
        void createApplication_shouldSuccess() {
            // Given
            CreateLetterApplicationCommand command = new CreateLetterApplicationCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterId(TEST_MATTER_ID);
            command.setTargetUnit("目标单位");
            command.setPurpose("调查取证");

            LetterTemplate template = LetterTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .letterType("LAWYER_LETTER")
                    .content("模板内容")
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .clientId(TEST_CLIENT_ID)
                    .status(MatterConstants.STATUS_ACTIVE)
                    .name("案件1")
                    .build();

            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(userMapper.selectById(any())).thenReturn(null);
            when(letterTemplateFormatter.isStructuredFormat(anyString())).thenReturn(false);
            when(applicationRepository.save(any(LetterApplication.class))).thenAnswer(invocation -> {
                LetterApplication app = invocation.getArgument(0);
                app.setId(TEST_APPLICATION_ID);
                app.setApplicationNo("HF2024001");
                return true;
            });
            lenient().when(approvalService.createApproval(anyString(), anyLong(), anyString(),
                    anyString(), anyLong(), anyString(), anyString(), any())).thenReturn(1L);

            // When
            LetterApplicationDTO result = letterAppService.createApplication(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTargetUnit()).isEqualTo("目标单位");
            assertThat(result.getStatus()).isEqualTo(LetterStatus.PENDING);
            verify(applicationRepository).save(any(LetterApplication.class));
        }

        @Test
        @DisplayName("非进行中的项目不能申请出函")
        void createApplication_shouldFail_whenMatterNotActive() {
            // Given
            CreateLetterApplicationCommand command = new CreateLetterApplicationCommand();
            command.setTemplateId(TEST_TEMPLATE_ID);
            command.setMatterId(TEST_MATTER_ID);

            LetterTemplate template = LetterTemplate.builder()
                    .id(TEST_TEMPLATE_ID)
                    .build();

            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .status("ARCHIVED") // 已归档
                    .build();

            lenient().doNothing().when(matterAppService).validateMatterOwnership(TEST_MATTER_ID);
            when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString())).thenReturn(template);
            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> letterAppService.createApplication(command));
            assertThat(exception.getMessage()).contains("只能为进行中的项目");
        }
    }

    @Nested
    @DisplayName("审批出函申请测试")
    class ApproveApplicationTests {

        @Test
        @DisplayName("应该成功审批通过")
        void approve_shouldSuccess() {
            // Given
            LetterApplication application = LetterApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("HF2024001")
                    .status(LetterStatus.PENDING)
                    .applicantId(TEST_USER_ID)
                    .build();

            when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(applicationRepository.updateById(any(LetterApplication.class))).thenReturn(true);
            lenient().doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(),
                    anyString(), anyString(), anyLong());

            // When
            letterAppService.approve(TEST_APPLICATION_ID, "同意");

            // Then
            assertThat(application.getStatus()).isEqualTo(LetterStatus.APPROVED);
            assertThat(application.getApprovedBy()).isEqualTo(TEST_USER_ID);
            assertThat(application.getApprovedAt()).isNotNull();
            verify(applicationRepository).updateById(application);
        }

        @Test
        @DisplayName("应该成功拒绝申请")
        void reject_shouldSuccess() {
            // Given
            LetterApplication application = LetterApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .status(LetterStatus.PENDING)
                    .applicantId(TEST_USER_ID)
                    .build();

            when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(applicationRepository.updateById(any(LetterApplication.class))).thenReturn(true);
            lenient().doNothing().when(notificationAppService).sendSystemNotification(anyLong(), anyString(),
                    anyString(), anyString(), anyLong());

            // When
            letterAppService.reject(TEST_APPLICATION_ID, "不符合要求");

            // Then
            assertThat(application.getStatus()).isEqualTo(LetterStatus.REJECTED);
            verify(applicationRepository).updateById(application);
        }

        @Test
        @DisplayName("非待审批状态不能审批")
        void approve_shouldFail_whenNotPending() {
            // Given
            LetterApplication application = LetterApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .status(LetterStatus.APPROVED)
                    .build();

            when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> letterAppService.approve(TEST_APPLICATION_ID, ""));
            assertThat(exception.getMessage()).contains("只能审批待审批状态");
        }
    }

    @Nested
    @DisplayName("查询出函申请测试")
    class QueryApplicationTests {

        @Test
        @DisplayName("应该成功获取申请详情")
        void getById_shouldSuccess() {
            // Given
            LetterApplication application = LetterApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .applicationNo("HF2024001")
                    .matterId(TEST_MATTER_ID)
                    .build();

            when(applicationRepository.getByIdOrThrow(eq(TEST_APPLICATION_ID), anyString())).thenReturn(application);
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(new Matter());
            lenient().when(clientRepository.findById(any())).thenReturn(null);
            lenient().when(contractRepository.getById(any())).thenReturn(null);

            // When
            LetterApplicationDTO result = letterAppService.getById(TEST_APPLICATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getApplicationNo()).isEqualTo("HF2024001");
        }

        @Test
        @DisplayName("应该成功查询项目的出函记录")
        void listByMatter_shouldSuccess() {
            // Given
            LetterApplication application = LetterApplication.builder()
                    .id(TEST_APPLICATION_ID)
                    .matterId(TEST_MATTER_ID)
                    .build();

            when(applicationMapper.selectByMatterId(TEST_MATTER_ID))
                    .thenReturn(Collections.singletonList(application));
            when(matterRepository.findById(TEST_MATTER_ID)).thenReturn(new Matter());
            lenient().when(clientRepository.findById(any())).thenReturn(null);
            lenient().when(contractRepository.getById(any())).thenReturn(null);

            // When
            List<LetterApplicationDTO> result = letterAppService.listByMatter(TEST_MATTER_ID);

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
