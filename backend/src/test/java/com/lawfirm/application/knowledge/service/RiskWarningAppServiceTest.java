package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateRiskWarningCommand;
import com.lawfirm.application.knowledge.dto.RiskWarningDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.RiskWarning;
import com.lawfirm.domain.knowledge.repository.RiskWarningRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.RiskWarningMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RiskWarningAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskWarningAppService 风险预警服务测试")
class RiskWarningAppServiceTest {

    private static final Long TEST_WARNING_ID = 100L;
    private static final Long TEST_MATTER_ID = 200L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private RiskWarningRepository warningRepository;

    @Mock
    private RiskWarningMapper warningMapper;

    @Mock
    private MatterRepository matterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RiskWarningAppService warningAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("创建预警测试")
    class CreateWarningTests {

        @Test
        @DisplayName("应该成功创建风险预警")
        void createWarning_shouldSuccess() {
            // Given
            Matter matter = Matter.builder()
                    .id(TEST_MATTER_ID)
                    .name("测试项目")
                    .build();

            CreateRiskWarningCommand command = new CreateRiskWarningCommand();
            command.setMatterId(TEST_MATTER_ID);
            command.setRiskType(RiskWarning.TYPE_DEADLINE);
            command.setRiskLevel(RiskWarning.LEVEL_HIGH);
            command.setRiskDescription("风险描述");
            command.setWarningReason("预警原因");
            command.setSuggestedAction("建议措施");

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString())).thenReturn(matter);
            when(warningRepository.save(any(RiskWarning.class))).thenAnswer(invocation -> {
                RiskWarning warning = invocation.getArgument(0);
                warning.setId(TEST_WARNING_ID);
                return true;
            });

            Matter matterForDTO = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matterForDTO);

            // When
            RiskWarningDTO result = warningAppService.createWarning(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMatterId()).isEqualTo(TEST_MATTER_ID);
            assertThat(result.getRiskType()).isEqualTo(RiskWarning.TYPE_DEADLINE);
            assertThat(result.getRiskLevel()).isEqualTo(RiskWarning.LEVEL_HIGH);
            assertThat(result.getStatus()).isEqualTo(RiskWarning.STATUS_ACTIVE);
            verify(warningRepository).save(any(RiskWarning.class));
        }

        @Test
        @DisplayName("应该失败当项目不存在")
        void createWarning_shouldFail_whenMatterNotExists() {
            // Given
            CreateRiskWarningCommand command = new CreateRiskWarningCommand();
            command.setMatterId(TEST_MATTER_ID);

            when(matterRepository.getByIdOrThrow(eq(TEST_MATTER_ID), anyString()))
                    .thenThrow(new BusinessException("项目不存在"));

            // When & Then
            assertThrows(BusinessException.class, () -> warningAppService.createWarning(command));
        }
    }

    @Nested
    @DisplayName("确认预警测试")
    class AcknowledgeWarningTests {

        @Test
        @DisplayName("应该成功确认预警")
        void acknowledgeWarning_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(RiskWarning.STATUS_ACTIVE)
                    .build();

            when(warningRepository.getByIdOrThrow(eq(TEST_WARNING_ID), anyString())).thenReturn(warning);
            when(warningRepository.updateById(any(RiskWarning.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            RiskWarningDTO result = warningAppService.acknowledgeWarning(TEST_WARNING_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(warning.getStatus()).isEqualTo(RiskWarning.STATUS_ACKNOWLEDGED);
            assertThat(warning.getAcknowledgedAt()).isNotNull();
            assertThat(warning.getAcknowledgedBy()).isEqualTo(TEST_USER_ID);
            verify(warningRepository).updateById(warning);
        }
    }

    @Nested
    @DisplayName("解决预警测试")
    class ResolveWarningTests {

        @Test
        @DisplayName("应该成功解决预警")
        void resolveWarning_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(RiskWarning.STATUS_ACKNOWLEDGED)
                    .build();

            when(warningRepository.getByIdOrThrow(eq(TEST_WARNING_ID), anyString())).thenReturn(warning);
            when(warningRepository.updateById(any(RiskWarning.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            User user = User.builder().id(TEST_USER_ID).realName("解决人").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);
            when(userRepository.getById(TEST_USER_ID)).thenReturn(user);

            // When
            RiskWarningDTO result = warningAppService.resolveWarning(TEST_WARNING_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(warning.getStatus()).isEqualTo(RiskWarning.STATUS_RESOLVED);
            assertThat(warning.getResolvedAt()).isNotNull();
            assertThat(warning.getResolvedBy()).isEqualTo(TEST_USER_ID);
            verify(warningRepository).updateById(warning);
        }
    }

    @Nested
    @DisplayName("关闭预警测试")
    class CloseWarningTests {

        @Test
        @DisplayName("应该成功关闭预警")
        void closeWarning_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(RiskWarning.STATUS_RESOLVED)
                    .build();

            when(warningRepository.getByIdOrThrow(eq(TEST_WARNING_ID), anyString())).thenReturn(warning);
            when(warningRepository.updateById(any(RiskWarning.class))).thenReturn(true);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            RiskWarningDTO result = warningAppService.closeWarning(TEST_WARNING_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(warning.getStatus()).isEqualTo(RiskWarning.STATUS_CLOSED);
            verify(warningRepository).updateById(warning);
        }
    }

    @Nested
    @DisplayName("查询预警测试")
    class QueryWarningTests {

        @Test
        @DisplayName("应该成功获取预警详情")
        void getWarningById_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .riskType(RiskWarning.TYPE_QUALITY)
                    .riskLevel(RiskWarning.LEVEL_MEDIUM)
                    .status(RiskWarning.STATUS_ACTIVE)
                    .build();

            when(warningRepository.getByIdOrThrow(eq(TEST_WARNING_ID), anyString())).thenReturn(warning);

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            RiskWarningDTO result = warningAppService.getWarningById(TEST_WARNING_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_WARNING_ID);
        }

        @Test
        @DisplayName("应该成功获取项目的所有预警")
        void getWarningsByMatterId_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .build();

            when(warningMapper.selectByMatterId(TEST_MATTER_ID)).thenReturn(List.of(warning));

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            List<RiskWarningDTO> result = warningAppService.getWarningsByMatterId(TEST_MATTER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取活跃的预警")
        void getActiveWarnings_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .status(RiskWarning.STATUS_ACTIVE)
                    .build();

            when(warningMapper.selectActiveWarnings()).thenReturn(List.of(warning));

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            List<RiskWarningDTO> result = warningAppService.getActiveWarnings();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取高风险预警")
        void getHighRiskWarnings_shouldSuccess() {
            // Given
            RiskWarning warning = RiskWarning.builder()
                    .id(TEST_WARNING_ID)
                    .matterId(TEST_MATTER_ID)
                    .riskLevel(RiskWarning.LEVEL_HIGH)
                    .build();

            when(warningMapper.selectHighRiskWarnings()).thenReturn(List.of(warning));

            Matter matter = Matter.builder().id(TEST_MATTER_ID).name("测试项目").build();
            when(matterRepository.getById(TEST_MATTER_ID)).thenReturn(matter);

            // When
            List<RiskWarningDTO> result = warningAppService.getHighRiskWarnings();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }
    }
}
