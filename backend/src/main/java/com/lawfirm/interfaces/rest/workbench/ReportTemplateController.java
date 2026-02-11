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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 自定义报表模板控制器 */
@Tag(name = "自定义报表模板", description = "报表模板管理接口")
@RestController
@RequestMapping("/workbench/report-template")
@RequiredArgsConstructor
public class ReportTemplateController {

  /** 自定义报表应用服务. */
  private final CustomReportAppService customReportAppService;

  /**
   * 分页查询报表模板
   *
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param keyword 关键词
   * @param dataSource 数据源
   * @param status 状态
   * @return 分页结果
   */
  @Operation(summary = "分页查询报表模板")
  @GetMapping
  @RequirePermission("report:template:list")
  public Result<PageResult<ReportTemplateDTO>> list(
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "数据源") @RequestParam(required = false) final String dataSource,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    PageResult<ReportTemplateDTO> result =
        customReportAppService.listTemplates(pageNum, pageSize, keyword, dataSource, status);
    return Result.success(result);
  }

  /**
   * 获取模板详情
   *
   * @param id 模板ID
   * @return 模板详情
   */
  @Operation(summary = "获取模板详情")
  @GetMapping("/{id}")
  @RequirePermission("report:template:view")
  public Result<ReportTemplateDTO> getById(@PathVariable final Long id) {
    ReportTemplateDTO template = customReportAppService.getTemplateById(id);
    return Result.success(template);
  }

  /**
   * 创建报表模板
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建报表模板")
  @PostMapping
  @RequirePermission("report:template:create")
  @OperationLog(module = "报表模板", action = "创建模板")
  public Result<ReportTemplateDTO> create(
      @RequestBody @Valid final CreateReportTemplateCommand command) {
    ReportTemplateDTO template = customReportAppService.createTemplate(command);
    return Result.success(template);
  }

  /**
   * 更新报表模板
   *
   * @param id 模板ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新报表模板")
  @PutMapping("/{id}")
  @RequirePermission("report:template:update")
  @OperationLog(module = "报表模板", action = "更新模板")
  public Result<ReportTemplateDTO> update(
      @PathVariable final Long id, @RequestBody @Valid final CreateReportTemplateCommand command) {
    ReportTemplateDTO template = customReportAppService.updateTemplate(id, command);
    return Result.success(template);
  }

  /**
   * 删除报表模板
   *
   * @param id 模板ID
   * @return 操作结果
   */
  @Operation(summary = "删除报表模板")
  @DeleteMapping("/{id}")
  @RequirePermission("report:template:delete")
  @OperationLog(module = "报表模板", action = "删除模板")
  public Result<Void> delete(@PathVariable final Long id) {
    customReportAppService.deleteTemplate(id);
    return Result.success();
  }

  /**
   * 启用模板
   *
   * @param id 模板ID
   * @return 操作结果
   */
  @Operation(summary = "启用模板")
  @PostMapping("/{id}/enable")
  @RequirePermission("report:template:update")
  @OperationLog(module = "报表模板", action = "启用模板")
  public Result<Void> enable(@PathVariable final Long id) {
    customReportAppService.changeTemplateStatus(id, "ACTIVE");
    return Result.success();
  }

  /**
   * 停用模板
   *
   * @param id 模板ID
   * @return 操作结果
   */
  @Operation(summary = "停用模板")
  @PostMapping("/{id}/disable")
  @RequirePermission("report:template:update")
  @OperationLog(module = "报表模板", action = "停用模板")
  public Result<Void> disable(@PathVariable final Long id) {
    customReportAppService.changeTemplateStatus(id, "INACTIVE");
    return Result.success();
  }

  /**
   * 根据模板生成报表
   *
   * @param id 模板ID
   * @param parameters 参数
   * @param format 格式
   * @return 生成的报表
   */
  @Operation(summary = "根据模板生成报表")
  @PostMapping("/{id}/generate")
  @RequirePermission("report:generate")
  @OperationLog(module = "报表模板", action = "生成报表")
  public Result<ReportDTO> generate(
      @PathVariable final Long id,
      @RequestBody(required = false) final Map<String, Object> parameters,
      @RequestParam(defaultValue = "EXCEL") final String format) {
    ReportDTO report = customReportAppService.generateReportByTemplate(id, parameters, format);
    return Result.success(report);
  }

  /**
   * 获取可用数据源列表
   *
   * @return 数据源列表
   */
  @Operation(summary = "获取可用数据源列表")
  @GetMapping("/data-sources")
  public Result<List<Map<String, Object>>> getDataSources() {
    List<Map<String, Object>> dataSources = customReportAppService.getDataSources();
    return Result.success(dataSources);
  }

  /**
   * 获取数据源可用字段
   *
   * @param dataSource 数据源名称
   * @return 字段列表
   */
  @Operation(summary = "获取数据源可用字段")
  @GetMapping("/data-sources/{dataSource}/fields")
  public Result<List<Map<String, Object>>> getDataSourceFields(
      @PathVariable final String dataSource) {
    List<Map<String, Object>> fields = customReportAppService.getDataSourceFields(dataSource);
    return Result.success(fields);
  }
}
