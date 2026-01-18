package com.lawfirm.application.contract.service;

import com.lawfirm.application.contract.command.CreateContractTemplateCommand;
import com.lawfirm.application.contract.dto.ContractTemplateDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContractTemplateAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractTemplateAppService 合同模板服务测试")
class ContractTemplateAppServiceTest {

    private static final Long TEST_TEMPLATE_ID = 100L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private ContractTemplateRepository contractTemplateRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractTemplateAppService templateAppService;

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
    @DisplayName("查询模板测试")
    class QueryTemplateTests {

        @Test
        @DisplayName("应该成功获取所有启用的模板")
        void getActiveTemplates_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .templateNo("TPL-001")
                    .name("测试模板")
                    .templateType("CIVIL_PROXY")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            when(contractTemplateRepository.findActiveTemplates())
                    .thenReturn(List.of(template));

            // When
            List<ContractTemplateDTO> result = templateAppService.getActiveTemplates();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("测试模板");
        }

        @Test
        @DisplayName("应该成功获取所有模板")
        void getAllTemplates_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            when(contractTemplateRepository.findAllTemplates())
                    .thenReturn(List.of(template));

            // When
            List<ContractTemplateDTO> result = templateAppService.getAllTemplates();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该成功按类型获取模板")
        void getTemplatesByType_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("民事代理模板")
                    .templateType("CIVIL_PROXY")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            when(contractTemplateRepository.findByContractType("CIVIL_PROXY"))
                    .thenReturn(List.of(template));

            // When
            List<ContractTemplateDTO> result = templateAppService.getTemplatesByType("CIVIL_PROXY");

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("应该失败当合同类型为空")
        void getTemplatesByType_shouldFail_whenTypeEmpty() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> templateAppService.getTemplatesByType(""));
            assertThat(exception.getMessage()).contains("合同类型不能为空");
        }

        @Test
        @DisplayName("应该成功获取模板详情")
        void getTemplate_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .templateType("CIVIL_PROXY")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);

            // When
            ContractTemplateDTO result = templateAppService.getTemplate(TEST_TEMPLATE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("测试模板");
        }
    }

    @Nested
    @DisplayName("创建模板测试")
    class CreateTemplateTests {

        @Test
        @DisplayName("应该成功创建模板")
        void createTemplate_shouldSuccess() {
            // Given
            CreateContractTemplateCommand command = new CreateContractTemplateCommand();
            command.setName("新模板");
            command.setContractType("CIVIL_PROXY");
            command.setFeeType("FIXED");
            command.setContent("模板内容");
            command.setDescription("模板描述");

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.generateTemplateNo()).thenReturn("TPL-001");
            when(contractTemplateRepository.save(any(ContractTemplate.class))).thenAnswer(invocation -> {
                ContractTemplate template = invocation.getArgument(0);
                template.setId(TEST_TEMPLATE_ID);
                return true;
            });

            // When
            ContractTemplateDTO result = templateAppService.createTemplate(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新模板");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            verify(contractTemplateRepository).save(any(ContractTemplate.class));
        }

        @Test
        @DisplayName("应该失败当无权限")
        void createTemplate_shouldFail_whenNoPermission() {
            // Given
            CreateContractTemplateCommand command = new CreateContractTemplateCommand();
            command.setName("新模板");

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("USER"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> templateAppService.createTemplate(command));
            assertThat(exception.getMessage()).contains("权限不足");
        }
    }

    @Nested
    @DisplayName("更新模板测试")
    class UpdateTemplateTests {

        @Test
        @DisplayName("应该成功更新模板")
        void updateTemplate_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("原名称")
                    .templateType("CIVIL_PROXY")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            CreateContractTemplateCommand command = new CreateContractTemplateCommand();
            command.setName("新名称");
            command.setContractType("CRIMINAL_DEFENSE");
            command.setContent("新内容");

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(0L);
            when(contractTemplateRepository.updateById(any(ContractTemplate.class))).thenReturn(true);

            // When
            ContractTemplateDTO result = templateAppService.updateTemplate(TEST_TEMPLATE_ID, command);

            // Then
            assertThat(result).isNotNull();
            assertThat(template.getName()).isEqualTo("新名称");
            assertThat(template.getTemplateType()).isEqualTo("CRIMINAL_DEFENSE");
            verify(contractTemplateRepository).updateById(template);
        }

        @Test
        @DisplayName("应该警告当模板正在使用")
        void updateTemplate_shouldWarn_whenInUse() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            CreateContractTemplateCommand command = new CreateContractTemplateCommand();
            command.setName("新名称");

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(5L);
            when(contractTemplateRepository.updateById(any(ContractTemplate.class))).thenReturn(true);

            // When
            templateAppService.updateTemplate(TEST_TEMPLATE_ID, command);

            // Then
            verify(contractTemplateRepository).updateById(any(ContractTemplate.class));
        }
    }

    @Nested
    @DisplayName("切换状态测试")
    class ToggleStatusTests {

        @Test
        @DisplayName("应该成功切换模板状态")
        void toggleStatus_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(0L);
            when(contractTemplateRepository.updateById(any(ContractTemplate.class))).thenReturn(true);

            // When
            templateAppService.toggleStatus(TEST_TEMPLATE_ID);

            // Then
            assertThat(template.getStatus()).isEqualTo("INACTIVE");
            verify(contractTemplateRepository).updateById(template);
        }

        @Test
        @DisplayName("应该警告当禁用正在使用的模板")
        void toggleStatus_shouldWarn_whenDisablingInUse() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(3L);
            when(contractTemplateRepository.updateById(any(ContractTemplate.class))).thenReturn(true);

            // When
            templateAppService.toggleStatus(TEST_TEMPLATE_ID);

            // Then
            assertThat(template.getStatus()).isEqualTo("INACTIVE");
            verify(contractTemplateRepository).updateById(template);
        }
    }

    @Nested
    @DisplayName("删除模板测试")
    class DeleteTemplateTests {

        @Test
        @DisplayName("应该成功删除模板")
        void deleteTemplate_shouldSuccess() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .status("ACTIVE")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(0L);
            when(contractTemplateRepository.updateById(any(ContractTemplate.class))).thenReturn(true);

            // When
            templateAppService.deleteTemplate(TEST_TEMPLATE_ID);

            // Then
            assertThat(template.getDeleted()).isTrue();
            verify(contractTemplateRepository).updateById(template);
        }

        @Test
        @DisplayName("应该失败当模板正在使用")
        void deleteTemplate_shouldFail_whenInUse() {
            // Given
            ContractTemplate template = ContractTemplate.builder()
                    .name("测试模板")
                    .build();
            template.setId(TEST_TEMPLATE_ID);

            securityUtilsMock.when(SecurityUtils::getRoles).thenReturn(Set.of("ADMIN"));
            when(contractTemplateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
                    .thenReturn(template);
            when(contractRepository.countByTemplateId(TEST_TEMPLATE_ID)).thenReturn(5L);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> templateAppService.deleteTemplate(TEST_TEMPLATE_ID));
            assertThat(exception.getMessage()).contains("模板正在被");
        }
    }
}
