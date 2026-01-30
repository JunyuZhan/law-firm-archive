package com.lawfirm.interfaces.rest.contract;

import com.lawfirm.application.contract.command.CreateContractTemplateCommand;
import com.lawfirm.application.contract.dto.ContractTemplateDTO;
import com.lawfirm.application.contract.service.ContractTemplateAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 合同模板控制器 */
@Tag(name = "合同模板管理", description = "合同模板的增删改查")
@RestController
@RequestMapping("/system/contract-template")
@RequiredArgsConstructor
public class ContractTemplateController {

  /** 合同模板应用服务 */
  private final ContractTemplateAppService contractTemplateAppService;

  /**
   * 获取启用的模板列表
   *
   * @return 启用的合同模板列表
   */
  @GetMapping("/active")
  @Operation(summary = "获取启用的模板列表", description = "获取所有启用状态的合同模板")
  public Result<List<ContractTemplateDTO>> getActiveTemplates() {
    return Result.success(contractTemplateAppService.getActiveTemplates());
  }

  /**
   * 获取所有模板
   *
   * @return 所有合同模板列表（包括停用的）
   */
  @GetMapping("/list")
  @RequirePermission("sys:contract-template:list")
  @Operation(summary = "获取所有模板", description = "获取所有合同模板（包括停用的）")
  public Result<List<ContractTemplateDTO>> getAllTemplates() {
    return Result.success(contractTemplateAppService.getAllTemplates());
  }

  /**
   * 按类型获取模板
   *
   * @param contractType 合同类型
   * @return 合同模板列表
   */
  @GetMapping("/type/{contractType}")
  @Operation(summary = "按类型获取模板", description = "根据合同类型获取模板列表")
  public Result<List<ContractTemplateDTO>> getTemplatesByType(
      @PathVariable final String contractType) {
    return Result.success(contractTemplateAppService.getTemplatesByType(contractType));
  }

  /**
   * 获取模板详情
   *
   * @param id 模板ID
   * @return 合同模板详情
   */
  @GetMapping("/{id}")
  @Operation(summary = "获取模板详情")
  public Result<ContractTemplateDTO> getTemplate(@PathVariable final Long id) {
    return Result.success(contractTemplateAppService.getTemplate(id));
  }

  /**
   * 创建模板
   *
   * @param command 创建合同模板命令
   * @return 合同模板信息
   */
  @PostMapping
  @RequirePermission("sys:contract-template:create")
  @OperationLog(module = "合同模板", action = "创建模板")
  @Operation(summary = "创建模板")
  public Result<ContractTemplateDTO> createTemplate(
      @RequestBody @Valid final CreateContractTemplateCommand command) {
    return Result.success(contractTemplateAppService.createTemplate(command));
  }

  /**
   * 更新模板
   *
   * @param id 模板ID
   * @param command 更新合同模板命令
   * @return 合同模板信息
   */
  @PutMapping("/{id}")
  @RequirePermission("sys:contract-template:update")
  @OperationLog(module = "合同模板", action = "更新模板")
  @Operation(summary = "更新模板")
  public Result<ContractTemplateDTO> updateTemplate(
      @PathVariable final Long id,
      @RequestBody @Valid final CreateContractTemplateCommand command) {
    return Result.success(contractTemplateAppService.updateTemplate(id, command));
  }

  /**
   * 切换模板状态
   *
   * @param id 模板ID
   * @return 空结果
   */
  @PostMapping("/{id}/toggle")
  @RequirePermission("sys:contract-template:update")
  @OperationLog(module = "合同模板", action = "切换模板状态")
  @Operation(summary = "切换模板状态", description = "启用/停用模板")
  public Result<Void> toggleStatus(@PathVariable final Long id) {
    contractTemplateAppService.toggleStatus(id);
    return Result.success();
  }

  /**
   * 删除模板
   *
   * @param id 模板ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("sys:contract-template:delete")
  @OperationLog(module = "合同模板", action = "删除模板")
  @Operation(summary = "删除模板")
  public Result<Void> deleteTemplate(@PathVariable final Long id) {
    contractTemplateAppService.deleteTemplate(id);
    return Result.success();
  }
}
