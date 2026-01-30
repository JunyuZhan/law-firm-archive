package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreatePrepaymentCommand;
import com.lawfirm.application.finance.command.UsePrepaymentCommand;
import com.lawfirm.application.finance.dto.PrepaymentDTO;
import com.lawfirm.application.finance.dto.PrepaymentQueryDTO;
import com.lawfirm.application.finance.dto.PrepaymentUsageDTO;
import com.lawfirm.application.finance.service.PrepaymentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 预收款管理 Controller */
@RestController
@RequestMapping("/finance/prepayment")
@RequiredArgsConstructor
public class PrepaymentController {

  /** 预收款应用服务. */
  private final PrepaymentAppService prepaymentAppService;

  /**
   * 分页查询预收款
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("prepayment:list")
  public Result<PageResult<PrepaymentDTO>> listPrepayments(final PrepaymentQueryDTO query) {
    PageResult<PrepaymentDTO> result = prepaymentAppService.listPrepayments(query);
    return Result.success(result);
  }

  /**
   * 获取预收款详情
   *
   * @param id 预收款ID
   * @return 预收款详情
   */
  @GetMapping("/{id}")
  @RequirePermission("prepayment:list")
  public Result<PrepaymentDTO> getPrepayment(@PathVariable final Long id) {
    PrepaymentDTO prepayment = prepaymentAppService.getPrepaymentById(id);
    return Result.success(prepayment);
  }

  /**
   * 创建预收款
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("prepayment:create")
  @OperationLog(module = "预收款管理", action = "创建预收款")
  public Result<PrepaymentDTO> createPrepayment(
      @RequestBody @Valid final CreatePrepaymentCommand command) {
    PrepaymentDTO prepayment = prepaymentAppService.createPrepayment(command);
    return Result.success(prepayment);
  }

  /**
   * 确认预收款
   *
   * @param id 预收款ID
   * @return 操作结果
   */
  @PostMapping("/{id}/confirm")
  @RequirePermission("prepayment:confirm")
  @OperationLog(module = "预收款管理", action = "确认预收款")
  public Result<PrepaymentDTO> confirmPrepayment(@PathVariable final Long id) {
    PrepaymentDTO prepayment = prepaymentAppService.confirmPrepayment(id);
    return Result.success(prepayment);
  }

  /**
   * 使用预收款（核销）
   *
   * @param command 使用命令
   * @return 使用结果
   */
  @PostMapping("/use")
  @RequirePermission("prepayment:use")
  @OperationLog(module = "预收款管理", action = "使用预收款")
  public Result<PrepaymentUsageDTO> usePrepayment(
      @RequestBody @Valid final UsePrepaymentCommand command) {
    PrepaymentUsageDTO usage = prepaymentAppService.usePrepayment(command);
    return Result.success(usage);
  }

  /**
   * 查询客户可用预收款
   *
   * @param clientId 客户ID
   * @return 可用预收款列表
   */
  @GetMapping("/available/{clientId}")
  @RequirePermission("prepayment:list")
  public Result<List<PrepaymentDTO>> getAvailablePrepayments(@PathVariable final Long clientId) {
    List<PrepaymentDTO> prepayments = prepaymentAppService.getAvailablePrepayments(clientId);
    return Result.success(prepayments);
  }

  /**
   * 退款
   *
   * @param id 预收款ID
   * @param remark 备注
   * @return 操作结果
   */
  @PostMapping("/{id}/refund")
  @RequirePermission("prepayment:refund")
  @OperationLog(module = "预收款管理", action = "预收款退款")
  public Result<PrepaymentDTO> refundPrepayment(
      @PathVariable final Long id, @RequestParam final String remark) {
    PrepaymentDTO prepayment = prepaymentAppService.refundPrepayment(id, remark);
    return Result.success(prepayment);
  }
}
