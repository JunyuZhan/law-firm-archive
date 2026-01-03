package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateQualityIssueCommand;
import com.lawfirm.application.knowledge.dto.QualityIssueDTO;
import com.lawfirm.application.knowledge.service.QualityIssueAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问题整改接口（M10-032）
 */
@Tag(name = "问题整改", description = "问题整改管理相关接口")
@RestController
@RequestMapping("/knowledge/quality-issue")
@RequiredArgsConstructor
public class QualityIssueController {

    private final QualityIssueAppService issueAppService;

    @Operation(summary = "创建问题")
    @PostMapping
    @RequirePermission("knowledge:quality:create")
    @OperationLog(module = "质量管理", action = "创建问题")
    public Result<QualityIssueDTO> createIssue(@RequestBody CreateQualityIssueCommand command) {
        return Result.success(issueAppService.createIssue(command));
    }

    @Operation(summary = "更新问题状态")
    @PutMapping("/{id}/status")
    @RequirePermission("knowledge:quality:edit")
    @OperationLog(module = "质量管理", action = "更新问题状态")
    public Result<QualityIssueDTO> updateIssueStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String resolution) {
        return Result.success(issueAppService.updateIssueStatus(id, status, resolution));
    }

    @Operation(summary = "获取问题详情")
    @GetMapping("/{id}")
    @RequirePermission("knowledge:quality:detail")
    public Result<QualityIssueDTO> getIssueById(@PathVariable Long id) {
        return Result.success(issueAppService.getIssueById(id));
    }

    @Operation(summary = "获取项目的所有问题")
    @GetMapping("/matter/{matterId}")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityIssueDTO>> getIssuesByMatterId(@PathVariable Long matterId) {
        return Result.success(issueAppService.getIssuesByMatterId(matterId));
    }

    @Operation(summary = "获取待整改的问题")
    @GetMapping("/pending")
    @RequirePermission("knowledge:quality:list")
    public Result<List<QualityIssueDTO>> getPendingIssues() {
        return Result.success(issueAppService.getPendingIssues());
    }
}

