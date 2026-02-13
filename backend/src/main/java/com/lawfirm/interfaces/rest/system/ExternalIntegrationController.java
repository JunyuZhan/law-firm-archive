package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.UpdateExternalIntegrationCommand;
import com.lawfirm.application.system.dto.ExternalIntegrationDTO;
import com.lawfirm.application.system.dto.ExternalIntegrationQueryDTO;
import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 外部系统集成管理控制器 */
@Tag(name = "外部系统集成管理")
@RestController
@RequestMapping("/system/integration")
@RequiredArgsConstructor
public class ExternalIntegrationController {

  /** 外部集成应用服务 */
  private final ExternalIntegrationAppService integrationAppService;

  /**
   * 分页查询集成配置
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询集成配置")
  @GetMapping
  @RequirePermission("sys:integration:list")
  public Result<PageResult<ExternalIntegrationDTO>> list(final ExternalIntegrationQueryDTO query) {
    return Result.success(integrationAppService.listIntegrations(query));
  }

  /**
   * 获取所有集成配置
   *
   * @return 集成配置列表
   */
  @Operation(summary = "获取所有集成配置")
  @GetMapping("/all")
  @RequirePermission("sys:integration:list")
  public Result<List<ExternalIntegrationDTO>> listAll() {
    return Result.success(integrationAppService.listAllIntegrations());
  }

  /**
   * 获取指定类型的启用集成
   *
   * @param type 集成类型：ARCHIVE/AI/OTHER
   * @return 集成配置列表
   */
  @Operation(summary = "获取指定类型的启用集成")
  @GetMapping("/enabled")
  @RequirePermission("sys:integration:list")
  public Result<List<ExternalIntegrationDTO>> listEnabled(
      @Parameter(description = "集成类型：ARCHIVE/AI/OTHER") @RequestParam final String type) {
    return Result.success(integrationAppService.listEnabledByType(type));
  }

  /**
   * 获取集成详情
   *
   * @param id 集成ID
   * @return 集成详情
   */
  @Operation(summary = "获取集成详情")
  @GetMapping("/{id}")
  @RequirePermission("sys:integration:list")
  public Result<ExternalIntegrationDTO> getById(@PathVariable final Long id) {
    return Result.success(integrationAppService.getIntegrationById(id));
  }

  /**
   * 根据编码获取集成详情
   *
   * @param code 集成编码
   * @return 集成详情
   */
  @Operation(summary = "根据编码获取集成详情")
  @GetMapping("/code/{code}")
  @RequirePermission("sys:integration:list")
  public Result<ExternalIntegrationDTO> getByCode(@PathVariable final String code) {
    return Result.success(integrationAppService.getIntegrationByCode(code));
  }

  /**
   * 创建集成配置
   *
   * @param command 创建集成配置命令
   * @return 集成配置信息
   */
  @Operation(summary = "创建集成配置")
  @PostMapping
  @RequirePermission("sys:integration:update")
  @OperationLog(module = "系统管理", action = "创建外部系统集成配置", saveParams = false)
  public Result<ExternalIntegrationDTO> create(
      @RequestBody @Validated final UpdateExternalIntegrationCommand command) {
    return Result.success(integrationAppService.createIntegration(command));
  }

  /**
   * 更新集成配置
   *
   * @param id 集成ID
   * @param command 更新集成配置命令
   * @return 空结果
   */
  @Operation(summary = "更新集成配置")
  @PutMapping("/{id}")
  @RequirePermission("sys:integration:update")
  @OperationLog(module = "系统管理", action = "更新外部系统集成配置", saveParams = false)
  public Result<Void> update(
      @PathVariable final Long id,
      @RequestBody @Validated final UpdateExternalIntegrationCommand command) {
    command.setId(id);
    integrationAppService.updateIntegration(command);
    return Result.success();
  }

  /**
   * 启用集成
   *
   * @param id 集成ID
   * @return 空结果
   */
  @Operation(summary = "启用集成")
  @PostMapping("/{id}/enable")
  @RequirePermission("sys:integration:update")
  @OperationLog(module = "系统管理", action = "启用外部系统集成")
  public Result<Void> enable(@PathVariable final Long id) {
    integrationAppService.enableIntegration(id);
    return Result.success();
  }

  /**
   * 禁用集成
   *
   * @param id 集成ID
   * @return 空结果
   */
  @Operation(summary = "禁用集成")
  @PostMapping("/{id}/disable")
  @RequirePermission("sys:integration:update")
  @OperationLog(module = "系统管理", action = "禁用外部系统集成")
  public Result<Void> disable(@PathVariable final Long id) {
    integrationAppService.disableIntegration(id);
    return Result.success();
  }

  /**
   * 测试连接
   *
   * @param id 集成ID
   * @return 测试结果
   */
  @Operation(summary = "测试连接")
  @PostMapping("/{id}/test")
  @RequirePermission("sys:integration:update")
  @OperationLog(module = "系统管理", action = "测试外部系统连接")
  public Result<ExternalIntegrationDTO> testConnection(@PathVariable final Long id) {
    return Result.success(integrationAppService.testConnection(id));
  }
}
