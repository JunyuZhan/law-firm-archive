package com.lawfirm.application.system.service;

import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.LoginLogRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;

    /**
     * 记录登录成功日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginSuccess(Long userId, String username, String ip, String userAgent) {
        User user = userRepository.findById(userId);
        String realName = user != null ? user.getRealName() : null;

        LoginLog loginLog = LoginLog.builder()
                .userId(userId)
                .username(username)
                .realName(realName)
                .loginIp(ip)
                .loginLocation(parseLocation(ip)) // 可以集成IP地址库
                .userAgent(userAgent)
                .browser(parseBrowser(userAgent))
                .os(parseOS(userAgent))
                .deviceType(parseDeviceType(userAgent))
                .status("SUCCESS")
                .message("登录成功")
                .loginTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        loginLogRepository.getBaseMapper().insert(loginLog);
        log.debug("记录登录成功日志: userId={}, username={}, ip={}", userId, username, ip);
    }

    /**
     * 记录登录失败日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginFailure(String username, String ip, String userAgent, String message) {
        LoginLog loginLog = LoginLog.builder()
                .username(username)
                .loginIp(ip)
                .loginLocation(parseLocation(ip))
                .userAgent(userAgent)
                .browser(parseBrowser(userAgent))
                .os(parseOS(userAgent))
                .deviceType(parseDeviceType(userAgent))
                .status("FAILURE")
                .message(message != null ? message : "登录失败")
                .loginTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        loginLogRepository.getBaseMapper().insert(loginLog);
        log.debug("记录登录失败日志: username={}, ip={}, message={}", username, ip, message);
    }

    /**
     * 记录登出日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLogout(Long userId) {
        // 更新最近一条登录日志的登出时间
        // 这里简化处理，实际可以查询最近一条登录记录
        log.debug("记录登出日志: userId={}", userId);
    }

    /**
     * 检查是否需要锁定账户（连续失败次数）
     */
    public boolean shouldLockAccount(String username) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int failureCount = loginLogRepository.countFailureByUsername(username, oneHourAgo);
        // 1小时内失败5次，建议锁定账户
        return failureCount >= 5;
    }

    // ========== 工具方法 ==========

    /**
     * 解析登录地点（简化实现，实际可集成IP地址库）
     */
    private String parseLocation(String ip) {
        // TODO: 集成IP地址库（如ip2region、GeoIP2等）
        return "未知";
    }

    /**
     * 解析浏览器
     */
    private String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("opera")) {
            return "Opera";
        }
        return "未知";
    }

    /**
     * 解析操作系统
     */
    private String parseOS(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "macOS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("ios") || userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        }
        return "未知";
    }

    /**
     * 解析设备类型
     */
    private String parseDeviceType(String userAgent) {
        if (userAgent == null) {
            return "PC";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile")) {
            return "MOBILE";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "TABLET";
        }
        return "PC";
    }
}

