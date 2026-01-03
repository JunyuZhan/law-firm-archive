package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.UserSessionDTO;
import com.lawfirm.application.system.dto.UserSessionQueryDTO;
import com.lawfirm.application.system.service.SessionAppService;
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
 * 会话管理 Controller
 */
@Tag(name = "会话管理", description = "用户会话管理相关接口")
@RestController
@RequestMapping("/system/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionAppService sessionAppService;

    /**
     * 分页查询会话列表
     */
    @Operation(summary = "分页查询会话列表")
    @GetMapping("/list")
    @RequirePermission("session:list")
    public Result<PageResult<UserSessionDTO>> listSessions(UserSessionQueryDTO query) {
        PageResult<UserSessionDTO> result = sessionAppService.listSessions(query);
        return Result.success(result);
    }

    /**
     * 获取当前用户的活跃会话
     */
    @Operation(summary = "获取我的活跃会话")
    @GetMapping("/my-sessions")
    @RequirePermission("session:view")
    public Result<List<UserSessionDTO>> getMyActiveSessions() {
        List<UserSessionDTO> sessions = sessionAppService.getMyActiveSessions();
        return Result.success(sessions);
    }

    /**
     * 登出指定会话
     */
    @Operation(summary = "登出指定会话")
    @PostMapping("/{id}/logout")
    @RequirePermission("session:logout")
    @OperationLog(module = "会话管理", action = "登出会话")
    public Result<Void> logoutSession(@PathVariable Long id) {
        sessionAppService.logoutSession(id);
        return Result.success();
    }

    /**
     * 强制下线（管理员功能）
     */
    @Operation(summary = "强制下线会话")
    @PostMapping("/{id}/force-logout")
    @RequirePermission("session:forceLogout")
    @OperationLog(module = "会话管理", action = "强制下线会话")
    public Result<Void> forceLogout(@PathVariable Long id, @RequestParam(required = false) String reason) {
        sessionAppService.forceLogout(id, reason != null ? reason : "管理员强制下线");
        return Result.success();
    }

    /**
     * 强制下线用户的所有会话
     */
    @Operation(summary = "强制下线用户所有会话")
    @PostMapping("/user/{userId}/force-logout")
    @RequirePermission("session:forceLogout")
    @OperationLog(module = "会话管理", action = "强制下线用户所有会话")
    public Result<Void> forceLogoutUser(@PathVariable Long userId, @RequestParam(required = false) String reason) {
        sessionAppService.forceLogoutUser(userId, reason != null ? reason : "管理员强制下线");
        return Result.success();
    }
}

