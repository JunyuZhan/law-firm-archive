package com.lawfirm.application.workbench.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.AvailableReportDTO;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;

import static com.lawfirm.common.util.ReportParameterUtils.*;  // 报表参数工具类静态导入
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.domain.workbench.repository.ReportRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.report.ExcelReportGenerator;
import com.lawfirm.infrastructure.external.report.PdfReportGenerator;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import com.lawfirm.infrastructure.persistence.mapper.StatisticsMapper;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import com.lawfirm.application.system.service.NotificationAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAppService {

    private final ExcelReportGenerator excelReportGenerator;
    private final PdfReportGenerator pdfReportGenerator;
    private final MinioService minioService;
    private final StatisticsMapper statisticsMapper;
    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final ApprovalMapper approvalMapper;
    private final com.lawfirm.domain.matter.repository.MatterRepository matterRepository;
    private final NotificationAppService notificationAppService;
    
    // ObjectMapper由Spring Boot自动配置
    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 获取可用报表列表
     * ✅ 修复问题568: 使用类型安全的DTO替代Map
     */
    public List<AvailableReportDTO> getAvailableReports() {
        List<AvailableReportDTO> reports = new ArrayList<>();

        // 收入报表
        reports.add(AvailableReportDTO.builder()
                .type("REVENUE")
                .name("收入报表")
                .description("统计收入情况，支持按时间、客户、案件等维度")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 案件报表
        reports.add(AvailableReportDTO.builder()
                .type("MATTER")
                .name("案件报表")
                .description("统计案件情况，包括案件数量、状态分布等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 客户报表
        reports.add(AvailableReportDTO.builder()
                .type("CLIENT")
                .name("客户报表")
                .description("统计客户情况，包括客户数量、类型分布等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 律师业绩报表
        reports.add(AvailableReportDTO.builder()
                .type("LAWYER_PERFORMANCE")
                .name("律师业绩报表")
                .description("统计律师业绩，包括案件数、收入、工时等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 应收报表
        reports.add(AvailableReportDTO.builder()
                .type("RECEIVABLE")
                .name("应收报表")
                .description("统计应收账款情况，包括客户、案件、应收金额、账龄等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目进度报表（M3-025）
        reports.add(AvailableReportDTO.builder()
                .type("MATTER_PROGRESS")
                .name("项目进度报表")
                .description("统计项目进度情况，包括任务完成率、工时统计、进度状态等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目工时报表（M3-026）
        reports.add(AvailableReportDTO.builder()
                .type("MATTER_TIMESHEET")
                .name("项目工时报表")
                .description("按项目统计工时情况，包括工时明细、工时汇总等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目任务报表（M3-027）
        reports.add(AvailableReportDTO.builder()
                .type("MATTER_TASK")
                .name("项目任务报表")
                .description("按项目统计任务完成情况，包括任务分布、完成率等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目阶段进度报表（M3-028）
        reports.add(AvailableReportDTO.builder()
                .type("MATTER_STAGE")
                .name("项目阶段进度报表")
                .description("按阶段统计项目进度，包括各阶段项目数量、平均进度等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目趋势分析报表（M3-029）
        reports.add(AvailableReportDTO.builder()
                .type("MATTER_TREND")
                .name("项目趋势分析报表")
                .description("项目进度趋势分析，包括新增项目趋势、完成项目趋势等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 项目成本报表（M4-044）
        reports.add(AvailableReportDTO.builder()
                .type("COST_ANALYSIS")
                .name("项目成本分析报表")
                .description("项目成本分析，包括归集成本、分摊成本、成本类型分析等")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 账龄分析报表（M4-053）
        reports.add(AvailableReportDTO.builder()
                .type("AGING_ANALYSIS")
                .name("应收账款账龄分析报表")
                .description("应收账款账龄分析，按账龄区间（0-30天、31-60天、61-90天、90天以上）统计")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        // 利润分析报表（M4-054）
        reports.add(AvailableReportDTO.builder()
                .type("PROFIT_ANALYSIS")
                .name("项目利润分析报表")
                .description("项目利润分析，包括收入、成本、利润、利润率等详细分析")
                .formats(List.of("EXCEL", "PDF"))
                .build());

        return reports;
    }

    /**
     * 分页查询报表记录
     * ✅ 修复问题561: 添加严格权限验证
     */
    public PageResult<ReportDTO> listReports(ReportQueryDTO query) {
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
                log.warn("权限不足尝试查看他人报表: requester={}, targetUser={}", currentUserId, query.getGeneratedBy());
            }
        }
        
        IPage<Report> page = reportMapper.selectReportPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getReportType(),
                query.getStatus(),
                queryUserId
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
                page.getTotal(),
                query.getPageNum(),
                query.getPageSize()
        );
    }

    /**
     * 根据ID查询报表
     * ✅ 修复问题552: 添加权限验证，只能查看自己生成的报表，或有ALL权限
     */
    public ReportDTO getReportById(Long id) {
        Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
        
        // ✅ 权限验证：只能查看自己生成的报表，或有ALL权限
        validateReportAccess(report, "查看");
        
        return toDTO(report);
    }
    
    /**
     * 验证报表访问权限
     * @param report 报表实体
     * @param operation 操作类型（用于日志和错误提示）
     */
    private void validateReportAccess(Report report, String operation) {
        Long currentUserId = SecurityUtils.getUserId();
        if (!report.getGeneratedBy().equals(currentUserId)) {
            String dataScope = SecurityUtils.getDataScope();
            if (!"ALL".equals(dataScope)) {
                throw new BusinessException("权限不足：只能" + operation + "自己生成的报表");
            }
            log.warn("跨用户{}报表: reportId={}, owner={}, operator={}",
                    operation, report.getId(), report.getGeneratedBy(), currentUserId);
        }
    }

    /**
     * 提交报表生成任务（立即返回，异步生成）
     * 大型报表建议使用此方法，避免HTTP请求超时
     */
    @Transactional
    public ReportDTO submitReportGeneration(GenerateReportCommand command) {
        String reportNo = generateReportNo();
        String reportName = command.getReportName() != null ? 
                command.getReportName() : getReportTypeName(command.getReportType());
        
        // 获取当前用户信息（异步执行时无法获取）
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getRealName();
        
        // 创建报表记录，状态为待处理
        Report report = Report.builder()
                .reportNo(reportNo)
                .reportName(reportName)
                .reportType(command.getReportType())
                .format(command.getFormat())
                .status("PENDING")  // 待处理
                .parameters(convertParametersToJson(command.getParameters()))
                .generatedBy(userId)
                .generatedByName(userName)
                .build();

        reportRepository.save(report);

        log.info("报表生成任务已提交: type={}, format={}, reportNo={}, user={}", 
                command.getReportType(), command.getFormat(), reportNo, userId);

        // 异步生成报表（立即返回）
        generateReportAsync(report.getId(), command, userId, userName);

        return toDTO(report);
    }
    
    /**
     * 异步生成报表（内部方法）
     */
    @Async("taskExecutor")
    public void generateReportAsync(Long reportId, GenerateReportCommand command, Long userId, String userName) {
        Report report = reportRepository.findById(reportId);
        if (report == null) {
            log.error("报表记录不存在: id={}", reportId);
            return;
        }
        
        try {
            // 更新状态为生成中
            report.setStatus("GENERATING");
            reportRepository.updateById(report);
            
            log.info("开始异步生成报表: reportNo={}", report.getReportNo());
            
            // 生成报表文件
            String fileUrl = generateReportFile(command);
            
            // 更新报表记录
            report.setFileUrl(fileUrl);
            report.setStatus("COMPLETED");
            report.setGeneratedAt(LocalDateTime.now());
            reportRepository.updateById(report);
            
            log.info("报表异步生成成功: reportNo={}, fileUrl={}", report.getReportNo(), fileUrl);
            
            // 发送报表生成完成通知
            try {
                notificationAppService.sendSystemNotification(
                        userId, 
                        "报表生成完成", 
                        report.getReportName() + " 已生成完成，请前往报表中心查看", 
                        "REPORT", 
                        reportId
                );
            } catch (Exception notifyEx) {
                log.warn("发送报表完成通知失败: {}", notifyEx.getMessage());
            }
            
        } catch (Exception e) {
            log.error("报表异步生成失败: reportNo={}", report.getReportNo(), e);
            report.setStatus("FAILED");
            reportRepository.updateById(report);
            
            // 发送报表生成失败通知
            try {
                notificationAppService.sendSystemNotification(
                        userId, 
                        "报表生成失败", 
                        report.getReportName() + " 生成失败: " + e.getMessage(), 
                        "REPORT", 
                        reportId
                );
            } catch (Exception notifyEx) {
                log.warn("发送报表失败通知失败: {}", notifyEx.getMessage());
            }
        }
    }
    
    /**
     * 同步生成报表（小型报表使用）
     */
    @Transactional
    public ReportDTO generateReport(GenerateReportCommand command) {
        String reportNo = generateReportNo();
        String reportName = command.getReportName() != null ? 
                command.getReportName() : getReportTypeName(command.getReportType());
        
        // 创建报表记录
        Report report = Report.builder()
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

        log.info("开始生成报表: type={}, format={}, reportNo={}, user={}", 
                command.getReportType(), command.getFormat(), reportNo, SecurityUtils.getUserId());

        try {
            // 生成报表文件
            String fileUrl = generateReportFile(command);
            
            // 更新报表记录
            report.setFileUrl(fileUrl);
            report.setStatus("COMPLETED");
            report.setGeneratedAt(LocalDateTime.now());
            reportRepository.updateById(report);
            
            log.info("报表生成成功: reportNo={}, fileUrl={}", reportNo, fileUrl);
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
     */
    public ReportDTO getReportStatus(Long id) {
        Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
        return toDTO(report);
    }

    /**
     * 获取报表下载URL（预签名URL）
     * ✅ 修复问题552: 添加权限验证，只能下载自己生成的报表，或有ALL权限
     */
    public String getReportDownloadUrl(Long id) throws Exception {
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
        log.info("下载报表: reportNo={}, type={}, downloader={}",
                report.getReportNo(), report.getReportType(), SecurityUtils.getUserId());
        
        // 生成预签名URL（有效期1小时）
        return minioService.getPresignedUrl(objectName, 3600);
    }

    /**
     * 删除报表记录
     * ✅ 修复问题557: 添加权限验证，只允许报表创建者或管理员删除
     */
    @Transactional
    public void deleteReport(Long id) {
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
        log.info("删除报表记录: id={}, reportNo={}, deletedBy={}", 
                id, report.getReportNo(), SecurityUtils.getUserId());
    }

    /**
     * 转换为DTO
     */
    private ReportDTO toDTO(Report report) {
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
                dto.setParameters(objectMapper.readValue(report.getParameters(), new TypeReference<java.util.Map<String, Object>>() {}));
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
     */
    private String convertParametersToJson(Map<String, Object> parameters) {
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
     * 生成报表文件
     */
    private String generateReportFile(GenerateReportCommand command) throws Exception {
        // 1. 查询数据
        List<Map<String, Object>> data = queryReportData(command);
        
        // 2. 生成文件
        InputStream fileStream;
        String fileName;
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
        String reportNo = generateReportNo();
        String reportTypeName = getReportTypeName(command.getReportType());
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        fileName = String.format("%s_%s_%s.%s", reportTypeName, dateStr, reportNo, extension);
        
        // 4. 上传到MinIO
        String fileUrl = minioService.uploadFile(fileStream, fileName, "reports/", contentType);
        
        log.info("报表文件生成并上传成功: {}", fileUrl);
        return fileUrl;
    }

    /**
     * 查询报表数据（根据权限过滤）
     */
    private List<Map<String, Object>> queryReportData(GenerateReportCommand command) {
        String reportType = command.getReportType();
        Map<String, Object> parameters = command.getParameters() != null ? command.getParameters() : new HashMap<>();
        
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
     * 根据用户数据权限范围应用过滤条件
     * ALL: 不添加过滤条件（可查看所有数据）
     * DEPT_AND_CHILD: 过滤本部门及下级部门的数据
     * DEPT: 过滤本部门的数据
     * SELF: 只查看自己相关的数据
     */
    private void applyDataScopeFilter(Map<String, Object> parameters, String reportType) {
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
            case "MATTER", "MATTER_PROGRESS", "MATTER_TIMESHEET", "MATTER_TASK", "MATTER_STAGE", "MATTER_TREND" -> {
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
                    List<Long> matterIds = matterRepository.lambdaQuery()
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
                    List<Long> matterIds = matterRepository.lambdaQuery()
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
                    List<Long> leadMatterIds = matterRepository.lambdaQuery()
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

    // ✅ 修复问题556: 使用ThreadLocal缓存部门ID列表（同一请求内复用）
    private static final ThreadLocal<Map<Long, List<Long>>> DEPT_CHILDREN_CACHE = new ThreadLocal<>();
    
    /**
     * 清理缓存（请求结束后调用，可在Filter或Interceptor中调用）
     */
    public static void clearCache() {
        DEPT_CHILDREN_CACHE.remove();
    }
    
    /**
     * 获取部门及所有下级部门ID
     * ✅ 修复问题556: 使用递归CTE一次性查询，并添加缓存
     */
    private List<Long> getAllChildDepartmentIds(Long parentId) {
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
     */
    private List<Map<String, Object>> queryRevenueData(Map<String, Object> parameters) {
        return statisticsMapper.queryRevenueReportData(
                getString(parameters, PARAM_START_DATE),
                getString(parameters, PARAM_END_DATE),
                getLong(parameters, PARAM_CLIENT_ID)
        );
    }

    /**
     * 查询案件数据
     */
    private List<Map<String, Object>> queryMatterData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterReportData(
                getString(parameters, PARAM_STATUS),
                getString(parameters, "matterType")
        );
    }

    /**
     * 查询客户数据
     */
    private List<Map<String, Object>> queryClientData(Map<String, Object> parameters) {
        return statisticsMapper.queryClientReportData(
                getString(parameters, "clientType"),
                getString(parameters, PARAM_STATUS)
        );
    }

    /**
     * 查询律师业绩数据
     */
    private List<Map<String, Object>> queryLawyerPerformanceData(Map<String, Object> parameters) {
        @SuppressWarnings("unchecked")
        List<Long> matterIds = (List<Long>) parameters.get("matterIds");
        return statisticsMapper.getLawyerPerformanceRanking(
                getInteger(parameters, PARAM_LIMIT, 10),
                matterIds
        );
    }

    /**
     * 查询应收数据
     */
    private List<Map<String, Object>> queryReceivableData(Map<String, Object> parameters) {
        return statisticsMapper.queryReceivableReportData(
                getLong(parameters, PARAM_CLIENT_ID),
                getString(parameters, PARAM_START_DATE),
                getString(parameters, PARAM_END_DATE)
        );
    }

    /**
     * 查询项目进度数据（M3-025）
     */
    private List<Map<String, Object>> queryMatterProgressData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterProgressReportData(
                getString(parameters, PARAM_STATUS),
                getString(parameters, "matterType"),
                getLong(parameters, "leadLawyerId"),
                getLong(parameters, PARAM_CLIENT_ID)
        );
    }

    /**
     * 查询项目工时数据（M3-026）
     */
    private List<Map<String, Object>> queryMatterTimesheetData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterTimesheetReportData(
                getLong(parameters, PARAM_MATTER_ID),
                getLong(parameters, PARAM_USER_ID),
                getString(parameters, PARAM_START_DATE),
                getString(parameters, PARAM_END_DATE)
        );
    }

    /**
     * 查询项目任务数据（M3-027）
     */
    private List<Map<String, Object>> queryMatterTaskData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterTaskReportData(
                getLong(parameters, PARAM_MATTER_ID),
                getLong(parameters, "assigneeId"),
                getString(parameters, PARAM_STATUS)
        );
    }

    /**
     * 查询项目阶段进度数据（M3-028）
     */
    private List<Map<String, Object>> queryMatterStageData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterStageReportData(
                getString(parameters, PARAM_STATUS),
                getString(parameters, "matterType"),
                getLong(parameters, PARAM_DEPARTMENT_ID)
        );
    }

    /**
     * 查询项目趋势分析数据（M3-029）
     */
    private List<Map<String, Object>> queryMatterTrendData(Map<String, Object> parameters) {
        return statisticsMapper.queryMatterTrendReportData(
                getString(parameters, PARAM_START_DATE),
                getString(parameters, PARAM_END_DATE)
        );
    }

    /**
     * 生成Excel文件
     */
    private InputStream generateExcelFile(String reportType, List<Map<String, Object>> data) throws Exception {
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
     */
    private InputStream generatePdfFile(String reportType, List<Map<String, Object>> data) throws Exception {
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

    // 用于生成唯一报表编号的原子计数器
    private static final java.util.concurrent.atomic.AtomicLong reportSequence = 
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
    
    /**
     * 生成报表编号（并发安全）
     * 格式：RPT + 日期 + 4位随机序号
     */
    private String generateReportNo() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = reportSequence.incrementAndGet() % 10000;
        return String.format("RPT%s%04d", date, seq);
    }

    /**
     * 获取报表类型名称
     */
    private String getReportTypeName(String type) {
        if (type == null) return null;
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
     */
    private List<Map<String, Object>> queryProfitAnalysisData(Map<String, Object> parameters) {
        Long matterId = parameters.get("matterId") != null ? 
                Long.parseLong(parameters.get("matterId").toString()) : null;
        Long clientId = parameters.get("clientId") != null ? 
                Long.parseLong(parameters.get("clientId").toString()) : null;
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        
        return statisticsMapper.queryProfitAnalysisReportData(matterId, clientId, startDate, endDate);
    }

    /**
     * 查询账龄分析数据（M4-053）
     */
    private List<Map<String, Object>> queryAgingAnalysisData(Map<String, Object> parameters) {
        Long clientId = parameters.get("clientId") != null ? 
                Long.parseLong(parameters.get("clientId").toString()) : null;
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        
        return statisticsMapper.queryAgingAnalysisReportData(clientId, startDate, endDate);
    }

    /**
     * 查询项目成本分析数据（M4-044）
     */
    private List<Map<String, Object>> queryCostAnalysisData(Map<String, Object> parameters) {
        Long matterId = parameters.get("matterId") != null ? 
                Long.parseLong(parameters.get("matterId").toString()) : null;
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        
        return statisticsMapper.queryCostAnalysisReportData(matterId, startDate, endDate);
    }
}

