package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.LoginLogDTO;
import com.lawfirm.application.system.dto.LoginLogQueryDTO;
import com.lawfirm.application.system.service.LoginLogAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志控制器
 */
@RestController
@RequestMapping("/api/v1/login-logs")
@RequiredArgsConstructor
@Tag(name = "登录日志", description = "登录日志查询和管理")
public class LoginLogController {

    private final LoginLogAppService loginLogAppService;

    @GetMapping
    @RequirePermission("system:loginlog:list")
    @Operation(summary = "查询登录日志列表")
    public Result<PageResult<LoginLogDTO>> listLoginLogs(LoginLogQueryDTO query) {
        PageResult<LoginLogDTO> result = loginLogAppService.listLoginLogs(query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequirePermission("system:loginlog:view")
    @Operation(summary = "获取登录日志详情")
    public Result<LoginLogDTO> getLoginLog(@PathVariable Long id) {
        LoginLogDTO log = loginLogAppService.getLoginLog(id);
        if (log == null) {
            return Result.notFound("登录日志不存在");
        }
        return Result.success(log);
    }

    @GetMapping("/users/{userId}/recent")
    @RequirePermission("system:loginlog:view")
    @Operation(summary = "查询用户最近登录记录")
    public Result<List<LoginLogDTO>> getRecentLogs(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "10") int limit) {
        List<LoginLogDTO> logs = loginLogAppService.getRecentLogsByUserId(userId, limit);
        return Result.success(logs);
    }

    @GetMapping("/failure-count")
    @RequirePermission("system:loginlog:view")
    @Operation(summary = "统计登录失败次数")
    public Result<Integer> countFailure(@RequestParam String username) {
        int count = loginLogAppService.countFailureByUsername(username);
        return Result.success(count);
    }
}

