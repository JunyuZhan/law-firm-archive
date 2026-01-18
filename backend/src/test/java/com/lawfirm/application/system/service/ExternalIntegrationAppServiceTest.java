package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.security.AesEncryptionService;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.repository.ExternalIntegrationRepository;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExternalIntegrationAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalIntegrationAppService 外部集成服务测试")
class ExternalIntegrationAppServiceTest {

    private static final Long TEST_INTEGRATION_ID = 100L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private ExternalIntegrationRepository integrationRepository;

    @Mock
    private ExternalIntegrationMapper integrationMapper;

    @Mock
    private AesEncryptionService encryptionService;

    @InjectMocks
    private ExternalIntegrationAppService integrationAppService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("testuser");
        ReflectionTestUtils.setField(integrationAppService, "connectTimeoutMs", 5000);
        ReflectionTestUtils.setField(integrationAppService, "readTimeoutMs", 5000);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    @Nested
    @DisplayName("查询集成配置测试")
    class QueryIntegrationTests {

        @Test
        @DisplayName("应该成功分页查询集成配置")
        void listIntegrations_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .integrationName("AI集成")
                    .integrationType(ExternalIntegration.TYPE_AI)
                    .enabled(true)
                    .build();

            @SuppressWarnings("unchecked")
            IPage<ExternalIntegration> page = mock(IPage.class);
            when(page.getRecords()).thenReturn(List.of(integration));
            when(page.getTotal()).thenReturn(1L);

            when(integrationMapper.selectPage(any(Page.class), any(), any(), any()))
                    .thenReturn(page);

            ExternalIntegrationQueryDTO query = new ExternalIntegrationQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            // When
            PageResult<ExternalIntegrationDTO> result = integrationAppService.listIntegrations(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取所有集成配置")
        void listAllIntegrations_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .build();

            when(integrationMapper.selectAllIntegrations()).thenReturn(List.of(integration));

            // When
            List<ExternalIntegrationDTO> result = integrationAppService.listAllIntegrations();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取指定类型的启用集成")
        void listEnabledByType_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationType(ExternalIntegration.TYPE_AI)
                    .enabled(true)
                    .build();

            when(integrationMapper.selectEnabledByType(ExternalIntegration.TYPE_AI))
                    .thenReturn(List.of(integration));

            // When
            List<ExternalIntegrationDTO> result = integrationAppService.listEnabledByType(ExternalIntegration.TYPE_AI);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功获取集成配置详情")
        void getIntegrationById_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .integrationName("AI集成")
                    .build();

            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);

            // When
            ExternalIntegrationDTO result = integrationAppService.getIntegrationById(TEST_INTEGRATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIntegrationCode()).isEqualTo("AI-001");
        }
    }

    @Nested
    @DisplayName("创建集成配置测试")
    class CreateIntegrationTests {

        @Test
        @DisplayName("应该成功创建集成配置")
        void createIntegration_shouldSuccess() {
            // Given
            UpdateExternalIntegrationCommand command = new UpdateExternalIntegrationCommand();
            command.setIntegrationCode("AI-001");
            command.setIntegrationName("AI集成");
            command.setIntegrationType(ExternalIntegration.TYPE_AI);
            command.setApiUrl("https://api.example.com");
            command.setApiKey("test-key");
            command.setApiSecret("test-secret");

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationMapper.selectByCode("AI-001")).thenReturn(null);
            when(encryptionService.encrypt("test-key")).thenReturn("encrypted-key");
            when(encryptionService.encrypt("test-secret")).thenReturn("encrypted-secret");
            when(integrationMapper.insert(any(ExternalIntegration.class))).thenReturn(1);

            // When
            ExternalIntegrationDTO result = integrationAppService.createIntegration(command);

            // Then
            assertThat(result).isNotNull();
            verify(encryptionService).encrypt("test-key");
            verify(encryptionService).encrypt("test-secret");
        }

        @Test
        @DisplayName("应该失败当编码已存在")
        void createIntegration_shouldFail_whenCodeExists() {
            // Given
            UpdateExternalIntegrationCommand command = new UpdateExternalIntegrationCommand();
            command.setIntegrationCode("AI-001");

            ExternalIntegration existing = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationMapper.selectByCode("AI-001")).thenReturn(existing);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> integrationAppService.createIntegration(command));
            assertThat(exception.getMessage()).contains("集成编码已存在");
        }
    }

    @Nested
    @DisplayName("更新集成配置测试")
    class UpdateIntegrationTests {

        @Test
        @DisplayName("应该成功更新集成配置")
        void updateIntegration_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .apiKey("old-encrypted-key")
                    .build();

            UpdateExternalIntegrationCommand command = new UpdateExternalIntegrationCommand();
            command.setId(TEST_INTEGRATION_ID);
            command.setApiUrl("https://new-api.example.com");
            command.setApiKey("new-key");

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);
            when(encryptionService.encrypt("new-key")).thenReturn("new-encrypted-key");
            when(integrationRepository.updateById(any(ExternalIntegration.class))).thenReturn(true);

            // When
            integrationAppService.updateIntegration(command);

            // Then
            assertThat(integration.getApiUrl()).isEqualTo("https://new-api.example.com");
            verify(encryptionService).encrypt("new-key");
            verify(integrationRepository).updateById(integration);
        }
    }

    @Nested
    @DisplayName("启用/禁用集成测试")
    class EnableDisableIntegrationTests {

        @Test
        @DisplayName("应该成功启用集成")
        void enableIntegration_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .apiUrl("https://api.example.com")
                    .apiKey("encrypted-key")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);
            when(integrationMapper.updateEnabled(TEST_INTEGRATION_ID, true)).thenReturn(1);

            // When
            integrationAppService.enableIntegration(TEST_INTEGRATION_ID);

            // Then
            verify(integrationMapper).updateEnabled(TEST_INTEGRATION_ID, true);
        }

        @Test
        @DisplayName("应该失败当API地址未配置")
        void enableIntegration_shouldFail_whenApiUrlEmpty() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .apiUrl("")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> integrationAppService.enableIntegration(TEST_INTEGRATION_ID));
            assertThat(exception.getMessage()).contains("请先配置API地址");
        }

        @Test
        @DisplayName("应该成功禁用集成")
        void disableIntegration_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .integrationCode("AI-001")
                    .build();

            securityUtilsMock.when(() -> SecurityUtils.hasAnyRole(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);
            when(integrationMapper.updateEnabled(TEST_INTEGRATION_ID, false)).thenReturn(1);

            // When
            integrationAppService.disableIntegration(TEST_INTEGRATION_ID);

            // Then
            verify(integrationMapper).updateEnabled(TEST_INTEGRATION_ID, false);
        }
    }

    @Nested
    @DisplayName("获取解密密钥测试")
    class GetDecryptedKeysTests {

        @Test
        @DisplayName("应该成功获取解密后的API密钥")
        void getDecryptedApiKey_shouldSuccess() {
            // Given
            ExternalIntegration integration = ExternalIntegration.builder()
                    .id(TEST_INTEGRATION_ID)
                    .apiKey("encrypted-key")
                    .build();

            when(integrationRepository.getByIdOrThrow(eq(TEST_INTEGRATION_ID), anyString()))
                    .thenReturn(integration);
            when(encryptionService.decrypt("encrypted-key")).thenReturn("decrypted-key");

            // When
            String result = integrationAppService.getDecryptedApiKey(TEST_INTEGRATION_ID);

            // Then
            assertThat(result).isEqualTo("decrypted-key");
            verify(encryptionService).decrypt("encrypted-key");
        }
    }
}
