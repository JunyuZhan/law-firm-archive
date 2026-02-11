package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.AssetReceiveCommand;
import com.lawfirm.application.admin.command.CreateAssetCommand;
import com.lawfirm.application.admin.dto.AssetDTO;
import com.lawfirm.application.admin.dto.AssetRecordDTO;
import com.lawfirm.application.admin.service.AssetAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 资产管理接口 */
@Tag(name = "资产管理", description = "固定资产管理相关接口")
@RestController
@RequestMapping("/admin/assets")
@RequiredArgsConstructor
public class AssetController {

  /** 资产应用服务 */
  private final AssetAppService assetAppService;

  /**
   * 分页查询资产列表
   *
   * @param query 分页查询条件
   * @param keyword 关键词
   * @param category 资产类别
   * @param status 资产状态
   * @param departmentId 所属部门ID
   * @return 分页结果
   */
  @Operation(summary = "分页查询资产列表")
  @GetMapping
  @RequirePermission("admin:asset:list")
  public Result<PageResult<AssetDTO>> list(
      final PageQuery query,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "资产类别") @RequestParam(required = false) final String category,
      @Parameter(description = "资产状态") @RequestParam(required = false) final String status,
      @Parameter(description = "所属部门ID") @RequestParam(required = false) final Long departmentId) {
    return Result.success(
        assetAppService.listAssets(query, keyword, category, status, departmentId));
  }

  /**
   * 获取资产详情
   *
   * @param id 资产ID
   * @return 资产详情
   */
  @Operation(summary = "获取资产详情")
  @GetMapping("/{id}")
  @RequirePermission("admin:asset:detail")
  public Result<AssetDTO> getById(@PathVariable final Long id) {
    return Result.success(assetAppService.getAssetById(id));
  }

  /**
   * 创建资产
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建资产")
  @PostMapping
  @RequirePermission("admin:asset:create")
  @OperationLog(module = "资产管理", action = "创建资产")
  public Result<AssetDTO> create(@RequestBody final CreateAssetCommand command) {
    return Result.success(assetAppService.createAsset(command));
  }

  /**
   * 更新资产
   *
   * @param id 资产ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新资产")
  @PutMapping("/{id}")
  @RequirePermission("admin:asset:update")
  @OperationLog(module = "资产管理", action = "更新资产")
  public Result<AssetDTO> update(
      @PathVariable final Long id, @RequestBody final CreateAssetCommand command) {
    return Result.success(assetAppService.updateAsset(id, command));
  }

  /**
   * 删除资产
   *
   * @param id 资产ID
   * @return 无返回
   */
  @Operation(summary = "删除资产")
  @DeleteMapping("/{id}")
  @RequirePermission("admin:asset:delete")
  @OperationLog(module = "资产管理", action = "删除资产")
  public Result<Void> delete(@PathVariable final Long id) {
    assetAppService.deleteAsset(id);
    return Result.success();
  }

  /**
   * 资产领用
   *
   * @param command 领用命令
   * @return 无返回
   */
  @Operation(summary = "资产领用")
  @PostMapping("/receive")
  @RequirePermission("admin:asset:receive")
  @OperationLog(module = "资产管理", action = "资产领用")
  public Result<Void> receive(@RequestBody final AssetReceiveCommand command) {
    assetAppService.receiveAsset(command);
    return Result.success();
  }

  /**
   * 资产归还
   *
   * @param id 资产ID
   * @param remarks 备注
   * @return 无返回
   */
  @Operation(summary = "资产归还")
  @PostMapping("/{id}/return")
  @RequirePermission("admin:asset:return")
  @OperationLog(module = "资产管理", action = "资产归还")
  public Result<Void> returnAsset(
      @PathVariable final Long id,
      @Parameter(description = "备注") @RequestParam(required = false) final String remarks) {
    assetAppService.returnAsset(id, remarks);
    return Result.success();
  }

  /**
   * 资产报废
   *
   * @param id 资产ID
   * @param reason 报废原因
   * @return 无返回
   */
  @Operation(summary = "资产报废")
  @PostMapping("/{id}/scrap")
  @RequirePermission("admin:asset:scrap")
  @OperationLog(module = "资产管理", action = "资产报废")
  public Result<Void> scrap(
      @PathVariable final Long id,
      @Parameter(description = "报废原因") @RequestParam final String reason) {
    assetAppService.scrapAsset(id, reason);
    return Result.success();
  }

  /**
   * 获取资产操作记录
   *
   * @param id 资产ID
   * @return 操作记录列表
   */
  @Operation(summary = "获取资产操作记录")
  @GetMapping("/{id}/records")
  @RequirePermission("admin:asset:detail")
  public Result<List<AssetRecordDTO>> getRecords(@PathVariable final Long id) {
    return Result.success(assetAppService.getAssetRecords(id));
  }

  /**
   * 获取我领用的资产
   *
   * @return 资产列表
   */
  @Operation(summary = "获取我领用的资产")
  @GetMapping("/my")
  public Result<List<AssetDTO>> getMyAssets() {
    return Result.success(assetAppService.getMyAssets());
  }

  /**
   * 获取闲置资产
   *
   * @return 资产列表
   */
  @Operation(summary = "获取闲置资产")
  @GetMapping("/idle")
  @RequirePermission("admin:asset:list")
  public Result<List<AssetDTO>> getIdleAssets() {
    return Result.success(assetAppService.getIdleAssets());
  }

  /**
   * 获取资产统计
   *
   * @return 统计信息
   */
  @Operation(summary = "获取资产统计")
  @GetMapping("/statistics")
  @RequirePermission("admin:asset:list")
  public Result<Map<String, Object>> getStatistics() {
    return Result.success(assetAppService.getAssetStatistics());
  }
}
