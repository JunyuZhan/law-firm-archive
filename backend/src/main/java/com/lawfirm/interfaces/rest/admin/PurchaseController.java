package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreatePurchaseRequestCommand;
import com.lawfirm.application.admin.command.PurchaseReceiveCommand;
import com.lawfirm.application.admin.dto.PurchaseReceiveDTO;
import com.lawfirm.application.admin.dto.PurchaseRequestDTO;
import com.lawfirm.application.admin.service.PurchaseAppService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 采购管理接口 */
@Tag(name = "采购管理", description = "采购申请、审批、入库相关接口")
@RestController
@RequestMapping("/admin/purchases")
@RequiredArgsConstructor
public class PurchaseController {

  /** 采购应用服务 */
  private final PurchaseAppService purchaseAppService;

  /**
   * 分页查询采购申请
   *
   * @param query 查询条件
   * @param keyword 关键词
   * @param purchaseType 采购类型
   * @param status 状态
   * @param applicantId 申请人ID
   * @param departmentId 部门ID
   * @return 分页结果
   */
  @Operation(summary = "分页查询采购申请")
  @GetMapping
  @RequirePermission("admin:purchase:list")
  public Result<PageResult<PurchaseRequestDTO>> list(
      final PageQuery query,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "采购类型") @RequestParam(required = false) final String purchaseType,
      @Parameter(description = "状态") @RequestParam(required = false) final String status,
      @Parameter(description = "申请人ID") @RequestParam(required = false) final Long applicantId,
      @Parameter(description = "部门ID") @RequestParam(required = false) final Long departmentId) {
    return Result.success(
        purchaseAppService.listRequests(
            query, keyword, purchaseType, status, applicantId, departmentId));
  }

  /**
   * 获取采购申请详情
   *
   * @param id 申请ID
   * @return 申请详情
   */
  @Operation(summary = "获取采购申请详情")
  @GetMapping("/{id}")
  @RequirePermission("admin:purchase:detail")
  public Result<PurchaseRequestDTO> getById(@PathVariable final Long id) {
    return Result.success(purchaseAppService.getRequestById(id));
  }

  /**
   * 创建采购申请
   *
   * @param command 创建命令
   * @return 申请详情
   */
  @Operation(summary = "创建采购申请")
  @PostMapping
  @RequirePermission("admin:purchase:create")
  @OperationLog(module = "采购管理", action = "创建采购申请")
  public Result<PurchaseRequestDTO> create(
      @RequestBody final CreatePurchaseRequestCommand command) {
    return Result.success(purchaseAppService.createRequest(command));
  }

  /**
   * 提交采购申请
   *
   * @param id 申请ID
   * @return 无返回
   */
  @Operation(summary = "提交采购申请")
  @PostMapping("/{id}/submit")
  @RequirePermission("admin:purchase:create")
  @OperationLog(module = "采购管理", action = "提交采购申请")
  public Result<Void> submit(@PathVariable final Long id) {
    purchaseAppService.submitRequest(id);
    return Result.success();
  }

  /**
   * 审批采购申请
   *
   * @param id 申请ID
   * @param approved 是否批准
   * @param comment 审批意见
   * @return 无返回
   */
  @Operation(summary = "审批采购申请")
  @PostMapping("/{id}/approve")
  @RequirePermission("admin:purchase:approve")
  @OperationLog(module = "采购管理", action = "审批采购申请")
  public Result<Void> approve(
      @PathVariable final Long id,
      @Parameter(description = "是否批准") @RequestParam final boolean approved,
      @Parameter(description = "审批意见") @RequestParam(required = false) final String comment) {
    purchaseAppService.approveRequest(id, approved, comment);
    return Result.success();
  }

  /**
   * 开始采购
   *
   * @param id 申请ID
   * @param supplierId 供应商ID
   * @return 无返回
   */
  @Operation(summary = "开始采购")
  @PostMapping("/{id}/start")
  @RequirePermission("admin:purchase:update")
  @OperationLog(module = "采购管理", action = "开始采购")
  public Result<Void> startPurchasing(
      @PathVariable final Long id,
      @Parameter(description = "供应商ID") @RequestParam final Long supplierId) {
    purchaseAppService.startPurchasing(id, supplierId);
    return Result.success();
  }

  /**
   * 采购入库
   *
   * @param command 入库命令
   * @return 入库记录
   */
  @Operation(summary = "采购入库")
  @PostMapping("/receive")
  @RequirePermission("admin:purchase:receive")
  @OperationLog(module = "采购管理", action = "采购入库")
  public Result<PurchaseReceiveDTO> receive(@RequestBody final PurchaseReceiveCommand command) {
    return Result.success(purchaseAppService.receiveItem(command));
  }

  /**
   * 取消采购申请
   *
   * @param id 申请ID
   * @return 无返回
   */
  @Operation(summary = "取消采购申请")
  @PostMapping("/{id}/cancel")
  @RequirePermission("admin:purchase:update")
  @OperationLog(module = "采购管理", action = "取消采购申请")
  public Result<Void> cancel(@PathVariable final Long id) {
    purchaseAppService.cancelRequest(id);
    return Result.success();
  }

  /**
   * 获取入库记录
   *
   * @param id 申请ID
   * @return 入库记录列表
   */
  @Operation(summary = "获取入库记录")
  @GetMapping("/{id}/receives")
  @RequirePermission("admin:purchase:detail")
  public Result<List<PurchaseReceiveDTO>> getReceiveRecords(@PathVariable final Long id) {
    return Result.success(purchaseAppService.getReceiveRecords(id));
  }

  /**
   * 获取我的采购申请
   *
   * @return 申请列表
   */
  @Operation(summary = "获取我的采购申请")
  @GetMapping("/my")
  public Result<List<PurchaseRequestDTO>> getMyRequests() {
    return Result.success(purchaseAppService.getMyRequests());
  }

  /**
   * 获取待审批的采购申请
   *
   * @return 申请列表
   */
  @Operation(summary = "获取待审批的采购申请")
  @GetMapping("/pending")
  @RequirePermission("admin:purchase:approve")
  public Result<List<PurchaseRequestDTO>> getPendingApproval() {
    return Result.success(purchaseAppService.getPendingApproval());
  }

  /**
   * 获取采购统计
   *
   * @return 统计信息
   */
  @Operation(summary = "获取采购统计")
  @GetMapping("/statistics")
  @RequirePermission("admin:purchase:list")
  public Result<Map<String, Object>> getStatistics() {
    return Result.success(purchaseAppService.getStatistics());
  }
}
