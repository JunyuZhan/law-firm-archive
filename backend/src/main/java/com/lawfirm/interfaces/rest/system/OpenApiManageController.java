package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.openapi.dto.ClientAccessTokenDTO;
import com.lawfirm.application.openapi.dto.CreateTokenCommand;
import com.lawfirm.application.openapi.service.OpenApiTokenService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenAPI 管理控制器
 * 供内部管理员管理客户访问令牌
 */
@Tag(name = "OpenAPI管理", description = "管理客户门户访问令牌")
@RestController
@RequestMapping("/system/openapi")
@RequiredArgsConstructor
public class OpenApiManageController {

    private final OpenApiTokenService tokenService;

    @Operation(summary = "创建客户访问令牌", description = "为客户创建用于访问客户门户的令牌")
    @PostMapping("/token")
    @RequirePermission("system:openapi:create")
    @OperationLog(module = "系统管理", action = "创建OpenAPI访问令牌")
    public Result<ClientAccessTokenDTO> createToken(@Valid @RequestBody CreateTokenCommand command) {
        return Result.success(tokenService.createToken(command));
    }

    @Operation(summary = "分页查询令牌列表")
    @GetMapping("/token")
    @RequirePermission("system:openapi:list")
    public Result<PageResult<ClientAccessTokenDTO>> listTokens(
            @Parameter(description = "客户ID") @RequestParam(required = false) Long clientId,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long matterId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(tokenService.listTokens(clientId, matterId, status, pageNum, pageSize));
    }

    @Operation(summary = "获取令牌详情")
    @GetMapping("/token/{id}")
    @RequirePermission("system:openapi:list")
    public Result<ClientAccessTokenDTO> getToken(@PathVariable Long id) {
        return Result.success(tokenService.getTokenById(id));
    }

    @Operation(summary = "获取客户的有效令牌列表")
    @GetMapping("/token/client/{clientId}")
    @RequirePermission("system:openapi:list")
    public Result<List<ClientAccessTokenDTO>> getClientTokens(@PathVariable Long clientId) {
        return Result.success(tokenService.getActiveTokensByClientId(clientId));
    }

    @Operation(summary = "撤销令牌")
    @PostMapping("/token/{id}/revoke")
    @RequirePermission("system:openapi:revoke")
    @OperationLog(module = "系统管理", action = "撤销OpenAPI访问令牌")
    public Result<Void> revokeToken(
            @PathVariable Long id,
            @Parameter(description = "撤销原因") @RequestParam(required = false) String reason) {
        tokenService.revokeToken(id, reason);
        return Result.success();
    }

    @Operation(summary = "获取授权范围选项")
    @GetMapping("/scopes")
    @RequirePermission("system:openapi:list")
    public Result<List<ScopeOption>> getScopes() {
        return Result.success(List.of(
                new ScopeOption("MATTER_INFO", "项目基本信息", "项目名称、类型、状态等基本信息"),
                new ScopeOption("MATTER_PROGRESS", "项目进度", "当前阶段、整体进度等进度信息"),
                new ScopeOption("LAWYER_INFO", "律师信息", "项目团队成员姓名、联系方式（脱敏）"),
                new ScopeOption("TASK_LIST", "任务列表", "项目任务标题、状态、进度"),
                new ScopeOption("DEADLINE_INFO", "关键期限", "重要日期和期限信息"),
                new ScopeOption("DOCUMENT_LIST", "文档列表", "文档名称列表（不含内容）"),
                new ScopeOption("FEE_INFO", "费用信息", "合同金额、已收款、待收款")
        ));
    }

    /**
     * 授权范围选项
     */
    public record ScopeOption(String value, String label, String description) {}
}

