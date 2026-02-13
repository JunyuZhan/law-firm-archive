package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateClientChangeHistoryCommand;
import com.lawfirm.application.client.dto.ClientChangeHistoryDTO;
import com.lawfirm.application.client.service.ClientChangeHistoryAppService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 企业变更历史接口（M2-014） */
@Tag(name = "企业变更历史", description = "企业变更历史记录相关接口")
@RestController
@RequestMapping("/client/change-history")
@RequiredArgsConstructor
public class ClientChangeHistoryController {

  /** 客户变更历史服务. */
  private final ClientChangeHistoryAppService changeHistoryAppService;

  /**
   * 创建变更记录
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建变更记录")
  @PostMapping
  @RequirePermission("client:change:create")
  @OperationLog(module = "客户管理", action = "创建企业变更记录")
  public Result<ClientChangeHistoryDTO> createChangeHistory(
      @RequestBody @Valid final CreateClientChangeHistoryCommand command) {
    return Result.success(changeHistoryAppService.createChangeHistory(command));
  }

  /**
   * 获取客户的所有变更记录
   *
   * @param clientId 客户ID
   * @return 变更记录列表
   */
  @Operation(summary = "获取客户的所有变更记录")
  @GetMapping("/client/{clientId}")
  @RequirePermission("client:change:list")
  public Result<List<ClientChangeHistoryDTO>> getClientChangeHistories(
      @PathVariable final Long clientId) {
    return Result.success(changeHistoryAppService.getClientChangeHistories(clientId));
  }

  /**
   * 获取指定类型的变更记录
   *
   * @param clientId 客户ID
   * @param changeType 变更类型
   * @return 变更记录列表
   */
  @Operation(summary = "获取指定类型的变更记录")
  @GetMapping("/client/{clientId}/type/{changeType}")
  @RequirePermission("client:change:list")
  public Result<List<ClientChangeHistoryDTO>> getChangeHistoriesByType(
      @PathVariable final Long clientId, @PathVariable final String changeType) {
    return Result.success(changeHistoryAppService.getChangeHistoriesByType(clientId, changeType));
  }

  /**
   * 删除变更记录
   *
   * @param id 变更记录ID
   * @return 无返回
   */
  @Operation(summary = "删除变更记录")
  @DeleteMapping("/{id}")
  @RequirePermission("client:change:delete")
  @OperationLog(module = "客户管理", action = "删除企业变更记录")
  public Result<Void> deleteChangeHistory(@PathVariable final Long id) {
    changeHistoryAppService.deleteChangeHistory(id);
    return Result.success();
  }
}
