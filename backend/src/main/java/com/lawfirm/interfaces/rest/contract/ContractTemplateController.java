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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同模板控制器
 */
@Tag(name = "合同模板管理", description = "合同模板的增删改查")
@RestController
@RequestMapping("/system/contract-template")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final ContractTemplateAppService contractTemplateAppService;

    @GetMapping("/active")
    @Operation(summary = "获取启用的模板列表", description = "获取所有启用状态的合同模板")
    public Result<List<ContractTemplateDTO>> getActiveTemplates() {
        return Result.success(contractTemplateAppService.getActiveTemplates());
    }

    @GetMapping("/list")
    @RequirePermission("sys:contract-template:list")
    @Operation(summary = "获取所有模板", description = "获取所有合同模板（包括停用的）")
    public Result<List<ContractTemplateDTO>> getAllTemplates() {
        return Result.success(contractTemplateAppService.getAllTemplates());
    }

    @GetMapping("/type/{contractType}")
    @Operation(summary = "按类型获取模板", description = "根据合同类型获取模板列表")
    public Result<List<ContractTemplateDTO>> getTemplatesByType(@PathVariable String contractType) {
        return Result.success(contractTemplateAppService.getTemplatesByType(contractType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public Result<ContractTemplateDTO> getTemplate(@PathVariable Long id) {
        return Result.success(contractTemplateAppService.getTemplate(id));
    }

    @PostMapping
    @RequirePermission("sys:contract-template:create")
    @OperationLog(module = "合同模板", action = "创建模板")
    @Operation(summary = "创建模板")
    public Result<ContractTemplateDTO> createTemplate(@RequestBody @Valid CreateContractTemplateCommand command) {
        return Result.success(contractTemplateAppService.createTemplate(command));
    }

    @PutMapping("/{id}")
    @RequirePermission("sys:contract-template:update")
    @OperationLog(module = "合同模板", action = "更新模板")
    @Operation(summary = "更新模板")
    public Result<ContractTemplateDTO> updateTemplate(@PathVariable Long id,
                                                       @RequestBody @Valid CreateContractTemplateCommand command) {
        return Result.success(contractTemplateAppService.updateTemplate(id, command));
    }

    @PostMapping("/{id}/toggle")
    @RequirePermission("sys:contract-template:update")
    @OperationLog(module = "合同模板", action = "切换模板状态")
    @Operation(summary = "切换模板状态", description = "启用/停用模板")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        contractTemplateAppService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("sys:contract-template:delete")
    @OperationLog(module = "合同模板", action = "删除模板")
    @Operation(summary = "删除模板")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        contractTemplateAppService.deleteTemplate(id);
        return Result.success();
    }
}
