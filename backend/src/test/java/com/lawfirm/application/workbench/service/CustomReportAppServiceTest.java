package com.lawfirm.application.workbench.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.workbench.command.CreateReportTemplateCommand;
import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportTemplateDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.domain.workbench.repository.ReportTemplateRepository;
import com.lawfirm.infrastructure.persistence.mapper.ReportTemplateMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

/** CustomReportAppService 单元测试 测试自定义报表服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomReportAppService 自定义报表服务测试")
class CustomReportAppServiceTest {

  private static final Long TEST_TEMPLATE_ID = 100L;
  private static final Long TEST_USER_ID = 200L;
  private static final Long TEST_REPORT_ID = 300L;

  @Mock private ReportTemplateRepository templateRepository;

  @Mock private ReportTemplateMapper templateMapper;

  @Mock private ReportAppService reportAppService;

  private ObjectMapper objectMapper;

  @InjectMocks private CustomReportAppService customReportAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
    securityUtilsMock.when(SecurityUtils::getRealName).thenReturn("测试用户");

    // 使用真实的ObjectMapper实例
    objectMapper = new ObjectMapper();
    // 通过反射设置objectMapper字段
    try {
      java.lang.reflect.Field field = CustomReportAppService.class.getDeclaredField("objectMapper");
      field.setAccessible(true);
      field.set(customReportAppService, objectMapper);
    } catch (Exception e) {
      // Ignore
    }
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询报表模板测试")
  class QueryTemplateTests {

    @Test
    @DisplayName("应该成功分页查询报表模板")
    void listTemplates_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .templateName("测试模板")
              .dataSource("MATTER")
              .status("ACTIVE")
              .build();

      Page<ReportTemplate> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(template));
      page.setTotal(1L);

      when(templateMapper.selectTemplatePage(
              ArgumentMatchers.<Page<ReportTemplate>>any(), any(), any(), any(), eq(TEST_USER_ID)))
          .thenReturn(page);

      // When
      PageResult<ReportTemplateDTO> result =
          customReportAppService.listTemplates(1, 10, null, null, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("应该成功获取模板详情")
    void getTemplateById_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .templateName("测试模板")
              .dataSource("MATTER")
              .status("ACTIVE")
              .isSystem(false)
              .build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When
      ReportTemplateDTO result = customReportAppService.getTemplateById(TEST_TEMPLATE_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTemplateNo()).isEqualTo("TPL2024001");
      assertThat(result.getTemplateName()).isEqualTo("测试模板");
    }
  }

  @Nested
  @DisplayName("创建报表模板测试")
  class CreateTemplateTests {

    @Test
    @DisplayName("应该成功创建报表模板")
    void createTemplate_shouldSuccess() {
      // Given
      CreateReportTemplateCommand command = new CreateReportTemplateCommand();
      command.setTemplateName("新模板");
      command.setDescription("模板描述");
      command.setDataSource("MATTER");
      command.setFieldConfig(Collections.emptyList());
      command.setFilterConfig(Collections.emptyList());
      command.setGroupConfig(Collections.emptyList());
      command.setSortConfig(Collections.emptyList());
      command.setAggregateConfig(Collections.emptyList());

      when(templateRepository.existsByTemplateName("新模板", null)).thenReturn(false);
      when(templateRepository.save(any(ReportTemplate.class)))
          .thenAnswer(
              invocation -> {
                ReportTemplate template = invocation.getArgument(0);
                template.setId(TEST_TEMPLATE_ID);
                template.setTemplateNo("TPL2024001");
                return true;
              });

      // When
      ReportTemplateDTO result = customReportAppService.createTemplate(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getTemplateName()).isEqualTo("新模板");
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
      assertThat(result.getIsSystem()).isFalse();
      verify(templateRepository).save(any(ReportTemplate.class));
    }

    @Test
    @DisplayName("模板名称已存在应该失败")
    void createTemplate_shouldFail_whenNameExists() {
      // Given
      CreateReportTemplateCommand command = new CreateReportTemplateCommand();
      command.setTemplateName("已存在模板");

      when(templateRepository.existsByTemplateName("已存在模板", null)).thenReturn(true);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> customReportAppService.createTemplate(command));
      assertThat(exception.getMessage()).contains("模板名称已存在");
    }
  }

  @Nested
  @DisplayName("更新报表模板测试")
  class UpdateTemplateTests {

    @Test
    @DisplayName("应该成功更新报表模板")
    void updateTemplate_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .templateName("原名称")
              .isSystem(false)
              .build();

      CreateReportTemplateCommand command = new CreateReportTemplateCommand();
      command.setTemplateName("新名称");
      command.setDescription("新描述");
      command.setDataSource("CLIENT");
      command.setFieldConfig(Collections.emptyList());

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(templateRepository.existsByTemplateName("新名称", TEST_TEMPLATE_ID)).thenReturn(false);
      when(templateRepository.updateById(any(ReportTemplate.class))).thenReturn(true);

      // When
      ReportTemplateDTO result = customReportAppService.updateTemplate(TEST_TEMPLATE_ID, command);

      // Then
      assertThat(result).isNotNull();
      assertThat(template.getTemplateName()).isEqualTo("新名称");
      assertThat(template.getDescription()).isEqualTo("新描述");
      verify(templateRepository).updateById(template);
    }

    @Test
    @DisplayName("系统内置模板不允许修改")
    void updateTemplate_shouldFail_whenSystemTemplate() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).isSystem(true).build();

      CreateReportTemplateCommand command = new CreateReportTemplateCommand();
      command.setTemplateName("新名称");

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> customReportAppService.updateTemplate(TEST_TEMPLATE_ID, command));
      assertThat(exception.getMessage()).contains("系统内置模板不允许修改");
    }

    @Test
    @DisplayName("更新时模板名称重复应该失败")
    void updateTemplate_shouldFail_whenNameExists() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).templateName("原名称").isSystem(false).build();

      CreateReportTemplateCommand command = new CreateReportTemplateCommand();
      command.setTemplateName("已存在名称");

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(templateRepository.existsByTemplateName("已存在名称", TEST_TEMPLATE_ID)).thenReturn(true);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> customReportAppService.updateTemplate(TEST_TEMPLATE_ID, command));
      assertThat(exception.getMessage()).contains("模板名称已存在");
    }
  }

  @Nested
  @DisplayName("删除报表模板测试")
  class DeleteTemplateTests {

    @Test
    @DisplayName("应该成功删除报表模板")
    void deleteTemplate_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .isSystem(false)
              .build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(templateRepository.softDelete(TEST_TEMPLATE_ID)).thenReturn(true);

      // When
      customReportAppService.deleteTemplate(TEST_TEMPLATE_ID);

      // Then
      verify(templateRepository).softDelete(TEST_TEMPLATE_ID);
    }

    @Test
    @DisplayName("系统内置模板不允许删除")
    void deleteTemplate_shouldFail_whenSystemTemplate() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).isSystem(true).build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> customReportAppService.deleteTemplate(TEST_TEMPLATE_ID));
      assertThat(exception.getMessage()).contains("系统内置模板不允许删除");
    }
  }

  @Nested
  @DisplayName("模板状态管理测试")
  class TemplateStatusTests {

    @Test
    @DisplayName("应该成功修改模板状态")
    void changeTemplateStatus_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .status("ACTIVE")
              .isSystem(false)
              .build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(templateRepository.updateById(any(ReportTemplate.class))).thenReturn(true);

      // When
      customReportAppService.changeTemplateStatus(TEST_TEMPLATE_ID, "INACTIVE");

      // Then
      assertThat(template.getStatus()).isEqualTo("INACTIVE");
      verify(templateRepository).updateById(template);
    }

    @Test
    @DisplayName("系统内置模板不允许修改状态")
    void changeTemplateStatus_shouldFail_whenSystemTemplate() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).isSystem(true).build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> customReportAppService.changeTemplateStatus(TEST_TEMPLATE_ID, "INACTIVE"));
      assertThat(exception.getMessage()).contains("系统内置模板不允许修改状态");
    }
  }

  @Nested
  @DisplayName("生成报表测试")
  class GenerateReportTests {

    @Test
    @DisplayName("应该成功根据模板生成报表")
    void generateReportByTemplate_shouldSuccess() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateNo("TPL2024001")
              .templateName("测试模板")
              .dataSource("MATTER")
              .status("ACTIVE")
              .fieldConfig("{}")
              .filterConfig("{}")
              .groupConfig("{}")
              .sortConfig("{}")
              .aggregateConfig("{}")
              .build();

      ReportDTO reportDTO = new ReportDTO();
      reportDTO.setId(TEST_REPORT_ID);
      reportDTO.setReportNo("RPT2024001");
      reportDTO.setStatus("COMPLETED");

      Map<String, Object> parameters = new HashMap<>();
      parameters.put("startDate", "2024-01-01");
      parameters.put("endDate", "2024-12-31");

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(reportAppService.generateReport(any(GenerateReportCommand.class))).thenReturn(reportDTO);

      // When
      ReportDTO result =
          customReportAppService.generateReportByTemplate(TEST_TEMPLATE_ID, parameters, "EXCEL");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(TEST_REPORT_ID);
      verify(reportAppService).generateReport(any(GenerateReportCommand.class));
    }

    @Test
    @DisplayName("已停用的模板不能生成报表")
    void generateReportByTemplate_shouldFail_whenTemplateInactive() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder().id(TEST_TEMPLATE_ID).status("INACTIVE").build();

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () ->
                  customReportAppService.generateReportByTemplate(TEST_TEMPLATE_ID, null, "EXCEL"));
      assertThat(exception.getMessage()).contains("报表模板已停用");
    }

    @Test
    @DisplayName("应该使用默认格式EXCEL")
    void generateReportByTemplate_shouldUseDefaultFormat() {
      // Given
      ReportTemplate template =
          ReportTemplate.builder()
              .id(TEST_TEMPLATE_ID)
              .templateName("测试模板")
              .dataSource("MATTER")
              .status("ACTIVE")
              .fieldConfig("{}")
              .filterConfig("{}")
              .groupConfig("{}")
              .sortConfig("{}")
              .aggregateConfig("{}")
              .build();

      ReportDTO reportDTO = new ReportDTO();
      reportDTO.setId(TEST_REPORT_ID);

      when(templateRepository.getByIdOrThrow(eq(TEST_TEMPLATE_ID), anyString()))
          .thenReturn(template);
      when(reportAppService.generateReport(any(GenerateReportCommand.class))).thenReturn(reportDTO);

      // When
      customReportAppService.generateReportByTemplate(TEST_TEMPLATE_ID, null, null);

      // Then
      ArgumentCaptor<GenerateReportCommand> captor =
          ArgumentCaptor.forClass(GenerateReportCommand.class);
      verify(reportAppService).generateReport(captor.capture());
      assertThat(captor.getValue().getFormat()).isEqualTo("EXCEL");
    }
  }

  @Nested
  @DisplayName("数据源相关测试")
  class DataSourceTests {

    @Test
    @DisplayName("应该成功获取可用数据源列表")
    void getDataSources_shouldSuccess() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSources();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
      // 验证包含常见的数据源
      boolean hasMatterDataSource = result.stream().anyMatch(ds -> "MATTER".equals(ds.get("code")));
      assertThat(hasMatterDataSource).isTrue();
    }

    @Test
    @DisplayName("应该成功获取案件数据源字段")
    void getDataSourceFields_shouldSuccess_forMatter() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("MATTER");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
      // 验证包含常见字段
      boolean hasMatterNo = result.stream().anyMatch(f -> "matter_no".equals(f.get("field")));
      assertThat(hasMatterNo).isTrue();
    }

    @Test
    @DisplayName("应该成功获取客户数据源字段")
    void getDataSourceFields_shouldSuccess_forClient() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("CLIENT");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("应该成功获取财务数据源字段")
    void getDataSourceFields_shouldSuccess_forFinance() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("FINANCE");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("应该成功获取工时数据源字段")
    void getDataSourceFields_shouldSuccess_forTimesheet() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("TIMESHEET");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("应该成功获取员工数据源字段")
    void getDataSourceFields_shouldSuccess_forEmployee() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("EMPLOYEE");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("未知数据源应该返回空列表")
    void getDataSourceFields_shouldReturnEmpty_forUnknown() {
      // When
      List<Map<String, Object>> result = customReportAppService.getDataSourceFields("UNKNOWN");

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }
  }
}
