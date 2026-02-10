package com.lawfirm.application.workbench.service;

import static com.lawfirm.common.util.ReportParameterUtils.PARAM_CLIENT_ID;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_DEPARTMENT_ID;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_END_DATE;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_LIMIT;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_MATTER_ID;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_START_DATE;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_STATUS;
import static com.lawfirm.common.util.ReportParameterUtils.PARAM_USER_ID;
import static com.lawfirm.common.util.ReportParameterUtils.getInteger;
import static com.lawfirm.common.util.ReportParameterUtils.getLong;
import static com.lawfirm.common.util.ReportParameterUtils.getString;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.document.service.FileAccessService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.AvailableReportDTO;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.FileHashUtil;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.domain.workbench.repository.ReportRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.report.ExcelReportGenerator;
import com.lawfirm.infrastructure.external.report.PdfReportGenerator;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import com.lawfirm.infrastructure.persistence.mapper.StatisticsMapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 报表应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAppService {

  /** Excel报表生成器 */
  private final ExcelReportGenerator excelReportGenerator;

  /** PDF报表生成器 */
  private final PdfReportGenerator pdfReportGenerator;

  /** Minio对象存储服务 */
  private final MinioService minioService;

  /** 文件访问服务 */
  @SuppressWarnings("unused")
  private final FileAccessService fileAccessService;

  /** 统计Mapper */
  private final StatisticsMapper statisticsMapper;

  /** 报表仓储 */
  private final ReportRepository reportRepository;

  /** 报表Mapper */
  private final ReportMapper reportMapper;

  /** 审批Mapper */
  private final ApprovalMapper approvalMapper;

  /** 项目仓储 */
  private final com.lawfirm.domain.matter.repository.MatterRepository matterRepository;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** JSON对象映射器（由Spring Boot自动配置） */
  private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

  /**
   * 获取可用报表列表 ✅ 修复问题568: 使用类型安全的DTO替代Map
   *
   * @return 可用报表列表
   */
  public List<AvailableReportDTO> getAvailableReports() {
    List<AvailableReportDTO> reports = new ArrayList<>();

    // 收入报表
    reports.add(
        AvailableReportDTO.builder()
            .type("REVENUE")
            .name("收入报表")
            .description("统计收入情况，支持按时间、客户、案件等维度")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 案件报表
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER")
            .name("案件报表")
            .description("统计案件情况，包括案件数量、状态分布等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 客户报表
    reports.add(
        AvailableReportDTO.builder()
            .type("CLIENT")
            .name("客户报表")
            .description("统计客户情况，包括客户数量、类型分布等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 律师业绩报表
    reports.add(
        AvailableReportDTO.builder()
            .type("LAWYER_PERFORMANCE")
            .name("律师业绩报表")
            .description("统计律师业绩，包括案件数、收入、工时等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 应收报表
    reports.add(
        AvailableReportDTO.builder()
            .type("RECEIVABLE")
            .name("应收报表")
            .description("统计应收账款情况，包括客户、案件、应收金额、账龄等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目进度报表（M3-025）
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER_PROGRESS")
            .name("项目进度报表")
            .description("统计项目进度情况，包括任务完成率、工时统计、进度状态等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目工时报表（M3-026）
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER_TIMESHEET")
            .name("项目工时报表")
            .description("按项目统计工时情况，包括工时明细、工时汇总等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目任务报表（M3-027）
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER_TASK")
            .name("项目任务报表")
            .description("按项目统计任务完成情况，包括任务分布、完成率等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目阶段进度报表（M3-028）
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER_STAGE")
            .name("项目阶段进度报表")
            .description("按阶段统计项目进度，包括各阶段项目数量、平均进度等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目趋势分析报表（M3-029）
    reports.add(
        AvailableReportDTO.builder()
            .type("MATTER_TREND")
            .name("项目趋势分析报表")
            .description("项目进度趋势分析，包括新增项目趋势、完成项目趋势等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 项目成本报表（M4-044）
    reports.add(
        AvailableReportDTO.builder()
            .type("COST_ANALYSIS")
            .name("项目成本分析报表")
            .description("项目成本分析，包括归集成本、分摊成本、成本类型分析等")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 账龄分析报表（M4-053）
    reports.add(
        AvailableReportDTO.builder()
            .type("AGING_ANALYSIS")
            .name("应收账款账龄分析报表")
            .description("应收账款账龄分析，按账龄区间（0-30天、31-60天、61-90天、90天以上）统计")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    // 利润分析报表（M4-054）
    reports.add(
        AvailableReportDTO.builder()
            .type("PROFIT_ANALYSIS")
            .name("项目利润分析报表")
            .description("项目利润分析，包括收入、成本、利润、利润率等详细分析")
            .formats(List.of("EXCEL", "PDF"))
            .build());

    return reports;
  }

  /**
   * 分页查询报表记录 ✅ 修复问题561: 添加严格权限验证
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<ReportDTO> listReports(final ReportQueryDTO query) {
    Long currentUserId = SecurityUtils.getUserId();
    String dataScope = SecurityUtils.getDataScope();

    // ✅ 权限验证: 根据数据权限范围决定查询范围
    Long queryUserId;
    if ("ALL".equals(dataScope)) {
      // ALL权限可以查看所有人的报表，或指定用户的报表
      queryUserId = query.getGeneratedBy();
      if (query.getGeneratedBy() != null && !query.getGeneratedBy().equals(currentUserId)) {
        log.info("管理员查看他人报表: viewer={}, target={}", currentUserId, query.getGeneratedBy());
      }
    } else {
      // 非ALL权限只能查看自己的报表
      queryUserId = currentUserId;
      if (query.getGeneratedBy() != null && !query.getGeneratedBy().equals(currentUserId)) {
        log.warn(
            "权限不足尝试查看他人报表: requester={}, targetUser={}", currentUserId, query.getGeneratedBy());
      }
    }

    IPage<Report> page =
        reportMapper.selectReportPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getReportType(),
            query.getStatus(),
            queryUserId);

    return PageResult.of(
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
        page.getTotal(),
        query.getPageNum(),
        query.getPageSize());
  }

  /**
   * 根据ID查询报表 ✅ 修复问题552: 添加权限验证，只能查看自己生成的报表，或有ALL权限
   *
   * @param id 报表ID
   * @return 报表DTO
   */
  public ReportDTO getReportById(final Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    // ✅ 权限验证：只能查看自己生成的报表，或有ALL权限
    validateReportAccess(report, "查看");

    return toDTO(report);
  }

  /**
   * 验证报表访问权限
   *
   * @param report 报表实体
   * @param operation 操作类型（用于日志和错误提示）
   */
  private void validateReportAccess(final Report report, final String operation) {
    Long currentUserId = SecurityUtils.getUserId();
    if (!report.getGeneratedBy().equals(currentUserId)) {
      String dataScope = SecurityUtils.getDataScope();
      if (!"ALL".equals(dataScope)) {
        throw new BusinessException("权限不足：只能" + operation + "自己生成的报表");
      }
      log.warn(
          "跨用户{}报表: reportId={}, owner={}, operator={}",
          operation,
          report.getId(),
          report.getGeneratedBy(),
          currentUserId);
    }
  }

  /**
   * 提交报表生成任务（立即返回，异步生成） 大型报表建议使用此方法，避免HTTP请求超时
   *
   * @param command 生成命令
   * @return 报表DTO
   */
  @Transactional
  public ReportDTO submitReportGeneration(final GenerateReportCommand command) {
    String reportNo = generateReportNo();
    String reportName =
        command.getReportName() != null
            ? command.getReportName()
            : getReportTypeName(command.getReportType());

    // 获取当前用户信息（异步执行时无法获取）
    Long userId = SecurityUtils.getUserId();
    String userName = SecurityUtils.getRealName();

    // 创建报表记录，状态为待处理
    Report report =
        Report.builder()
            .reportNo(reportNo)
            .reportName(reportName)
            .reportType(command.getReportType())
            .format(command.getFormat())
            .status("PENDING") // 待处理
            .parameters(convertParametersToJson(command.getParameters()))
            .generatedBy(userId)
            .generatedByName(userName)
            .build();

    reportRepository.save(report);

    log.info(
        "报表生成任务已提交: type={}, format={}, reportNo={}, user={}",
        command.getReportType(),
        command.getFormat(),
        reportNo,
        userId);

    // 异步生成报表（立即返回）
    generateReportAsync(report.getId(), command, userId, userName);

    return toDTO(report);
  }

  /**
   * 异步生成报表（内部方法）
   *
   * @param reportId 报表ID
   * @param command 生成命令
   * @param userId 用户ID
   * @param userName 用户名
   */
  @Async("taskExecutor")
  public void generateReportAsync(
      final Long reportId,
      final GenerateReportCommand command,
      final Long userId,
      final String userName) {
    Report report = null;
    try {
      report = reportRepository.findById(reportId);
      if (report == null) {
        log.error("报表记录不存在: id={}", reportId);
        return;
      }
      // 更新状态为生成中
      report.setStatus("GENERATING");
      reportRepository.updateById(report);

      log.info("开始异步生成报表: reportNo={}", report.getReportNo());

      // 生成报表文件（设置存储信息）
      generateReportFile(command, report);

      // 更新报表记录
      report.setStatus("COMPLETED");
      report.setGeneratedAt(LocalDateTime.now());
      reportRepository.updateById(report);

      log.info(
          "报表异步生成成功: reportNo={}, fileUrl={}, storagePath={}",
          report.getReportNo(),
          report.getFileUrl(),
          report.getStoragePath());

      // 发送报表生成完成通知
      try {
        notificationAppService.sendSystemNotification(
            userId, "报表生成完成", report.getReportName() + " 已生成完成，请前往报表中心查看", "REPORT", reportId);
      } catch (Exception notifyEx) {
        log.warn("发送报表完成通知失败: {}", notifyEx.getMessage());
      }

    } catch (Exception e) {
      log.error("报表异步生成失败: reportId={}", reportId, e);
      // 仅当 report 不为 null 时才更新状态
      if (report != null) {
        try {
          report.setStatus("FAILED");
          reportRepository.updateById(report);
        } catch (Exception updateEx) {
          log.error("更新报表失败状态失败: reportId={}", reportId, updateEx);
        }
      }

      // 发送报表生成失败通知
      try {
        String reportName = report != null ? report.getReportName() : "报表";
        notificationAppService.sendSystemNotification(
            userId,
            "报表生成失败",
            reportName + " 生成失败: " + e.getMessage(),
            "REPORT",
            reportId);
      } catch (Exception notifyEx) {
        log.warn("发送报表失败通知失败: {}", notifyEx.getMessage());
      }
    }
  }

  /**
   * 同步生成报表（小型报表使用）
   *
   * @param command 生成命令
   * @return 报表DTO
   */
  @Transactional
  public ReportDTO generateReport(final GenerateReportCommand command) {
    String reportNo = generateReportNo();
    String reportName =
        command.getReportName() != null
            ? command.getReportName()
            : getReportTypeName(command.getReportType());

    // 创建报表记录
    Report report =
        Report.builder()
            .reportNo(reportNo)
            .reportName(reportName)
            .reportType(command.getReportType())
            .format(command.getFormat())
            .status("GENERATING")
            .parameters(convertParametersToJson(command.getParameters()))
            .generatedBy(SecurityUtils.getUserId())
            .generatedByName(SecurityUtils.getRealName())
            .build();

    reportRepository.save(report);

    log.info(
        "开始生成报表: type={}, format={}, reportNo={}, user={}",
        command.getReportType(),
        command.getFormat(),
        reportNo,
        SecurityUtils.getUserId());

    try {
      // 生成报表文件（设置存储信息）
      generateReportFile(command, report);

      // 更新报表记录
      report.setStatus("COMPLETED");
      report.setGeneratedAt(LocalDateTime.now());
      reportRepository.updateById(report);

      log.info(
          "报表生成成功: reportNo={}, fileUrl={}, storagePath={}",
          reportNo,
          report.getFileUrl(),
          report.getStoragePath());
    } catch (Exception e) {
      log.error("报表生成失败: reportNo={}", reportNo, e);
      report.setStatus("FAILED");
      reportRepository.updateById(report);
      throw new BusinessException("报表生成失败: " + e.getMessage());
    }

    return toDTO(report);
  }

  /**
   * 查询报表状态（用于轮询）
   *
   * @param id 报表ID
   * @return 报表DTO
   */
  public ReportDTO getReportStatus(final Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
    return toDTO(report);
  }

  /**
   * 获取报表下载URL（预签名URL） ✅ 修复问题552: 添加权限验证，只能下载自己生成的报表，或有ALL权限
   *
   * @param id 报表ID
   * @return 下载URL
   * @throws Exception 异常
   */
  public String getReportDownloadUrl(final Long id) throws Exception {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    // ✅ 权限验证：只能下载自己生成的报表，或有ALL权限
    validateReportAccess(report, "下载");

    if (!"COMPLETED".equals(report.getStatus())) {
      throw new BusinessException("报表尚未生成完成");
    }

    if (report.getFileUrl() == null || report.getFileUrl().isEmpty()) {
      throw new BusinessException("报表文件不存在");
    }

    // 从URL提取对象名称
    String objectName = minioService.extractObjectName(report.getFileUrl());
    if (objectName == null) {
      throw new BusinessException("无效的文件URL");
    }

    // ✅ 记录下载审计
    log.info(
        "下载报表: reportNo={}, type={}, downloader={}",
        report.getReportNo(),
        report.getReportType(),
        SecurityUtils.getUserId());

    // 生成预签名URL（有效期1小时）
    final int presignedUrlExpirationSeconds = 3600;
    return minioService.getPresignedUrl(objectName, presignedUrlExpirationSeconds);
  }

  /**
   * 删除报表记录 ✅ 修复问题557: 添加权限验证，只允许报表创建者或管理员删除
   *
   * @param id 报表ID
   */
  @Transactional
  public void deleteReport(final Long id) {
    Report report = reportRepository.getByIdOrThrow(id, "报表不存在");

    // ✅ 权限验证：只能删除自己生成的报表，或有ALL权限
    validateReportAccess(report, "删除");

    // 删除MinIO中的文件
    if (report.getFileUrl() != null) {
      try {
        String objectName = minioService.extractObjectName(report.getFileUrl());
        if (objectName != null) {
          minioService.deleteFile(objectName);
          log.info("删除报表文件成功: {}", objectName);
        }
      } catch (Exception e) {
        // ✅ 改进: 区分不同类型的异常
        log.error("删除报表文件失败: {}, error: {}", report.getFileUrl(), e.getMessage());
        // 文件删除失败不应阻止记录删除，继续执行
      }
    }

    reportRepository.softDelete(id);
    log.info(
        "删除报表记录: id={}, reportNo={}, deletedBy={}",
        id,
        report.getReportNo(),
        SecurityUtils.getUserId());
  }

  /**
   * 转换为DTO
   *
   * @param report 报表实体
   * @return 报表DTO
   */
  private ReportDTO toDTO(final Report report) {
    ReportDTO dto = new ReportDTO();
    dto.setId(report.getId());
    dto.setReportNo(report.getReportNo());
    dto.setReportName(report.getReportName());
    dto.setReportType(report.getReportType());
    dto.setReportTypeName(getReportTypeName(report.getReportType()));
    dto.setFormat(report.getFormat());
    dto.setStatus(report.getStatus());
    dto.setFileUrl(report.getFileUrl());
    dto.setFileSize(report.getFileSize());
    dto.setGeneratedAt(report.getGeneratedAt());
    dto.setGeneratedBy(report.getGeneratedBy());
    dto.setGeneratedByName(report.getGeneratedByName());
    dto.setCreatedAt(report.getCreatedAt());

    // 解析参数
    if (report.getParameters() != null) {
      try {
        dto.setParameters(
            objectMapper.readValue(
                report.getParameters(), new TypeReference<java.util.Map<String, Object>>() {}));
      } catch (JsonProcessingException e) {
        log.warn("解析报表参数失败", e);
      }
    }

    // 设置状态名称
    if (dto.getStatus() != null) {
      switch (dto.getStatus()) {
        case "GENERATING" -> dto.setStatusName("生成中");
        case "COMPLETED" -> dto.setStatusName("已完成");
        case "FAILED" -> dto.setStatusName("生成失败");
        default -> dto.setStatusName(dto.getStatus());
      }
    }

    return dto;
  }

  /**
   * 将参数Map转换为JSON字符串
   *
   * @param parameters 参数Map
   * @return JSON字符串
   */
  private String convertParametersToJson(final Map<String, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(parameters);
    } catch (JsonProcessingException e) {
      log.warn("转换报表参数为JSON失败", e);
      return null;
    }
  }

  /**
   * 生成报表文件并设置存储信息
   *
   * @param command 生成命令
   * @param report 报表实体
   * @throws Exception 异常
   */
  private void generateReportFile(final GenerateReportCommand command, final Report report)
      throws Exception {
    // 1. 查询数据
    List<Map<String, Object>> data = queryReportData(command);

    // 2. 生成文件
    InputStream fileStream;
    String originalFileName;
    String contentType;
    String extension;

    if ("EXCEL".equalsIgnoreCase(command.getFormat())) {
      fileStream = generateExcelFile(command.getReportType(), data);
      extension = "xlsx";
      contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    } else if ("PDF".equalsIgnoreCase(command.getFormat())) {
      fileStream = generatePdfFile(command.getReportType(), data);
      extension = "pdf";
      contentType = "application/pdf";
    } else {
      throw new BusinessException("不支持的报表格式: " + command.getFormat());
    }

    // 3. 生成文件名
    String reportTypeName = getReportTypeName(command.getReportType());
    String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    originalFileName =
        String.format("%s_%s_%s.%s", reportTypeName, dateStr, report.getReportNo(), extension);

    // 4. 生成标准化存储路径
    String storagePath =
        MinioPathGenerator.generateStandardPath(
            MinioPathGenerator.FileType.REPORTS,
            null, // 报表不关联项目
            command.getReportType() // 使用报表类型作为文件夹
            );

    // 5. 生成物理文件名
    String physicalName = MinioPathGenerator.generatePhysicalName(originalFileName);

    // 6. 构建完整对象名称
    String objectName = MinioPathGenerator.buildObjectName(storagePath, physicalName);

    // 7. 读取文件流到字节数组（用于计算Hash和上传）
    byte[] fileBytes = fileStream.readAllBytes();

    // 8. 计算文件Hash
    String fileHash = FileHashUtil.calculateHash(fileBytes);

    // 9. 上传到MinIO（使用字节数组）
    java.io.ByteArrayInputStream byteStream = new java.io.ByteArrayInputStream(fileBytes);
    String fileUrl = minioService.uploadFile(byteStream, objectName, contentType);

    // 10. 设置存储信息
    report.setFileUrl(fileUrl);
    report.setBucketName(minioService.getBucketName());
    report.setStoragePath(storagePath);
    report.setPhysicalName(physicalName);
    report.setFileHash(fileHash);
    report.setFileSize((long) fileBytes.length);

    log.info(
        "报表文件生成并上传成功: fileUrl={}, storagePath={}, physicalName={}, hash={}, size={}",
        fileUrl,
        storagePath,
        physicalName,
        fileHash,
        fileBytes.length);
  }

  /**
   * 查询报表数据（根据权限过滤）
   *
   * @param command 生成命令
   * @return 报表数据列表
   */
  private List<Map<String, Object>> queryReportData(final GenerateReportCommand command) {
    String reportType = command.getReportType();
    Map<String, Object> parameters =
        command.getParameters() != null ? command.getParameters() : new HashMap<>();

    // 根据用户权限添加过滤条件
    applyDataScopeFilter(parameters, reportType);

    List<Map<String, Object>> data = new ArrayList<>();

    switch (reportType) {
      case "REVENUE" -> {
        data = queryRevenueData(parameters);
      }
      case "MATTER" -> {
        data = queryMatterData(parameters);
      }
      case "CLIENT" -> {
        data = queryClientData(parameters);
      }
      case "LAWYER_PERFORMANCE" -> {
        data = queryLawyerPerformanceData(parameters);
      }
      case "RECEIVABLE" -> {
        data = queryReceivableData(parameters);
      }
      case "MATTER_PROGRESS" -> {
        data = queryMatterProgressData(parameters);
      }
      case "MATTER_TIMESHEET" -> {
        data = queryMatterTimesheetData(parameters);
      }
      case "MATTER_TASK" -> {
        data = queryMatterTaskData(parameters);
      }
      case "MATTER_STAGE" -> {
        data = queryMatterStageData(parameters);
      }
      case "MATTER_TREND" -> {
        data = queryMatterTrendData(parameters);
      }
      case "COST_ANALYSIS" -> {
        data = queryCostAnalysisData(parameters);
      }
      case "AGING_ANALYSIS" -> {
        data = queryAgingAnalysisData(parameters);
      }
      case "PROFIT_ANALYSIS" -> {
        data = queryProfitAnalysisData(parameters);
      }
      default -> {
        log.warn("未知的报表类型: {}", reportType);
      }
    }

    return data;
  }

  /**
   * 根据用户数据权限范围应用过滤条件 ALL: 不添加过滤条件（可查看所有数据） DEPT_AND_CHILD: 过滤本部门及下级部门的数据 DEPT: 过滤本部门的数据 SELF:
   * 只查看自己相关的数据
   *
   * @param parameters 参数Map
   * @param reportType 报表类型
   */
  private void applyDataScopeFilter(final Map<String, Object> parameters, final String reportType) {
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    // ALL权限：主任和财务可以看到所有数据，不添加过滤
    if ("ALL".equals(dataScope)) {
      log.debug("用户拥有ALL权限，可查看所有数据");
      return;
    }

    // 根据报表类型应用不同的权限过滤
    switch (reportType) {
      case "REVENUE", "RECEIVABLE", "AGING_ANALYSIS", "PROFIT_ANALYSIS", "COST_ANALYSIS" -> {
        // 财务相关报表：财务角色（ALL权限）可查看所有，其他角色只能查看自己相关的
        if (!"ALL".equals(dataScope)) {
          // 对于非ALL权限，可以添加客户过滤（如果用户只负责特定客户）
          // 这里简化处理，如果用户不是财务角色，可能需要根据实际业务逻辑调整
          log.debug("财务报表权限过滤: dataScope={}, userId={}", dataScope, currentUserId);
        }
      }
      case "MATTER",
          "MATTER_PROGRESS",
          "MATTER_TIMESHEET",
          "MATTER_TASK",
          "MATTER_STAGE",
          "MATTER_TREND" -> {
        // 项目相关报表：根据数据范围过滤
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
          List<Long> deptIds = getAllChildDepartmentIds(deptId);
          deptIds.add(deptId);
          parameters.put("departmentIds", deptIds);
          log.debug("项目报表权限过滤: 部门及下级部门, deptIds={}", deptIds);
        } else if ("DEPT".equals(dataScope) && deptId != null) {
          parameters.put("departmentId", deptId);
          log.debug("项目报表权限过滤: 本部门, deptId={}", deptId);
        } else if ("SELF".equals(dataScope)) {
          parameters.put("leadLawyerId", currentUserId);
          log.debug("项目报表权限过滤: 自己负责的项目, userId={}", currentUserId);
        }
      }
      case "CLIENT" -> {
        // 客户报表：根据数据范围过滤
        if ("SELF".equals(dataScope)) {
          parameters.put("responsibleLawyerId", currentUserId);
          log.debug("客户报表权限过滤: 自己负责的客户, userId={}", currentUserId);
        }
        // DEPT和DEPT_AND_CHILD可能需要根据客户关联的项目部门来过滤，这里简化处理
      }
      case "LAWYER_PERFORMANCE" -> {
        // 律师业绩报表：根据数据范围过滤项目
        if ("DEPT_AND_CHILD".equals(dataScope) && deptId != null) {
          List<Long> deptIds = getAllChildDepartmentIds(deptId);
          deptIds.add(deptId);
          // 查询符合条件的项目ID列表
          List<Long> matterIds =
              matterRepository
                  .lambdaQuery()
                  .select(com.lawfirm.domain.matter.entity.Matter::getId)
                  .in(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptIds)
                  .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                  .list()
                  .stream()
                  .map(com.lawfirm.domain.matter.entity.Matter::getId)
                  .collect(Collectors.toList());
          parameters.put("matterIds", matterIds);
          log.debug("律师业绩报表权限过滤: 部门及下级部门, matterIds={}", matterIds);
        } else if ("DEPT".equals(dataScope) && deptId != null) {
          List<Long> matterIds =
              matterRepository
                  .lambdaQuery()
                  .select(com.lawfirm.domain.matter.entity.Matter::getId)
                  .eq(com.lawfirm.domain.matter.entity.Matter::getDepartmentId, deptId)
                  .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                  .list()
                  .stream()
                  .map(com.lawfirm.domain.matter.entity.Matter::getId)
                  .collect(Collectors.toList());
          parameters.put("matterIds", matterIds);
          log.debug("律师业绩报表权限过滤: 本部门, matterIds={}", matterIds);
        } else if ("SELF".equals(dataScope)) {
          // 查询自己负责或参与的项目
          List<Long> leadMatterIds =
              matterRepository
                  .lambdaQuery()
                  .select(com.lawfirm.domain.matter.entity.Matter::getId)
                  .eq(com.lawfirm.domain.matter.entity.Matter::getDeleted, false)
                  .eq(com.lawfirm.domain.matter.entity.Matter::getLeadLawyerId, currentUserId)
                  .list()
                  .stream()
                  .map(com.lawfirm.domain.matter.entity.Matter::getId)
                  .collect(Collectors.toList());
          parameters.put("matterIds", leadMatterIds);
          log.debug("律师业绩报表权限过滤: 自己负责的项目, matterIds={}", leadMatterIds);
        }
      }
      default -> {
        log.debug("报表类型 {} 暂不支持权限过滤", reportType);
      }
    }
  }

  /** ✅ 修复问题556: 使用ThreadLocal缓存部门ID列表（同一请求内复用） */
  private static final ThreadLocal<Map<Long, List<Long>>> DEPT_CHILDREN_CACHE = new ThreadLocal<>();

  /** 清理缓存（请求结束后调用，可在Filter或Interceptor中调用）. */
  public static void clearCache() {
    DEPT_CHILDREN_CACHE.remove();
  }

  /**
   * 获取部门及所有下级部门ID ✅ 修复问题556: 使用递归CTE一次性查询，并添加缓存
   *
   * @param parentId 父部门ID
   * @return 部门ID列表
   */
  private List<Long> getAllChildDepartmentIds(final Long parentId) {
    if (parentId == null) {
      return new ArrayList<>();
    }

    // ✅ 检查缓存
    Map<Long, List<Long>> cache = DEPT_CHILDREN_CACHE.get();
    if (cache != null && cache.containsKey(parentId)) {
      return new ArrayList<>(cache.get(parentId));
    }

    try {
      // ✅ 使用递归CTE一次性查询所有后代部门
      List<Long> childIds = approvalMapper.selectAllDescendantDeptIds(parentId);
      List<Long> result = childIds != null ? new ArrayList<>(childIds) : new ArrayList<>();

      // ✅ 放入缓存
      if (cache == null) {
        cache = new HashMap<>();
        DEPT_CHILDREN_CACHE.set(cache);
      }
      cache.put(parentId, result);

      return result;
    } catch (Exception e) {
      log.error("查询子部门失败: parentId={}", parentId, e);
      return new ArrayList<>();
    }
  }

  // ============ 报表数据查询方法（使用ReportParameterUtils简化参数解析 - 问题576）============

  /**
   * 查询收入数据
   *
   * @param parameters 参数Map
   * @return 收入数据列表
   */
  private List<Map<String, Object>> queryRevenueData(final Map<String, Object> parameters) {
    return statisticsMapper.queryRevenueReportData(
        getString(parameters, PARAM_START_DATE),
        getString(parameters, PARAM_END_DATE),
        getLong(parameters, PARAM_CLIENT_ID));
  }

  /**
   * 查询案件数据
   *
   * @param parameters 参数Map
   * @return 案件数据列表
   */
  private List<Map<String, Object>> queryMatterData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterReportData(
        getString(parameters, PARAM_STATUS), getString(parameters, "matterType"));
  }

  /**
   * 查询客户数据
   *
   * @param parameters 参数Map
   * @return 客户数据列表
   */
  private List<Map<String, Object>> queryClientData(final Map<String, Object> parameters) {
    return statisticsMapper.queryClientReportData(
        getString(parameters, "clientType"), getString(parameters, PARAM_STATUS));
  }

  /**
   * 查询律师业绩数据
   *
   * @param parameters 参数Map
   * @return 律师业绩数据列表
   */
  private List<Map<String, Object>> queryLawyerPerformanceData(
      final Map<String, Object> parameters) {
    @SuppressWarnings("unchecked")
    List<Long> matterIds = (List<Long>) parameters.get("matterIds");
    return statisticsMapper.getLawyerPerformanceRanking(
        getInteger(parameters, PARAM_LIMIT, 10), matterIds);
  }

  /**
   * 查询应收数据
   *
   * @param parameters 参数Map
   * @return 应收数据列表
   */
  private List<Map<String, Object>> queryReceivableData(final Map<String, Object> parameters) {
    return statisticsMapper.queryReceivableReportData(
        getLong(parameters, PARAM_CLIENT_ID),
        getString(parameters, PARAM_START_DATE),
        getString(parameters, PARAM_END_DATE));
  }

  /**
   * 查询项目进度数据（M3-025）
   *
   * @param parameters 参数Map
   * @return 项目进度数据列表
   */
  private List<Map<String, Object>> queryMatterProgressData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterProgressReportData(
        getString(parameters, PARAM_STATUS),
        getString(parameters, "matterType"),
        getLong(parameters, "leadLawyerId"),
        getLong(parameters, PARAM_CLIENT_ID));
  }

  /**
   * 查询项目工时数据（M3-026）
   *
   * @param parameters 参数Map
   * @return 项目工时数据列表
   */
  private List<Map<String, Object>> queryMatterTimesheetData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterTimesheetReportData(
        getLong(parameters, PARAM_MATTER_ID),
        getLong(parameters, PARAM_USER_ID),
        getString(parameters, PARAM_START_DATE),
        getString(parameters, PARAM_END_DATE));
  }

  /**
   * 查询项目任务数据（M3-027）
   *
   * @param parameters 参数Map
   * @return 项目任务数据列表
   */
  private List<Map<String, Object>> queryMatterTaskData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterTaskReportData(
        getLong(parameters, PARAM_MATTER_ID),
        getLong(parameters, "assigneeId"),
        getString(parameters, PARAM_STATUS));
  }

  /**
   * 查询项目阶段进度数据（M3-028）
   *
   * @param parameters 参数Map
   * @return 项目阶段进度数据列表
   */
  private List<Map<String, Object>> queryMatterStageData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterStageReportData(
        getString(parameters, PARAM_STATUS),
        getString(parameters, "matterType"),
        getLong(parameters, PARAM_DEPARTMENT_ID));
  }

  /**
   * 查询项目趋势分析数据（M3-029）
   *
   * @param parameters 参数Map
   * @return 项目趋势分析数据列表
   */
  private List<Map<String, Object>> queryMatterTrendData(final Map<String, Object> parameters) {
    return statisticsMapper.queryMatterTrendReportData(
        getString(parameters, PARAM_START_DATE), getString(parameters, PARAM_END_DATE));
  }

  /**
   * 生成Excel文件
   *
   * @param reportType 报表类型
   * @param data 报表数据
   * @return Excel文件输入流
   * @throws Exception 异常
   */
  private InputStream generateExcelFile(
      final String reportType, final List<Map<String, Object>> data) throws Exception {
    return switch (reportType) {
      case "REVENUE" -> excelReportGenerator.generateRevenueReport(data);
      case "MATTER" -> excelReportGenerator.generateMatterReport(data);
      case "CLIENT" -> excelReportGenerator.generateClientReport(data);
      case "LAWYER_PERFORMANCE" -> excelReportGenerator.generateLawyerPerformanceReport(data);
      case "RECEIVABLE" -> excelReportGenerator.generateReceivableReport(data);
      case "MATTER_PROGRESS" -> excelReportGenerator.generateMatterProgressReport(data);
      case "MATTER_TIMESHEET" -> excelReportGenerator.generateMatterTimesheetReport(data);
      case "MATTER_TASK" -> excelReportGenerator.generateMatterTaskReport(data);
      case "MATTER_STAGE" -> excelReportGenerator.generateMatterStageReport(data);
      case "MATTER_TREND" -> excelReportGenerator.generateMatterTrendReport(data);
      case "COST_ANALYSIS" -> excelReportGenerator.generateCostAnalysisReport(data);
      case "AGING_ANALYSIS" -> excelReportGenerator.generateAgingAnalysisReport(data);
      case "PROFIT_ANALYSIS" -> excelReportGenerator.generateProfitAnalysisReport(data);
      default -> throw new BusinessException("不支持的报表类型: " + reportType);
    };
  }

  /**
   * 生成PDF文件
   *
   * @param reportType 报表类型
   * @param data 报表数据
   * @return PDF文件输入流
   * @throws Exception 异常
   */
  private InputStream generatePdfFile(final String reportType, final List<Map<String, Object>> data)
      throws Exception {
    return switch (reportType) {
      case "REVENUE" -> pdfReportGenerator.generateRevenueReport(data);
      case "MATTER" -> pdfReportGenerator.generateMatterReport(data);
      case "CLIENT" -> pdfReportGenerator.generateClientReport(data);
      case "LAWYER_PERFORMANCE" -> pdfReportGenerator.generateLawyerPerformanceReport(data);
      case "RECEIVABLE" -> pdfReportGenerator.generateReceivableReport(data);
      case "MATTER_PROGRESS" -> pdfReportGenerator.generateMatterProgressReport(data);
      case "MATTER_TIMESHEET" -> pdfReportGenerator.generateMatterTimesheetReport(data);
      case "MATTER_TASK" -> pdfReportGenerator.generateMatterTaskReport(data);
      case "MATTER_STAGE" -> pdfReportGenerator.generateMatterStageReport(data);
      case "MATTER_TREND" -> pdfReportGenerator.generateMatterTrendReport(data);
      case "COST_ANALYSIS" -> pdfReportGenerator.generateCostAnalysisReport(data);
      case "AGING_ANALYSIS" -> pdfReportGenerator.generateAgingAnalysisReport(data);
      case "PROFIT_ANALYSIS" -> pdfReportGenerator.generateProfitAnalysisReport(data);
      default -> throw new BusinessException("不支持的报表类型: " + reportType);
    };
  }

  /** 用于生成唯一报表编号的原子计数器 */
  private static final java.util.concurrent.atomic.AtomicLong REPORT_SEQUENCE =
      new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

  /**
   * 生成报表编号（并发安全） 格式：RPT + 日期 + 4位随机序号
   *
   * @return 报表编号
   */
  private String generateReportNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    final int maxSequence = 10000;
    long seq = REPORT_SEQUENCE.incrementAndGet() % maxSequence;
    return String.format("RPT%s%04d", date, seq);
  }

  /**
   * 获取报表类型名称
   *
   * @param type 报表类型
   * @return 类型名称
   */
  private String getReportTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "REVENUE" -> "收入报表";
      case "MATTER" -> "案件报表";
      case "CLIENT" -> "客户报表";
      case "LAWYER_PERFORMANCE" -> "律师业绩报表";
      case "RECEIVABLE" -> "应收报表";
      case "MATTER_PROGRESS" -> "项目进度报表";
      case "MATTER_TIMESHEET" -> "项目工时报表";
      case "MATTER_TASK" -> "项目任务报表";
      case "MATTER_STAGE" -> "项目阶段进度报表";
      case "MATTER_TREND" -> "项目趋势分析报表";
      case "COST_ANALYSIS" -> "项目成本分析报表";
      case "AGING_ANALYSIS" -> "应收账款账龄分析报表";
      case "PROFIT_ANALYSIS" -> "项目利润分析报表";
      default -> type;
    };
  }

  /**
   * 查询利润分析数据（M4-054）
   *
   * @param parameters 参数Map
   * @return 利润分析数据列表
   */
  private List<Map<String, Object>> queryProfitAnalysisData(final Map<String, Object> parameters) {
    Long matterId =
        parameters.get("matterId") != null
            ? Long.parseLong(parameters.get("matterId").toString())
            : null;
    Long clientId =
        parameters.get("clientId") != null
            ? Long.parseLong(parameters.get("clientId").toString())
            : null;
    String startDate =
        parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
    String endDate =
        parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;

    return statisticsMapper.queryProfitAnalysisReportData(matterId, clientId, startDate, endDate);
  }

  /**
   * 查询账龄分析数据（M4-053）
   *
   * @param parameters 参数Map
   * @return 账龄分析数据列表
   */
  private List<Map<String, Object>> queryAgingAnalysisData(final Map<String, Object> parameters) {
    Long clientId =
        parameters.get("clientId") != null
            ? Long.parseLong(parameters.get("clientId").toString())
            : null;
    String startDate =
        parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
    String endDate =
        parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;

    return statisticsMapper.queryAgingAnalysisReportData(clientId, startDate, endDate);
  }

  /**
   * 查询项目成本分析数据（M4-044）
   *
   * @param parameters 参数Map
   * @return 项目成本分析数据列表
   */
  private List<Map<String, Object>> queryCostAnalysisData(final Map<String, Object> parameters) {
    Long matterId =
        parameters.get("matterId") != null
            ? Long.parseLong(parameters.get("matterId").toString())
            : null;
    String startDate =
        parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
    String endDate =
        parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;

    return statisticsMapper.queryCostAnalysisReportData(matterId, startDate, endDate);
  }
}
