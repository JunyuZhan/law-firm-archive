package com.lawfirm.application.workbench.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义报表应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomReportAppService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateMapper templateMapper;
    private final ReportAppService reportAppService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询报表模板
     */
    public PageResult<ReportTemplateDTO> listTemplates(int pageNum, int pageSize, 
                                                        String keyword, String dataSource, String status) {
        IPage<ReportTemplate> page = templateMapper.selectTemplatePage(
                new Page<>(pageNum, pageSize),
                keyword, dataSource, status, SecurityUtils.getUserId()
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toTemplateDTO).collect(Collectors.toList()),
                page.getTotal(),
                pageNum,
                pageSize
        );
    }

    /**
     * 获取模板详情
     */
    public ReportTemplateDTO getTemplateById(Long id) {
        ReportTemplate template = templateRepository.getByIdOrThrow(id, "报表模板不存在");
        return toTemplateDTO(template);
    }

    /**
     * 创建报表模板
     */
    @Transactional
    public ReportTemplateDTO createTemplate(CreateReportTemplateCommand command) {
        // 检查模板名称是否重复
        if (templateRepository.existsByTemplateName(command.getTemplateName(), null)) {
            throw new BusinessException("模板名称已存在");
        }

        ReportTemplate template = ReportTemplate.builder()
                .templateNo(generateTemplateNo())
                .templateName(command.getTemplateName())
                .description(command.getDescription())
                .dataSource(command.getDataSource())
                .fieldConfig(toJson(command.getFieldConfig()))
                .filterConfig(toJson(command.getFilterConfig()))
                .groupConfig(toJson(command.getGroupConfig()))
                .sortConfig(toJson(command.getSortConfig()))
                .aggregateConfig(toJson(command.getAggregateConfig()))
                .status("ACTIVE")
                .isSystem(false)
                .createdByName(SecurityUtils.getRealName())
                .build();

        templateRepository.save(template);
        log.info("创建报表模板: {}", template.getTemplateNo());
        return toTemplateDTO(template);
    }

    /**
     * 更新报表模板
     */
    @Transactional
    public ReportTemplateDTO updateTemplate(Long id, CreateReportTemplateCommand command) {
        ReportTemplate template = templateRepository.getByIdOrThrow(id, "报表模板不存在");
        
        // 系统内置模板不允许修改
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("系统内置模板不允许修改");
        }

        // 检查模板名称是否重复
        if (templateRepository.existsByTemplateName(command.getTemplateName(), id)) {
            throw new BusinessException("模板名称已存在");
        }

        template.setTemplateName(command.getTemplateName());
        template.setDescription(command.getDescription());
        template.setDataSource(command.getDataSource());
        template.setFieldConfig(toJson(command.getFieldConfig()));
        template.setFilterConfig(toJson(command.getFilterConfig()));
        template.setGroupConfig(toJson(command.getGroupConfig()));
        template.setSortConfig(toJson(command.getSortConfig()));
        template.setAggregateConfig(toJson(command.getAggregateConfig()));

        templateRepository.updateById(template);
        log.info("更新报表模板: {}", template.getTemplateNo());
        return toTemplateDTO(template);
    }

    /**
     * 删除报表模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        ReportTemplate template = templateRepository.getByIdOrThrow(id, "报表模板不存在");
        
        // 系统内置模板不允许删除
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("系统内置模板不允许删除");
        }

        templateRepository.softDelete(id);
        log.info("删除报表模板: {}", template.getTemplateNo());
    }

    /**
     * 启用/停用模板
     */
    @Transactional
    public void changeTemplateStatus(Long id, String status) {
        ReportTemplate template = templateRepository.getByIdOrThrow(id, "报表模板不存在");
        
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("系统内置模板不允许修改状态");
        }

        template.setStatus(status);
        templateRepository.updateById(template);
        log.info("修改报表模板状态: {} -> {}", template.getTemplateNo(), status);
    }

    /**
     * 根据模板生成报表
     */
    @Transactional
    public ReportDTO generateReportByTemplate(Long templateId, Map<String, Object> parameters, String format) {
        ReportTemplate template = templateRepository.getByIdOrThrow(templateId, "报表模板不存在");
        
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("报表模板已停用");
        }

        // 构建报表生成命令
        GenerateReportCommand command = new GenerateReportCommand();
        command.setReportType("CUSTOM_" + template.getDataSource());
        command.setReportName(template.getTemplateName());
        command.setFormat(format != null ? format : "EXCEL");
        
        // 合并参数
        Map<String, Object> mergedParams = new HashMap<>();
        mergedParams.put("templateId", templateId);
        mergedParams.put("fieldConfig", template.getFieldConfig());
        mergedParams.put("filterConfig", template.getFilterConfig());
        mergedParams.put("groupConfig", template.getGroupConfig());
        mergedParams.put("sortConfig", template.getSortConfig());
        mergedParams.put("aggregateConfig", template.getAggregateConfig());
        if (parameters != null) {
            mergedParams.putAll(parameters);
        }
        command.setParameters(mergedParams);

        return reportAppService.generateReport(command);
    }

    /**
     * 获取可用数据源列表
     */
    public List<Map<String, Object>> getDataSources() {
        List<Map<String, Object>> dataSources = new ArrayList<>();
        
        dataSources.add(createDataSource("MATTER", "案件数据", 
                "案件基本信息、状态、金额、律师等"));
        dataSources.add(createDataSource("CLIENT", "客户数据", 
                "客户基本信息、类型、来源、合作情况等"));
        dataSources.add(createDataSource("FINANCE", "财务数据", 
                "收款记录、发票、提成等财务信息"));
        dataSources.add(createDataSource("TIMESHEET", "工时数据", 
                "工时记录、工时统计等"));
        dataSources.add(createDataSource("EMPLOYEE", "员工数据", 
                "员工基本信息、部门、职位等"));
        
        return dataSources;
    }

    /**
     * 获取数据源可用字段
     */
    public List<Map<String, Object>> getDataSourceFields(String dataSource) {
        return switch (dataSource) {
            case "MATTER" -> getMatterFields();
            case "CLIENT" -> getClientFields();
            case "FINANCE" -> getFinanceFields();
            case "TIMESHEET" -> getTimesheetFields();
            case "EMPLOYEE" -> getEmployeeFields();
            default -> Collections.emptyList();
        };
    }

    // ==================== 私有方法 ====================

    private ReportTemplateDTO toTemplateDTO(ReportTemplate template) {
        ReportTemplateDTO dto = new ReportTemplateDTO();
        dto.setId(template.getId());
        dto.setTemplateNo(template.getTemplateNo());
        dto.setTemplateName(template.getTemplateName());
        dto.setDescription(template.getDescription());
        dto.setDataSource(template.getDataSource());
        dto.setDataSourceName(getDataSourceName(template.getDataSource()));
        dto.setStatus(template.getStatus());
        dto.setStatusName("ACTIVE".equals(template.getStatus()) ? "启用" : "停用");
        dto.setIsSystem(template.getIsSystem());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setCreatedByName(template.getCreatedByName());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());

        // 解析JSON配置
        dto.setFieldConfig(parseJsonList(template.getFieldConfig(), ReportTemplateDTO.FieldConfig.class));
        dto.setFilterConfig(parseJsonList(template.getFilterConfig(), ReportTemplateDTO.FilterConfig.class));
        dto.setGroupConfig(parseJsonList(template.getGroupConfig(), ReportTemplateDTO.GroupConfig.class));
        dto.setSortConfig(parseJsonList(template.getSortConfig(), ReportTemplateDTO.SortConfig.class));
        dto.setAggregateConfig(parseJsonList(template.getAggregateConfig(), ReportTemplateDTO.AggregateConfig.class));

        return dto;
    }

    private String generateTemplateNo() {
        return "TPL" + System.currentTimeMillis();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败", e);
            return null;
        }
    }

    private <T> List<T> parseJsonList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败", e);
            return Collections.emptyList();
        }
    }

    private String getDataSourceName(String dataSource) {
        if (dataSource == null) return null;
        return switch (dataSource) {
            case "MATTER" -> "案件数据";
            case "CLIENT" -> "客户数据";
            case "FINANCE" -> "财务数据";
            case "TIMESHEET" -> "工时数据";
            case "EMPLOYEE" -> "员工数据";
            default -> dataSource;
        };
    }

    private Map<String, Object> createDataSource(String code, String name, String description) {
        Map<String, Object> ds = new HashMap<>();
        ds.put("code", code);
        ds.put("name", name);
        ds.put("description", description);
        return ds;
    }

    private List<Map<String, Object>> getMatterFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("matter_no", "案件编号", "string"));
        fields.add(createField("matter_name", "案件名称", "string"));
        fields.add(createField("matter_type", "案件类型", "string"));
        fields.add(createField("status", "状态", "string"));
        fields.add(createField("client_name", "客户名称", "string"));
        fields.add(createField("lawyer_name", "主办律师", "string"));
        fields.add(createField("contract_amount", "合同金额", "decimal"));
        fields.add(createField("received_amount", "已收金额", "decimal"));
        fields.add(createField("created_at", "创建时间", "datetime"));
        fields.add(createField("closed_at", "结案时间", "datetime"));
        return fields;
    }

    private List<Map<String, Object>> getClientFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("client_no", "客户编号", "string"));
        fields.add(createField("name", "客户名称", "string"));
        fields.add(createField("client_type", "客户类型", "string"));
        fields.add(createField("industry", "所属行业", "string"));
        fields.add(createField("source", "客户来源", "string"));
        fields.add(createField("level", "客户级别", "string"));
        fields.add(createField("status", "状态", "string"));
        fields.add(createField("contact_person", "联系人", "string"));
        fields.add(createField("contact_phone", "联系电话", "string"));
        fields.add(createField("first_cooperation_date", "首次合作", "date"));
        return fields;
    }

    private List<Map<String, Object>> getFinanceFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("payment_no", "收款编号", "string"));
        fields.add(createField("matter_name", "案件名称", "string"));
        fields.add(createField("client_name", "客户名称", "string"));
        fields.add(createField("amount", "收款金额", "decimal"));
        fields.add(createField("payment_method", "收款方式", "string"));
        fields.add(createField("payment_date", "收款日期", "date"));
        fields.add(createField("payer_name", "付款人", "string"));
        fields.add(createField("remark", "备注", "string"));
        return fields;
    }

    private List<Map<String, Object>> getTimesheetFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("lawyer_name", "律师姓名", "string"));
        fields.add(createField("matter_name", "案件名称", "string"));
        fields.add(createField("work_date", "工作日期", "date"));
        fields.add(createField("hours", "工时(小时)", "decimal"));
        fields.add(createField("hourly_rate", "小时费率", "decimal"));
        fields.add(createField("amount", "工时金额", "decimal"));
        fields.add(createField("work_content", "工作内容", "string"));
        fields.add(createField("status", "状态", "string"));
        return fields;
    }

    private List<Map<String, Object>> getEmployeeFields() {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("employee_no", "员工编号", "string"));
        fields.add(createField("name", "姓名", "string"));
        fields.add(createField("department_name", "部门", "string"));
        fields.add(createField("position", "职位", "string"));
        fields.add(createField("phone", "联系电话", "string"));
        fields.add(createField("email", "邮箱", "string"));
        fields.add(createField("join_date", "入职日期", "date"));
        fields.add(createField("status", "状态", "string"));
        return fields;
    }

    private Map<String, Object> createField(String field, String label, String type) {
        Map<String, Object> f = new HashMap<>();
        f.put("field", field);
        f.put("label", label);
        f.put("type", type);
        return f;
    }
}
