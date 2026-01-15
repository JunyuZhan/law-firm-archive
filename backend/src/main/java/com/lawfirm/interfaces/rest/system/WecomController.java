package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.UserWecom;
import com.lawfirm.infrastructure.notification.WecomNotificationChannel;
import com.lawfirm.infrastructure.persistence.mapper.UserWecomMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信配置控制器
 */
@Tag(name = "企业微信配置", description = "企业微信绑定与测试")
@RestController
@RequestMapping("/system/wecom")
@RequiredArgsConstructor
public class WecomController {

    private final UserWecomMapper userWecomMapper;
    
    @Autowired(required = false)
    private WecomNotificationChannel wecomChannel;

    @Operation(summary = "获取当前用户的企业微信绑定信息")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<UserWecom> getMyBinding() {
        Long userId = SecurityUtils.getUserId();
        UserWecom binding = userWecomMapper.selectByUserId(userId);
        return Result.success(binding);
    }

    @Operation(summary = "绑定企业微信")
    @PostMapping("/bind")
    @PreAuthorize("isAuthenticated()")
    public Result<UserWecom> bindWecom(@RequestBody BindWecomCommand command) {
        Long userId = SecurityUtils.getUserId();
        
        UserWecom binding = UserWecom.builder()
                .userId(userId)
                .wecomUserid(command.getWecomUserid())
                .wecomMobile(command.getWecomMobile())
                .enabled(true)
                .build();
        
        userWecomMapper.insertOrUpdate(binding);
        
        return Result.success(userWecomMapper.selectByUserId(userId));
    }

    @Operation(summary = "解绑企业微信")
    @PostMapping("/unbind")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> unbindWecom() {
        Long userId = SecurityUtils.getUserId();
        UserWecom binding = userWecomMapper.selectByUserId(userId);
        
        if (binding != null) {
            binding.setEnabled(false);
            binding.setWecomUserid(null);
            binding.setWecomMobile(null);
            userWecomMapper.insertOrUpdate(binding);
        }
        
        return Result.success();
    }

    @Operation(summary = "测试企业微信机器人连接")
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('system:config:manage')")
    public Result<Map<String, Object>> testWecomBot() {
        Map<String, Object> result = new HashMap<>();
        
        if (wecomChannel == null) {
            result.put("success", false);
            result.put("message", "企业微信通知渠道未初始化");
            return Result.success(result);
        }
        
        boolean success = wecomChannel.testConnection();
        result.put("success", success);
        result.put("message", success ? "连接成功！测试消息已发送到群聊。" : "连接失败，请检查Webhook地址是否正确。");
        
        return Result.success(result);
    }

    @Operation(summary = "获取企业微信推送状态")
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getWecomStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", wecomChannel != null && wecomChannel.isEnabled());
        return Result.success(result);
    }

    @Data
    public static class BindWecomCommand {
        private String wecomUserid;
        private String wecomMobile;
    }
}
