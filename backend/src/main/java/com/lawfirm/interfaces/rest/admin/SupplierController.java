package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreateSupplierCommand;
import com.lawfirm.application.admin.dto.SupplierDTO;
import com.lawfirm.application.admin.service.SupplierAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 供应商管理接口
 */
@Tag(name = "供应商管理", description = "供应商信息管理相关接口")
@RestController
@RequestMapping("/admin/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierAppService supplierAppService;

    @Operation(summary = "分页查询供应商列表")
    @GetMapping
    @RequirePermission("admin:supplier:list")
    public Result<PageResult<SupplierDTO>> list(
            PageQuery query,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商类型") @RequestParam(required = false) String supplierType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "评级") @RequestParam(required = false) String rating) {
        return Result.success(supplierAppService.listSuppliers(query, keyword, supplierType, status, rating));
    }

    @Operation(summary = "获取供应商详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:supplier:detail")
    public Result<SupplierDTO> getById(@PathVariable Long id) {
        return Result.success(supplierAppService.getSupplierById(id));
    }

    @Operation(summary = "创建供应商")
    @PostMapping
    @RequirePermission("admin:supplier:create")
    @OperationLog(module = "供应商管理", action = "创建供应商")
    public Result<SupplierDTO> create(@RequestBody CreateSupplierCommand command) {
        return Result.success(supplierAppService.createSupplier(command));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    @RequirePermission("admin:supplier:edit")
    @OperationLog(module = "供应商管理", action = "更新供应商")
    public Result<SupplierDTO> update(@PathVariable Long id, @RequestBody CreateSupplierCommand command) {
        return Result.success(supplierAppService.updateSupplier(id, command));
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:supplier:delete")
    @OperationLog(module = "供应商管理", action = "删除供应商")
    public Result<Void> delete(@PathVariable Long id) {
        supplierAppService.deleteSupplier(id);
        return Result.success();
    }

    @Operation(summary = "启用/停用供应商")
    @PutMapping("/{id}/status")
    @RequirePermission("admin:supplier:edit")
    @OperationLog(module = "供应商管理", action = "变更供应商状态")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        supplierAppService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取供应商统计")
    @GetMapping("/statistics")
    @RequirePermission("admin:supplier:list")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(supplierAppService.getStatistics());
    }
}
