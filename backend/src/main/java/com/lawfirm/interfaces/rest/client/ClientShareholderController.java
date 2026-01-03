package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateShareholderCommand;
import com.lawfirm.application.client.command.UpdateShareholderCommand;
import com.lawfirm.application.client.dto.ClientShareholderDTO;
import com.lawfirm.application.client.service.ClientShareholderAppService;
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
 * 客户股东信息 Controller
 */
@Tag(name = "客户股东信息", description = "客户股东信息管理相关接口")
@RestController
@RequestMapping("/client/shareholder")
@RequiredArgsConstructor
public class ClientShareholderController {

    private final ClientShareholderAppService shareholderAppService;

    @Operation(summary = "获取客户的股东列表")
    @GetMapping("/client/{clientId}")
    @RequirePermission("client:shareholder:list")
    public Result<List<ClientShareholderDTO>> getShareholdersByClientId(@PathVariable Long clientId) {
        return Result.success(shareholderAppService.getShareholdersByClientId(clientId));
    }

    @Operation(summary = "创建股东信息")
    @PostMapping
    @RequirePermission("client:shareholder:create")
    @OperationLog(module = "客户管理", action = "创建股东信息")
    public Result<ClientShareholderDTO> createShareholder(@RequestBody @Valid CreateShareholderCommand command) {
        return Result.success(shareholderAppService.createShareholder(command));
    }

    @Operation(summary = "更新股东信息")
    @PutMapping("/{id}")
    @RequirePermission("client:shareholder:update")
    @OperationLog(module = "客户管理", action = "更新股东信息")
    public Result<ClientShareholderDTO> updateShareholder(@PathVariable Long id, 
                                                          @RequestBody @Valid UpdateShareholderCommand command) {
        command.setId(id);
        return Result.success(shareholderAppService.updateShareholder(command));
    }

    @Operation(summary = "删除股东信息")
    @DeleteMapping("/{id}")
    @RequirePermission("client:shareholder:delete")
    @OperationLog(module = "客户管理", action = "删除股东信息")
    public Result<Void> deleteShareholder(@PathVariable Long id) {
        shareholderAppService.deleteShareholder(id);
        return Result.success();
    }
}

