package com.lawfirm.application.system.service;

import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.LoginLogRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.common.util.DeviceFingerprintUtils;
import com.lawfirm.common.util.DeviceFingerprintUtils.DeviceInfo;
import com.lawfirm.common.util.IpUtils;
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

        // ✅ 使用 DeviceFingerprintUtils 解析设备信息
        DeviceInfo deviceInfo = DeviceFingerprintUtils.parseDeviceInfo(userAgent);

        LoginLog loginLog = LoginLog.builder()
                .userId(userId)
                .username(username)
                .realName(realName)
                .loginIp(ip)
                .loginLocation(IpUtils.getIpDescription(ip)) // ✅ 使用 IpUtils 获取IP描述
                .userAgent(userAgent)
                .browser(deviceInfo.getFullBrowser())          // ✅ 使用 DeviceFingerprintUtils
                .os(deviceInfo.getFullOS())                    // ✅ 使用 DeviceFingerprintUtils
                .deviceType(deviceInfo.getDeviceType().name()) // ✅ 使用 DeviceFingerprintUtils
                .status("SUCCESS")
                .message("登录成功")
                .loginTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        loginLogRepository.getBaseMapper().insert(loginLog);
        log.debug("记录登录成功日志: userId={}, username={}, ip={}, device={}", 
            userId, username, ip, deviceInfo.getDeviceType().getDescription());
    }

    /**
     * 记录登录失败日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginFailure(String username, String ip, String userAgent, String message) {
        // ✅ 使用 DeviceFingerprintUtils 解析设备信息
        DeviceInfo deviceInfo = DeviceFingerprintUtils.parseDeviceInfo(userAgent);
        
        LoginLog loginLog = LoginLog.builder()
                .username(username)
                .loginIp(ip)
                .loginLocation(IpUtils.getIpDescription(ip)) // ✅ 使用 IpUtils 获取IP描述
                .userAgent(userAgent)
                .browser(deviceInfo.getFullBrowser())          // ✅ 使用 DeviceFingerprintUtils
                .os(deviceInfo.getFullOS())                    // ✅ 使用 DeviceFingerprintUtils
                .deviceType(deviceInfo.getDeviceType().name()) // ✅ 使用 DeviceFingerprintUtils
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

    /**
     * 获取最近的登录失败次数（用于告警判断）
     */
    public int getRecentFailureCount(String username) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return loginLogRepository.countFailureByUsername(username, oneHourAgo);
    }

    // 工具方法已迁移到 DeviceFingerprintUtils 和 IpUtils
}

