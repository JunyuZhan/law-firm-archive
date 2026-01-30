package com.lawfirm.application.system.service;

import com.lawfirm.common.util.DeviceFingerprintUtils;
import com.lawfirm.common.util.DeviceFingerprintUtils.DeviceInfo;
import com.lawfirm.common.util.IpUtils;
import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.LoginLogRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录日志服务. */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginLogService {

  /** LoginLog Repository. */
  private final LoginLogRepository loginLogRepository;

  /** User Repository. */
  private final UserRepository userRepository;

  /**
   * 记录登录成功日志
   *
   * @param userId 用户ID
   * @param username 用户名
   * @param ip IP地址
   * @param userAgent User-Agent
   */
  @Transactional(rollbackFor = Exception.class)
  public void recordLoginSuccess(
      final Long userId, final String username, final String ip, final String userAgent) {
    User user = userRepository.findById(userId);
    String realName = user != null ? user.getRealName() : null;

    // ✅ 使用 DeviceFingerprintUtils 解析设备信息
    DeviceInfo deviceInfo = DeviceFingerprintUtils.parseDeviceInfo(userAgent);

    LoginLog loginLog =
        LoginLog.builder()
            .userId(userId)
            .username(username)
            .realName(realName)
            .loginIp(ip)
            .loginLocation(IpUtils.getIpDescription(ip)) // ✅ 使用 IpUtils 获取IP描述
            .userAgent(userAgent)
            .browser(deviceInfo.getFullBrowser()) // ✅ 使用 DeviceFingerprintUtils
            .os(deviceInfo.getFullOS()) // ✅ 使用 DeviceFingerprintUtils
            .deviceType(deviceInfo.getDeviceType().name()) // ✅ 使用 DeviceFingerprintUtils
            .status("SUCCESS")
            .message("登录成功")
            .loginTime(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();

    loginLogRepository.getBaseMapper().insert(loginLog);
    log.debug(
        "记录登录成功日志: userId={}, username={}, ip={}, device={}",
        userId,
        username,
        ip,
        deviceInfo.getDeviceType().getDescription());
  }

  /**
   * 记录登录失败日志
   *
   * @param username 用户名
   * @param ip IP地址
   * @param userAgent User-Agent
   * @param message 错误消息
   */
  @Transactional(rollbackFor = Exception.class)
  public void recordLoginFailure(
      final String username, final String ip, final String userAgent, final String message) {
    // ✅ 使用 DeviceFingerprintUtils 解析设备信息
    DeviceInfo deviceInfo = DeviceFingerprintUtils.parseDeviceInfo(userAgent);

    LoginLog loginLog =
        LoginLog.builder()
            .username(username)
            .loginIp(ip)
            .loginLocation(IpUtils.getIpDescription(ip)) // ✅ 使用 IpUtils 获取IP描述
            .userAgent(userAgent)
            .browser(deviceInfo.getFullBrowser()) // ✅ 使用 DeviceFingerprintUtils
            .os(deviceInfo.getFullOS()) // ✅ 使用 DeviceFingerprintUtils
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
   *
   * @param userId 用户ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void recordLogout(final Long userId) {
    // 更新最近一条登录日志的登出时间
    // 这里简化处理，实际可以查询最近一条登录记录
    log.debug("记录登出日志: userId={}", userId);
  }

  /**
   * 检查是否需要锁定账户（连续失败次数）
   *
   * @param username 用户名
   * @return 是否需要锁定
   */
  public boolean shouldLockAccount(final String username) {
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    int failureCount = loginLogRepository.countFailureByUsername(username, oneHourAgo);
    // 1小时内失败5次，建议锁定账户
    return failureCount >= 5;
  }

  /**
   * 获取最近的登录失败次数（用于告警判断）
   *
   * @param username 用户名
   * @return 失败次数
   */
  public int getRecentFailureCount(final String username) {
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    return loginLogRepository.countFailureByUsername(username, oneHourAgo);
  }

  // 工具方法已迁移到 DeviceFingerprintUtils 和 IpUtils
}
