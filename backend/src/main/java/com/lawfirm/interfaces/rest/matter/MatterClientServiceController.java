package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.openapi.dto.PushConfigDTO;
import com.lawfirm.application.openapi.dto.PushRecordDTO;
import com.lawfirm.application.openapi.dto.PushRequest;
import com.lawfirm.application.openapi.service.DataPushService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目客户服务控制器
 * 管理项目数据推送到客户服务系统
 */
@Tag(name = "项目客户服务", description = "项目数据推送到客户服务系统")
@RestController
@RequestMapping("/matter/client-service")
@RequiredArgsConstructor
public class MatterClientServiceController {

    private final DataPushService dataPushService;

    @Operation(summary = "推送项目数据", description = "将项目数据推送到客户服务系统，客户服务系统会自动通知客户")
    @PostMapping("/push")
    @RequirePermission("matter:clientService:create")
    @OperationLog(module = "项目管理", action = "推送项目数据到客户服务系统")
    public Result<PushRecordDTO> pushData(@Valid @RequestBody PushRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(dataPushService.pushMatterData(request, operatorId));
    }

    @Operation(summary = "获取推送记录", description = "获取项目的数据推送历史记录")
    @GetMapping("/records")
    @RequirePermission("matter:clientService:list")
    public Result<PageResult<PushRecordDTO>> getPushRecords(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(dataPushService.getPushRecords(matterId, null, status, pageNum, pageSize));
    }

    @Operation(summary = "获取推送记录详情")
    @GetMapping("/records/{id}")
    @RequirePermission("matter:clientService:list")
    public Result<PushRecordDTO> getPushRecord(@PathVariable Long id) {
        return Result.success(dataPushService.getPushRecordById(id));
    }

    @Operation(summary = "获取最近一次成功推送")
    @GetMapping("/latest")
    @RequirePermission("matter:clientService:list")
    public Result<PushRecordDTO> getLatestPush(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId) {
        return Result.success(dataPushService.getLatestPush(matterId));
    }

    @Operation(summary = "获取推送配置", description = "获取或创建项目的推送配置")
    @GetMapping("/config")
    @RequirePermission("matter:clientService:list")
    public Result<PushConfigDTO> getConfig(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId,
            @Parameter(description = "客户ID", required = true) @RequestParam Long clientId) {
        return Result.success(dataPushService.getOrCreateConfig(matterId, clientId));
    }

    @Operation(summary = "更新推送配置")
    @PutMapping("/config")
    @RequirePermission("matter:clientService:create")
    public Result<PushConfigDTO> updateConfig(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId,
            @RequestBody PushConfigDTO config) {
        return Result.success(dataPushService.updateConfig(matterId, config));
    }

    @Operation(summary = "获取推送统计", description = "获取项目的推送统计信息")
    @GetMapping("/statistics")
    @RequirePermission("matter:clientService:list")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId) {
        return Result.success(dataPushService.getStatistics(matterId));
    }

    @Operation(summary = "获取可推送的数据范围选项")
    @GetMapping("/scopes")
    @RequirePermission("matter:clientService:list")
    public Result<List<ScopeOption>> getScopes() {
        return Result.success(List.of(
                new ScopeOption("MATTER_INFO", "项目基本信息", "项目名称、编号、类型、状态等"),
                new ScopeOption("MATTER_PROGRESS", "项目进度", "当前阶段、整体进度、最近更新时间"),
                new ScopeOption("LAWYER_INFO", "承办律师", "团队成员姓名、角色、联系方式（脱敏）"),
                new ScopeOption("DEADLINE_INFO", "关键期限", "诉讼时效、举证期限、开庭时间等"),
                new ScopeOption("TASK_LIST", "办理事项", "待办事项标题、状态、进度"),
                new ScopeOption("DOCUMENT_LIST", "文书材料", "文档名称列表（仅标题）"),
                new ScopeOption("FEE_INFO", "费用信息", "合同金额、已收款、待收款")
        ));
    }

    /**
     * 数据范围选项
     */
    public record ScopeOption(String value, String label, String description) {}
}
