package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.openapi.dto.ClientFileDTO;
import com.lawfirm.application.openapi.dto.ClientFileSyncRequest;
import com.lawfirm.application.openapi.service.ClientFileService;
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
 * 项目客户文件控制器
 * 管理客户通过客服系统上传的文件
 */
@Tag(name = "项目客户文件", description = "管理客户通过客服系统上传的文件")
@RestController
@RequestMapping("/matter/client-files")
@RequiredArgsConstructor
public class MatterClientFileController {

    private final ClientFileService clientFileService;

    @Operation(summary = "获取客户文件列表", description = "获取项目中客户上传的文件列表")
    @GetMapping
    @RequirePermission("matter:clientService:list")
    public Result<PageResult<ClientFileDTO>> getClientFiles(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId,
            @Parameter(description = "状态: PENDING/SYNCED/DELETED") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(clientFileService.getClientFiles(matterId, status, pageNum, pageSize));
    }

    @Operation(summary = "获取待同步文件列表", description = "获取项目中待同步的客户文件")
    @GetMapping("/pending")
    @RequirePermission("matter:clientService:list")
    public Result<List<ClientFileDTO>> getPendingFiles(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId) {
        return Result.success(clientFileService.getPendingFiles(matterId));
    }

    @Operation(summary = "获取待同步文件数量", description = "统计项目中待同步的客户文件数量")
    @GetMapping("/pending/count")
    @RequirePermission("matter:clientService:list")
    public Result<Map<String, Integer>> countPendingFiles(
            @Parameter(description = "项目ID", required = true) @RequestParam Long matterId) {
        int count = clientFileService.countPendingFiles(matterId);
        return Result.success(Map.of("count", count));
    }

    @Operation(summary = "同步文件到卷宗", description = "将客户上传的文件同步到指定卷宗目录")
    @PostMapping("/sync")
    @RequirePermission("matter:clientService:create")
    @OperationLog(module = "项目管理", action = "同步客户文件到卷宗")
    public Result<ClientFileDTO> syncFile(@Valid @RequestBody ClientFileSyncRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(clientFileService.syncToFolder(request, operatorId));
    }

    @Operation(summary = "批量同步文件", description = "批量将客户上传的文件同步到卷宗")
    @PostMapping("/sync/batch")
    @RequirePermission("matter:clientService:create")
    @OperationLog(module = "项目管理", action = "批量同步客户文件")
    public Result<List<ClientFileDTO>> batchSync(@Valid @RequestBody List<ClientFileSyncRequest> requests) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(clientFileService.batchSync(requests, operatorId));
    }

    @Operation(summary = "忽略文件", description = "忽略客户上传的文件（不同步到卷宗）")
    @PostMapping("/{fileId}/ignore")
    @RequirePermission("matter:clientService:create")
    @OperationLog(module = "项目管理", action = "忽略客户文件")
    public Result<Void> ignoreFile(@PathVariable Long fileId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        clientFileService.ignoreFile(fileId, operatorId);
        return Result.success();
    }
}
