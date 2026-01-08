package com.lawfirm.application.workbench.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.workbench.entity.Report;
import com.lawfirm.domain.workbench.repository.ReportRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.report.ExcelReportGenerator;
import com.lawfirm.infrastructure.external.report.PdfReportGenerator;
import com.lawfirm.infrastructure.persistence.mapper.ReportMapper;
import com.lawfirm.infrastructure.persistence.mapper.StatisticsMapper;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    // ObjectMapper由Spring Boot自动配置
    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 获取可用报表列表
     */
    public List<Map<String, Object>> getAvailableReports() {
        List<Map<String, Object>> reports = new ArrayList<>();

        // 收入报表
        Map<String, Object> revenueReport = new HashMap<>();
        revenueReport.put("type", "REVENUE");
        revenueReport.put("name", "收入报表");
        revenueReport.put("description", "统计收入情况，支持按时间、客户、案件等维度");
        revenueReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(revenueReport);

        // 案件报表
        Map<String, Object> matterReport = new HashMap<>();
        matterReport.put("type", "MATTER");
        matterReport.put("name", "案件报表");
        matterReport.put("description", "统计案件情况，包括案件数量、状态分布等");
        matterReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterReport);

        // 客户报表
        Map<String, Object> clientReport = new HashMap<>();
        clientReport.put("type", "CLIENT");
        clientReport.put("name", "客户报表");
        clientReport.put("description", "统计客户情况，包括客户数量、类型分布等");
        clientReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(clientReport);

        // 律师业绩报表
        Map<String, Object> lawyerReport = new HashMap<>();
        lawyerReport.put("type", "LAWYER_PERFORMANCE");
        lawyerReport.put("name", "律师业绩报表");
        lawyerReport.put("description", "统计律师业绩，包括案件数、收入、工时等");
        lawyerReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(lawyerReport);

        // 应收报表
        Map<String, Object> receivableReport = new HashMap<>();
        receivableReport.put("type", "RECEIVABLE");
        receivableReport.put("name", "应收报表");
        receivableReport.put("description", "统计应收账款情况，包括客户、案件、应收金额、账龄等");
        receivableReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(receivableReport);

        // 项目进度报表（M3-025）
        Map<String, Object> matterProgressReport = new HashMap<>();
        matterProgressReport.put("type", "MATTER_PROGRESS");
        matterProgressReport.put("name", "项目进度报表");
        matterProgressReport.put("description", "统计项目进度情况，包括任务完成率、工时统计、进度状态等");
        matterProgressReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterProgressReport);

        // 项目工时报表（M3-026）
        Map<String, Object> matterTimesheetReport = new HashMap<>();
        matterTimesheetReport.put("type", "MATTER_TIMESHEET");
        matterTimesheetReport.put("name", "项目工时报表");
        matterTimesheetReport.put("description", "按项目统计工时情况，包括工时明细、工时汇总等");
        matterTimesheetReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterTimesheetReport);

        // 项目任务报表（M3-027）
        Map<String, Object> matterTaskReport = new HashMap<>();
        matterTaskReport.put("type", "MATTER_TASK");
        matterTaskReport.put("name", "项目任务报表");
        matterTaskReport.put("description", "按项目统计任务完成情况，包括任务分布、完成率等");
        matterTaskReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterTaskReport);

        // 项目阶段进度报表（M3-028）
        Map<String, Object> matterStageReport = new HashMap<>();
        matterStageReport.put("type", "MATTER_STAGE");
        matterStageReport.put("name", "项目阶段进度报表");
        matterStageReport.put("description", "按阶段统计项目进度，包括各阶段项目数量、平均进度等");
        matterStageReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterStageReport);

        // 项目趋势分析报表（M3-029）
        Map<String, Object> matterTrendReport = new HashMap<>();
        matterTrendReport.put("type", "MATTER_TREND");
        matterTrendReport.put("name", "项目趋势分析报表");
        matterTrendReport.put("description", "项目进度趋势分析，包括新增项目趋势、完成项目趋势等");
        matterTrendReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(matterTrendReport);

        // 项目成本报表（M4-044）
        Map<String, Object> costReport = new HashMap<>();
        costReport.put("type", "COST_ANALYSIS");
        costReport.put("name", "项目成本分析报表");
        costReport.put("description", "项目成本分析，包括归集成本、分摊成本、成本类型分析等");
        costReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(costReport);

        // 账龄分析报表（M4-053）
        Map<String, Object> agingReport = new HashMap<>();
        agingReport.put("type", "AGING_ANALYSIS");
        agingReport.put("name", "应收账款账龄分析报表");
        agingReport.put("description", "应收账款账龄分析，按账龄区间（0-30天、31-60天、61-90天、90天以上）统计");
        agingReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(agingReport);

        // 利润分析报表（M4-054）
        Map<String, Object> profitReport = new HashMap<>();
        profitReport.put("type", "PROFIT_ANALYSIS");
        profitReport.put("name", "项目利润分析报表");
        profitReport.put("description", "项目利润分析，包括收入、成本、利润、利润率等详细分析");
        profitReport.put("formats", List.of("EXCEL", "PDF"));
        reports.add(profitReport);

        return reports;
    }

    /**
     * 分页查询报表记录
     */
    public PageResult<ReportDTO> listReports(ReportQueryDTO query) {
        IPage<Report> page = reportMapper.selectReportPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getReportType(),
                query.getStatus(),
                query.getGeneratedBy() != null ? query.getGeneratedBy() : SecurityUtils.getUserId()
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
     */
    public ReportDTO getReportById(Long id) {
        Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
        return toDTO(report);
    }

    /**
     * 生成报表
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
     * 获取报表下载URL（预签名URL）
     */
    public String getReportDownloadUrl(Long id) throws Exception {
        Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
        
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
        
        // 生成预签名URL（有效期1小时）
        return minioService.getPresignedUrl(objectName, 3600);
    }

    /**
     * 删除报表记录
     */
    @Transactional
    public void deleteReport(Long id) {
        Report report = reportRepository.getByIdOrThrow(id, "报表不存在");
        
        // 删除MinIO中的文件
        if (report.getFileUrl() != null) {
            try {
                String objectName = minioService.extractObjectName(report.getFileUrl());
                if (objectName != null) {
                    minioService.deleteFile(objectName);
                }
            } catch (Exception e) {
                log.warn("删除报表文件失败: {}", report.getFileUrl(), e);
            }
        }
        
        reportRepository.softDelete(id);
        log.info("删除报表记录: {}", id);
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

    /**
     * 获取部门及所有下级部门ID
     */
    private List<Long> getAllChildDepartmentIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        collectChildDeptIds(parentId, result);
        return result;
    }
    
    private void collectChildDeptIds(Long parentId, List<Long> result) {
        try {
            List<Long> childIds = approvalMapper.selectChildDeptIds(parentId);
            if (childIds != null && !childIds.isEmpty()) {
                result.addAll(childIds);
                for (Long childId : childIds) {
                    collectChildDeptIds(childId, result);
                }
            }
        } catch (Exception e) {
            log.warn("查询子部门失败: parentId={}", parentId, e);
        }
    }

    /**
     * 查询收入数据
     */
    private List<Map<String, Object>> queryRevenueData(Map<String, Object> parameters) {
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        Long clientId = parameters.get("clientId") != null ? 
                Long.parseLong(parameters.get("clientId").toString()) : null;
        
        return statisticsMapper.queryRevenueReportData(startDate, endDate, clientId);
    }

    /**
     * 查询案件数据
     */
    private List<Map<String, Object>> queryMatterData(Map<String, Object> parameters) {
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        String matterType = parameters.get("matterType") != null ? parameters.get("matterType").toString() : null;
        
        return statisticsMapper.queryMatterReportData(status, matterType);
    }

    /**
     * 查询客户数据
     */
    private List<Map<String, Object>> queryClientData(Map<String, Object> parameters) {
        String clientType = parameters.get("clientType") != null ? parameters.get("clientType").toString() : null;
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        
        return statisticsMapper.queryClientReportData(clientType, status);
    }

    /**
     * 查询律师业绩数据
     */
    private List<Map<String, Object>> queryLawyerPerformanceData(Map<String, Object> parameters) {
        Integer limit = parameters.get("limit") != null ? 
                Integer.parseInt(parameters.get("limit").toString()) : 10;
        // 从参数中获取可访问的项目ID列表（如果已设置）
        @SuppressWarnings("unchecked")
        List<Long> matterIds = (List<Long>) parameters.get("matterIds");
        return statisticsMapper.getLawyerPerformanceRanking(limit, matterIds);
    }

    /**
     * 查询应收数据
     */
    private List<Map<String, Object>> queryReceivableData(Map<String, Object> parameters) {
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        Long clientId = parameters.get("clientId") != null ? 
                Long.parseLong(parameters.get("clientId").toString()) : null;
        
        return statisticsMapper.queryReceivableReportData(clientId, startDate, endDate);
    }

    /**
     * 查询项目进度数据（M3-025）
     */
    private List<Map<String, Object>> queryMatterProgressData(Map<String, Object> parameters) {
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        String matterType = parameters.get("matterType") != null ? parameters.get("matterType").toString() : null;
        Long leadLawyerId = parameters.get("leadLawyerId") != null ? 
                Long.parseLong(parameters.get("leadLawyerId").toString()) : null;
        Long clientId = parameters.get("clientId") != null ? 
                Long.parseLong(parameters.get("clientId").toString()) : null;
        
        return statisticsMapper.queryMatterProgressReportData(status, matterType, leadLawyerId, clientId);
    }

    /**
     * 查询项目工时数据（M3-026）
     */
    private List<Map<String, Object>> queryMatterTimesheetData(Map<String, Object> parameters) {
        Long matterId = parameters.get("matterId") != null ? 
                Long.parseLong(parameters.get("matterId").toString()) : null;
        Long userId = parameters.get("userId") != null ? 
                Long.parseLong(parameters.get("userId").toString()) : null;
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        
        return statisticsMapper.queryMatterTimesheetReportData(matterId, userId, startDate, endDate);
    }

    /**
     * 查询项目任务数据（M3-027）
     */
    private List<Map<String, Object>> queryMatterTaskData(Map<String, Object> parameters) {
        Long matterId = parameters.get("matterId") != null ? 
                Long.parseLong(parameters.get("matterId").toString()) : null;
        Long assigneeId = parameters.get("assigneeId") != null ? 
                Long.parseLong(parameters.get("assigneeId").toString()) : null;
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        
        return statisticsMapper.queryMatterTaskReportData(matterId, assigneeId, status);
    }

    /**
     * 查询项目阶段进度数据（M3-028）
     */
    private List<Map<String, Object>> queryMatterStageData(Map<String, Object> parameters) {
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;
        String matterType = parameters.get("matterType") != null ? parameters.get("matterType").toString() : null;
        Long departmentId = parameters.get("departmentId") != null ? 
                Long.parseLong(parameters.get("departmentId").toString()) : null;
        
        return statisticsMapper.queryMatterStageReportData(status, matterType, departmentId);
    }

    /**
     * 查询项目趋势分析数据（M3-029）
     */
    private List<Map<String, Object>> queryMatterTrendData(Map<String, Object> parameters) {
        String startDate = parameters.get("startDate") != null ? parameters.get("startDate").toString() : null;
        String endDate = parameters.get("endDate") != null ? parameters.get("endDate").toString() : null;
        
        return statisticsMapper.queryMatterTrendReportData(startDate, endDate);
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

    /**
     * 生成报表编号
     */
    private String generateReportNo() {
        return "RPT" + System.currentTimeMillis();
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

