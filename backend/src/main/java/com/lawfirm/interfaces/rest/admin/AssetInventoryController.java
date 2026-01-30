package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreateAssetInventoryCommand;
import com.lawfirm.application.admin.dto.AssetInventoryDTO;
import com.lawfirm.application.admin.service.AssetInventoryAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 资产盘点接口（M8-033） */
@Tag(name = "资产盘点", description = "资产盘点管理")
@RestController
@RequestMapping("/admin/asset-inventories")
@RequiredArgsConstructor
public class AssetInventoryController {

  /** 资产盘点应用服务 */
  private final AssetInventoryAppService inventoryAppService;

  /**
   * 分页查询资产盘点列表
   *
   * @param query 分页查询条件
   * @param status 状态
   * @return 分页结果
   */
  @Operation(summary = "分页查询资产盘点列表")
  @GetMapping
  @RequirePermission("admin:asset:inventory")
  public Result<PageResult<AssetInventoryDTO>> list(
      final PageQuery query,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    return Result.success(inventoryAppService.listInventories(query, status));
  }

  /**
   * 创建资产盘点
   *
   * @param command 创建命令
   * @return 盘点记录
   */
  @Operation(summary = "创建资产盘点")
  @PostMapping
  @RequirePermission("admin:asset:inventory")
  @OperationLog(module = "资产盘点", action = "创建资产盘点")
  public Result<AssetInventoryDTO> createInventory(
      @RequestBody @Valid final CreateAssetInventoryCommand command) {
    return Result.success(inventoryAppService.createInventory(command));
  }

  /**
   * 更新盘点明细
   *
   * @param detailId 明细ID
   * @param request 更新请求
   * @return 无返回
   */
  @Operation(summary = "更新盘点明细")
  @PutMapping("/details/{detailId}")
  @RequirePermission("admin:asset:inventory")
  @OperationLog(module = "资产盘点", action = "更新盘点明细")
  public Result<Void> updateDetail(
      @PathVariable final Long detailId, @RequestBody final UpdateDetailRequest request) {
    inventoryAppService.updateInventoryDetail(
        detailId,
        request.getActualStatus(),
        request.getActualLocation(),
        request.getActualUserId(),
        request.getDiscrepancyDesc());
    return Result.success();
  }

  /**
   * 完成盘点
   *
   * @param id 盘点ID
   * @return 盘点记录
   */
  @Operation(summary = "完成盘点")
  @PostMapping("/{id}/complete")
  @RequirePermission("admin:asset:inventory")
  @OperationLog(module = "资产盘点", action = "完成盘点")
  public Result<AssetInventoryDTO> completeInventory(@PathVariable final Long id) {
    return Result.success(inventoryAppService.completeInventory(id));
  }

  /**
   * 获取盘点详情
   *
   * @param id 盘点ID
   * @return 盘点详情
   */
  @Operation(summary = "获取盘点详情")
  @GetMapping("/{id}")
  @RequirePermission("admin:asset:inventory")
  public Result<AssetInventoryDTO> getInventory(@PathVariable final Long id) {
    return Result.success(inventoryAppService.getInventoryById(id));
  }

  /**
   * 查询进行中的盘点
   *
   * @return 盘点列表
   */
  @Operation(summary = "查询进行中的盘点")
  @GetMapping("/in-progress")
  @RequirePermission("admin:asset:inventory")
  public Result<List<AssetInventoryDTO>> getInProgressInventories() {
    return Result.success(inventoryAppService.getInProgressInventories());
  }

  /** 更新明细请求 */
  @Data
  public static class UpdateDetailRequest {
    /** 实际状态 */
    private String actualStatus;

    /** 实际位置 */
    private String actualLocation;

    /** 实际使用人ID */
    private Long actualUserId;

    /** 差异描述 */
    private String discrepancyDesc;
  }
}
