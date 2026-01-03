package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.StartTimerCommand;
import com.lawfirm.application.matter.dto.TimerSessionDTO;
import com.lawfirm.application.matter.service.TimerAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 在线计时器接口（M3-044）
 */
@Tag(name = "在线计时器", description = "在线计时器工具，用于记录工时")
@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
public class TimerController {

    private final TimerAppService timerAppService;

    /**
     * 开始计时
     */
    @PostMapping("/start")
    @RequirePermission("timesheet:record")
    @Operation(summary = "开始计时", description = "开始一个新的计时器会话")
    @OperationLog(module = "工时管理", action = "开始计时")
    public Result<TimerSessionDTO> startTimer(@RequestBody @Valid StartTimerCommand command) {
        TimerSessionDTO session = timerAppService.startTimer(command);
        return Result.success(session);
    }

    /**
     * 暂停计时
     */
    @PostMapping("/pause")
    @RequirePermission("timesheet:record")
    @Operation(summary = "暂停计时", description = "暂停当前正在运行的计时器")
    @OperationLog(module = "工时管理", action = "暂停计时")
    public Result<TimerSessionDTO> pauseTimer() {
        TimerSessionDTO session = timerAppService.pauseTimer();
        return Result.success(session);
    }

    /**
     * 继续计时
     */
    @PostMapping("/resume")
    @RequirePermission("timesheet:record")
    @Operation(summary = "继续计时", description = "继续已暂停的计时器")
    @OperationLog(module = "工时管理", action = "继续计时")
    public Result<TimerSessionDTO> resumeTimer() {
        TimerSessionDTO session = timerAppService.resumeTimer();
        return Result.success(session);
    }

    /**
     * 停止计时并保存工时记录
     */
    @PostMapping("/stop")
    @RequirePermission("timesheet:record")
    @Operation(summary = "停止计时", description = "停止计时器并自动保存为工时记录")
    @OperationLog(module = "工时管理", action = "停止计时")
    public Result<Void> stopTimer() {
        timerAppService.stopTimer();
        return Result.success();
    }

    /**
     * 获取当前计时器状态
     */
    @GetMapping("/status")
    @RequirePermission("timesheet:record")
    @Operation(summary = "获取计时器状态", description = "获取当前用户的计时器状态")
    public Result<TimerSessionDTO> getTimerStatus() {
        TimerSessionDTO status = timerAppService.getTimerStatus();
        return Result.success(status);
    }
}

