package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.CreateReportTemplateCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportTemplateDTO;
import com.lawfirm.application.workbench.service.CustomReportAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 自定义报表模板控制器
 */
@Tag(name = "自定义报表模板", description = "报表模板管理接口")
@RestController
@RequestMapping("/workbench/report-template")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final CustomReportAppService customReportAppService;

    @Operation(summary = "分页查询报表模板")
    @GetMapping
    @RequirePermission("report:template:list")
    public Result<PageResult<ReportTemplateDTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "数据源") @RequestParam(required = false) String dataSource,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        PageResult<ReportTemplateDTO> result = customReportAppService.listTemplates(
                pageNum, pageSize, keyword, dataSource, status);
        return Result.success(result);
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    @RequirePermission("report:template:view")
    public Result<ReportTemplateDTO> getById(@PathVariable Long id) {
        ReportTemplateDTO template = customReportAppService.getTemplateById(id);
        return Result.success(template);
    }

    @Operation(summary = "创建报表模板")
    @PostMapping
    @RequirePermission("report:template:create")
    @OperationLog(module = "报表模板", action = "创建模板")
    public Result<ReportTemplateDTO> create(@RequestBody @Valid CreateReportTemplateCommand command) {
        ReportTemplateDTO template = customReportAppService.createTemplate(command);
        return Result.success(template);
    }

    @Operation(summary = "更新报表模板")
    @PutMapping("/{id}")
    @RequirePermission("report:template:edit")
    @OperationLog(module = "报表模板", action = "更新模板")
    public Result<ReportTemplateDTO> update(@PathVariable Long id,
                                             @RequestBody @Valid CreateReportTemplateCommand command) {
        ReportTemplateDTO template = customReportAppService.updateTemplate(id, command);
        return Result.success(template);
    }

    @Operation(summary = "删除报表模板")
    @DeleteMapping("/{id}")
    @RequirePermission("report:template:delete")
    @OperationLog(module = "报表模板", action = "删除模板")
    public Result<Void> delete(@PathVariable Long id) {
        customReportAppService.deleteTemplate(id);
        return Result.success();
    }

    @Operation(summary = "启用模板")
    @PostMapping("/{id}/enable")
    @RequirePermission("report:template:edit")
    @OperationLog(module = "报表模板", action = "启用模板")
    public Result<Void> enable(@PathVariable Long id) {
        customReportAppService.changeTemplateStatus(id, "ACTIVE");
        return Result.success();
    }

    @Operation(summary = "停用模板")
    @PostMapping("/{id}/disable")
    @RequirePermission("report:template:edit")
    @OperationLog(module = "报表模板", action = "停用模板")
    public Result<Void> disable(@PathVariable Long id) {
        customReportAppService.changeTemplateStatus(id, "INACTIVE");
        return Result.success();
    }

    @Operation(summary = "根据模板生成报表")
    @PostMapping("/{id}/generate")
    @RequirePermission("report:generate")
    @OperationLog(module = "报表模板", action = "生成报表")
    public Result<ReportDTO> generate(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, Object> parameters,
                                       @RequestParam(defaultValue = "EXCEL") String format) {
        ReportDTO report = customReportAppService.generateReportByTemplate(id, parameters, format);
        return Result.success(report);
    }

    @Operation(summary = "获取可用数据源列表")
    @GetMapping("/data-sources")
    public Result<List<Map<String, Object>>> getDataSources() {
        List<Map<String, Object>> dataSources = customReportAppService.getDataSources();
        return Result.success(dataSources);
    }

    @Operation(summary = "获取数据源可用字段")
    @GetMapping("/data-sources/{dataSource}/fields")
    public Result<List<Map<String, Object>>> getDataSourceFields(@PathVariable String dataSource) {
        List<Map<String, Object>> fields = customReportAppService.getDataSourceFields(dataSource);
        return Result.success(fields);
    }
}
