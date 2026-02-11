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

/** 供应商管理接口 */
@Tag(name = "供应商管理", description = "供应商信息管理相关接口")
@RestController
@RequestMapping("/admin/suppliers")
@RequiredArgsConstructor
public class SupplierController {

  /** 供应商应用服务 */
  private final SupplierAppService supplierAppService;

  /**
   * 分页查询供应商列表
   *
   * @param query 查询条件
   * @param keyword 关键词
   * @param supplierType 供应商类型
   * @param status 状态
   * @param rating 评级
   * @return 分页结果
   */
  @Operation(summary = "分页查询供应商列表")
  @GetMapping
  @RequirePermission("admin:supplier:list")
  public Result<PageResult<SupplierDTO>> list(
      final PageQuery query,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "供应商类型") @RequestParam(required = false) final String supplierType,
      @Parameter(description = "状态") @RequestParam(required = false) final String status,
      @Parameter(description = "评级") @RequestParam(required = false) final String rating) {
    return Result.success(
        supplierAppService.listSuppliers(query, keyword, supplierType, status, rating));
  }

  /**
   * 获取供应商详情
   *
   * @param id 供应商ID
   * @return 供应商详情
   */
  @Operation(summary = "获取供应商详情")
  @GetMapping("/{id}")
  @RequirePermission("admin:supplier:detail")
  public Result<SupplierDTO> getById(@PathVariable final Long id) {
    return Result.success(supplierAppService.getSupplierById(id));
  }

  /**
   * 创建供应商
   *
   * @param command 创建命令
   * @return 供应商详情
   */
  @Operation(summary = "创建供应商")
  @PostMapping
  @RequirePermission("admin:supplier:create")
  @OperationLog(module = "供应商管理", action = "创建供应商")
  public Result<SupplierDTO> create(@RequestBody final CreateSupplierCommand command) {
    return Result.success(supplierAppService.createSupplier(command));
  }

  /**
   * 更新供应商
   *
   * @param id 供应商ID
   * @param command 更新命令
   * @return 供应商详情
   */
  @Operation(summary = "更新供应商")
  @PutMapping("/{id}")
  @RequirePermission("admin:supplier:update")
  @OperationLog(module = "供应商管理", action = "更新供应商")
  public Result<SupplierDTO> update(
      @PathVariable final Long id, @RequestBody final CreateSupplierCommand command) {
    return Result.success(supplierAppService.updateSupplier(id, command));
  }

  /**
   * 删除供应商
   *
   * @param id 供应商ID
   * @return 无返回
   */
  @Operation(summary = "删除供应商")
  @DeleteMapping("/{id}")
  @RequirePermission("admin:supplier:delete")
  @OperationLog(module = "供应商管理", action = "删除供应商")
  public Result<Void> delete(@PathVariable final Long id) {
    supplierAppService.deleteSupplier(id);
    return Result.success();
  }

  /**
   * 启用/停用供应商
   *
   * @param id 供应商ID
   * @param status 状态
   * @return 无返回
   */
  @Operation(summary = "启用/停用供应商")
  @PutMapping("/{id}/status")
  @RequirePermission("admin:supplier:update")
  @OperationLog(module = "供应商管理", action = "变更供应商状态")
  public Result<Void> changeStatus(
      @PathVariable final Long id,
      @Parameter(description = "状态") @RequestParam final String status) {
    supplierAppService.changeStatus(id, status);
    return Result.success();
  }

  /**
   * 获取供应商统计
   *
   * @return 统计信息
   */
  @Operation(summary = "获取供应商统计")
  @GetMapping("/statistics")
  @RequirePermission("admin:supplier:list")
  public Result<Map<String, Object>> getStatistics() {
    return Result.success(supplierAppService.getStatistics());
  }
}
