package com.lawfirm.application.workbench.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.AvailableReportDTO;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.domain.workbench.repository.ReportRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.report.ExcelReportGenerator;
import com.lawfirm.infrastructure.external.report.PdfReportGenerator;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import com.lawfirm.infrastructure.persistence.mapper.StatisticsMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

/** ReportAppService 单元测试 测试报表服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportAppService 报表服务测试")
class ReportAppServiceTest {

  private static final Long TEST_REPORT_ID = 100L;
  private static final Long TEST_USER_ID = 200L;

  @Mock private ExcelReportGenerator excelReportGenerator;

  @Mock private PdfReportGenerator pdfReportGenerator;

  @Mock private MinioService minioService;

  @Mock private StatisticsMapper statisticsMapper;

  @Mock private ReportRepository reportRepository;

  @Mock private ReportMapper reportMapper;

  @Mock private ApprovalMapper approvalMapper;

  @Mock private com.lawfirm.domain.matter.repository.MatterRepository matterRepository;

  @Mock private NotificationAppService notificationAppService;

  @InjectMocks private ReportAppService reportAppService;

  @Nested
  @DisplayName("获取可用报表列表测试")
  class GetAvailableReportsTests {

    @Test
    @DisplayName("应该成功获取可用报表列表")
    void getAvailableReports_shouldSuccess() {
      // When
      List<AvailableReportDTO> result = reportAppService.getAvailableReports();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.size()).isGreaterThan(0);
      // 验证包含常见的报表类型
      boolean hasRevenueReport = result.stream().anyMatch(r -> "REVENUE".equals(r.getType()));
      assertThat(hasRevenueReport).isTrue();
    }
  }

  @Nested
  @DisplayName("查询报表测试")
  class QueryReportTests {

    @Test
    @DisplayName("应该成功分页查询报表")
    void listReports_shouldSuccess() {
      // Given
      ReportQueryDTO query = new ReportQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Report report =
          Report.builder()
              .id(TEST_REPORT_ID)
              .reportNo("RPT2024001")
              .reportType("REVENUE")
              .status("COMPLETED")
              .generatedBy(TEST_USER_ID)
              .build();

      Page<Report> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(report));
      page.setTotal(1);

      when(reportMapper.selectReportPage(ArgumentMatchers.<Page<Report>>any(), any(), any(), any()))
          .thenReturn(page);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");

        // When
        var result = reportAppService.listReports(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
      }
    }

    @Test
    @DisplayName("应该成功根据ID查询报表")
    void getReportById_shouldSuccess() {
      // Given
      Report report =
          Report.builder()
              .id(TEST_REPORT_ID)
              .reportNo("RPT2024001")
              .reportType("REVENUE")
              .generatedBy(TEST_USER_ID)
              .build();

      when(reportRepository.getByIdOrThrow(eq(TEST_REPORT_ID), anyString())).thenReturn(report);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("ALL");

        // When
        ReportDTO result = reportAppService.getReportById(TEST_REPORT_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReportNo()).isEqualTo("RPT2024001");
      }
    }

    @Test
    @DisplayName("无权访问的报表应该抛出异常")
    void getReportById_shouldFail_whenNoPermission() {
      // Given
      Report report =
          Report.builder()
              .id(TEST_REPORT_ID)
              .generatedBy(999L) // 其他用户生成的
              .build();

      when(reportRepository.getByIdOrThrow(eq(TEST_REPORT_ID), anyString())).thenReturn(report);

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getDataScope).thenReturn("DEPT"); // 非ALL权限

        // When & Then
        BusinessException exception =
            assertThrows(
                BusinessException.class, () -> reportAppService.getReportById(TEST_REPORT_ID));
        assertThat(exception.getMessage()).contains("权限不足");
      }
    }
  }

  @Nested
  @DisplayName("提交报表生成任务测试")
  class SubmitReportGenerationTests {

    @Test
    @DisplayName("应该成功提交报表生成任务")
    void submitReportGeneration_shouldSuccess() {
      // Given
      GenerateReportCommand command = new GenerateReportCommand();
      command.setReportType("REVENUE");
      command.setFormat("EXCEL");
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("startDate", "2024-01-01");
      parameters.put("endDate", "2024-12-31");
      command.setParameters(parameters);

      when(reportRepository.save(any(Report.class)))
          .thenAnswer(
              invocation -> {
                Report report = invocation.getArgument(0);
                report.setId(TEST_REPORT_ID);
                return true;
              });

      try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
        mockedSecurityUtils.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
        mockedSecurityUtils.when(SecurityUtils::getRealName).thenReturn("测试用户");

        // When
        ReportDTO result = reportAppService.submitReportGeneration(command);

        // Then
        assertThat(result).isNotNull();
        verify(reportRepository).save(any(Report.class));
      }
    }
  }

  @Nested
  @DisplayName("查询报表状态测试")
  class GetReportStatusTests {

    @Test
    @DisplayName("应该成功查询报表状态")
    void getReportStatus_shouldSuccess() {
      // Given
      Report report =
          Report.builder().id(TEST_REPORT_ID).reportNo("RPT2024001").status("GENERATING").build();

      when(reportRepository.getByIdOrThrow(eq(TEST_REPORT_ID), anyString())).thenReturn(report);

      // When
      ReportDTO result = reportAppService.getReportStatus(TEST_REPORT_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo("GENERATING");
    }
  }
}
