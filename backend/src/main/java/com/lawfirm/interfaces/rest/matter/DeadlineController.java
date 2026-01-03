package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateDeadlineCommand;
import com.lawfirm.application.matter.command.UpdateDeadlineCommand;
import com.lawfirm.application.matter.dto.DeadlineDTO;
import com.lawfirm.application.matter.dto.DeadlineQueryDTO;
import com.lawfirm.application.matter.service.DeadlineAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 期限提醒 Controller
 */
@Tag(name = "期限提醒管理", description = "期限提醒管理相关接口")
@RestController
@RequestMapping("/matter/deadlines")
@RequiredArgsConstructor
public class DeadlineController {

    private final DeadlineAppService deadlineAppService;

    /**
     * 分页查询期限提醒列表
     */
    @Operation(summary = "分页查询期限提醒列表")
    @GetMapping("/list")
    @RequirePermission("deadline:list")
    public Result<PageResult<DeadlineDTO>> listDeadlines(DeadlineQueryDTO query) {
        PageResult<DeadlineDTO> result = deadlineAppService.listDeadlines(query);
        return Result.success(result);
    }

    /**
     * 获取期限提醒详情
     */
    @Operation(summary = "获取期限提醒详情")
    @GetMapping("/{id}")
    @RequirePermission("deadline:view")
    public Result<DeadlineDTO> getDeadlineById(@PathVariable Long id) {
        DeadlineDTO dto = deadlineAppService.getDeadlineById(id);
        return Result.success(dto);
    }

    /**
     * 根据项目ID查询期限列表
     */
    @Operation(summary = "根据项目ID查询期限列表")
    @GetMapping("/matter/{matterId}")
    @RequirePermission("deadline:view")
    public Result<List<DeadlineDTO>> getDeadlinesByMatterId(@PathVariable Long matterId) {
        List<DeadlineDTO> deadlines = deadlineAppService.getDeadlinesByMatterId(matterId);
        return Result.success(deadlines);
    }

    /**
     * 创建期限提醒
     */
    @Operation(summary = "创建期限提醒")
    @PostMapping
    @RequirePermission("deadline:create")
    @OperationLog(module = "期限提醒管理", action = "创建期限提醒")
    public Result<DeadlineDTO> createDeadline(@RequestBody @Valid CreateDeadlineCommand command) {
        DeadlineDTO dto = deadlineAppService.createDeadline(command);
        return Result.success(dto);
    }

    /**
     * 自动创建期限提醒（根据项目信息）
     */
    @Operation(summary = "自动创建期限提醒")
    @PostMapping("/auto-create/{matterId}")
    @RequirePermission("deadline:create")
    @OperationLog(module = "期限提醒管理", action = "自动创建期限提醒")
    public Result<Void> autoCreateDeadlines(@PathVariable Long matterId) {
        deadlineAppService.autoCreateDeadlines(matterId);
        return Result.success();
    }

    /**
     * 更新期限提醒
     */
    @Operation(summary = "更新期限提醒")
    @PutMapping
    @RequirePermission("deadline:edit")
    @OperationLog(module = "期限提醒管理", action = "更新期限提醒")
    public Result<DeadlineDTO> updateDeadline(@RequestBody @Valid UpdateDeadlineCommand command) {
        DeadlineDTO dto = deadlineAppService.updateDeadline(command);
        return Result.success(dto);
    }

    /**
     * 完成期限
     */
    @Operation(summary = "完成期限")
    @PostMapping("/{id}/complete")
    @RequirePermission("deadline:edit")
    @OperationLog(module = "期限提醒管理", action = "完成期限")
    public Result<DeadlineDTO> completeDeadline(@PathVariable Long id) {
        DeadlineDTO dto = deadlineAppService.completeDeadline(id);
        return Result.success(dto);
    }

    /**
     * 删除期限提醒
     */
    @Operation(summary = "删除期限提醒")
    @DeleteMapping("/{id}")
    @RequirePermission("deadline:delete")
    @OperationLog(module = "期限提醒管理", action = "删除期限提醒")
    public Result<Void> deleteDeadline(@PathVariable Long id) {
        deadlineAppService.deleteDeadline(id);
        return Result.success();
    }
}

