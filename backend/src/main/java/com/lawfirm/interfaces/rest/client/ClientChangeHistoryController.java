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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业变更历史接口（M2-014）
 */
@Tag(name = "企业变更历史", description = "企业变更历史记录相关接口")
@RestController
@RequestMapping("/client/change-history")
@RequiredArgsConstructor
public class ClientChangeHistoryController {

    private final ClientChangeHistoryAppService changeHistoryAppService;

    @Operation(summary = "创建变更记录")
    @PostMapping
    @RequirePermission("client:change:create")
    @OperationLog(module = "客户管理", action = "创建企业变更记录")
    public Result<ClientChangeHistoryDTO> createChangeHistory(@RequestBody @Valid CreateClientChangeHistoryCommand command) {
        return Result.success(changeHistoryAppService.createChangeHistory(command));
    }

    @Operation(summary = "获取客户的所有变更记录")
    @GetMapping("/client/{clientId}")
    @RequirePermission("client:change:list")
    public Result<List<ClientChangeHistoryDTO>> getClientChangeHistories(@PathVariable Long clientId) {
        return Result.success(changeHistoryAppService.getClientChangeHistories(clientId));
    }

    @Operation(summary = "获取指定类型的变更记录")
    @GetMapping("/client/{clientId}/type/{changeType}")
    @RequirePermission("client:change:list")
    public Result<List<ClientChangeHistoryDTO>> getChangeHistoriesByType(
            @PathVariable Long clientId,
            @PathVariable String changeType) {
        return Result.success(changeHistoryAppService.getChangeHistoriesByType(clientId, changeType));
    }

    @Operation(summary = "删除变更记录")
    @DeleteMapping("/{id}")
    @RequirePermission("client:change:delete")
    @OperationLog(module = "客户管理", action = "删除企业变更记录")
    public Result<Void> deleteChangeHistory(@PathVariable Long id) {
        changeHistoryAppService.deleteChangeHistory(id);
        return Result.success();
    }
}

