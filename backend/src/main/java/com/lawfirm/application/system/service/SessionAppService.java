package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.UserSessionDTO;
import com.lawfirm.application.system.dto.UserSessionQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.entity.UserSession;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.system.repository.UserSessionRepository;
import com.lawfirm.infrastructure.persistence.mapper.UserSessionMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会话管理应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAppService {

  /** 用户会话仓储 */
  private final UserSessionRepository sessionRepository;

  /** 用户会话Mapper */
  private final UserSessionMapper sessionMapper;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** ✅ 会话过期时间可配置（默认24小时） */
  @Value("${law-firm.security.session-expire-hours:24}")
  private int sessionExpireHours;

  /**
   * 创建会话（登录时调用）
   *
   * @param userId 用户ID
   * @param username 用户名
   * @param token 访问令牌
   * @param refreshToken 刷新令牌
   * @param ipAddress IP地址
   * @param userAgent User-Agent
   * @param deviceType 设备类型
   * @param browser 浏览器
   * @param os 操作系统
   * @param location 位置
   * @return 用户会话
   */
  @Transactional
  public UserSession createSession(
      final Long userId,
      final String username,
      final String token,
      final String refreshToken,
      final String ipAddress,
      final String userAgent,
      final String deviceType,
      final String browser,
      final String os,
      final String location) {
    // 单点登录：将用户的其他活跃会话标记为已登出
    List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);
    if (!activeSessions.isEmpty()) {
      // ✅ 批量更新替代循环更新
      for (UserSession s : activeSessions) {
        s.setStatus("LOGGED_OUT");
        s.setIsCurrent(false);
      }
      sessionRepository.updateBatchById(activeSessions);
    }

    // 创建新会话
    UserSession session =
        UserSession.builder()
            .userId(userId)
            .username(username)
            .token(token)
            .refreshToken(refreshToken)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .deviceType(deviceType)
            .browser(browser)
            .os(os)
            .location(location)
            .loginTime(LocalDateTime.now())
            .lastAccessTime(LocalDateTime.now())
            .expireTime(LocalDateTime.now().plusHours(sessionExpireHours)) // ✅ 可配置过期时间
            .status("ACTIVE")
            .isCurrent(true)
            .build();

    sessionRepository.save(session);
    log.info("创建用户会话成功: userId={}, username={}", userId, username);
    return session;
  }

  /**
   * 分页查询会话列表
   *
   * @param query 查询参数
   * @return 分页结果
   */
  public PageResult<UserSessionDTO> listSessions(final UserSessionQueryDTO query) {
    // 数据权限：普通用户只能看自己的会话
    var roles = SecurityUtils.getRoles();
    if (roles == null
        || (!roles.contains("admin") && !roles.contains("system") && !roles.contains("ADMIN"))) {
      query.setUserId(SecurityUtils.getUserId());
    }

    IPage<UserSession> page =
        sessionMapper.selectSessionPage(new Page<>(query.getPageNum(), query.getPageSize()), query);

    List<UserSession> sessions = page.getRecords();
    if (sessions.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    // ✅ 批量加载用户信息，避免 N+1 查询
    Set<Long> userIds =
        sessions.stream()
            .map(UserSession::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO(从Map获取用户信息)
    List<UserSessionDTO> records =
        sessions.stream().map(s -> toDTO(s, userMap)).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取当前用户的活跃会话
   *
   * @return 会话列表
   */
  public List<UserSessionDTO> getMyActiveSessions() {
    Long userId = SecurityUtils.getUserId();
    List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);
    return sessions.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 根据Token登出会话
   *
   * @param token 令牌
   */
  @Transactional
  public void logoutSessionByToken(final String token) {
    sessionRepository
        .findByToken(token)
        .ifPresent(
            session -> {
              session.setStatus("LOGGED_OUT");
              session.setIsCurrent(false);
              sessionRepository.updateById(session);
              log.info("登出会话成功: sessionId={}, userId={}", session.getId(), session.getUserId());
            });
  }

  /**
   * 登出会话
   *
   * @param sessionId 会话ID
   */
  @Transactional
  public void logoutSession(final Long sessionId) {
    UserSession session = sessionRepository.getByIdOrThrow(sessionId, "会话不存在");

    // 权限检查：只能登出自己的会话，管理员可以登出任何会话
    var roles = SecurityUtils.getRoles();
    if ((roles == null || (!roles.contains("admin") && !roles.contains("system")))
        && !session.getUserId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("无权操作其他用户的会话");
    }

    session.setStatus("LOGGED_OUT");
    session.setIsCurrent(false);
    sessionRepository.updateById(session);
    log.info("登出会话成功: sessionId={}, userId={}", sessionId, session.getUserId());
  }

  /**
   * 强制下线（管理员功能）
   *
   * @param sessionId 会话ID
   * @param reason 原因
   */
  @Transactional
  public void forceLogout(final Long sessionId, final String reason) {
    // ✅ 权限验证：只有管理员才能强制下线
    if (!SecurityUtils.hasAnyRole("ADMIN", "admin", "SECURITY_ADMIN", "system")) {
      throw new BusinessException("权限不足：只有管理员才能强制下线");
    }

    UserSession session = sessionRepository.getByIdOrThrow(sessionId, "会话不存在");

    session.setStatus("FORCED_LOGOUT");
    session.setIsCurrent(false);
    sessionRepository.updateById(session);

    // ✅ 记录审计日志
    log.warn(
        "强制下线会话: sessionId={}, userId={}, reason={}, operator={}",
        sessionId,
        session.getUserId(),
        reason,
        SecurityUtils.getUserId());
  }

  /**
   * 强制下线用户的所有会话
   *
   * @param userId 用户ID
   * @param reason 原因
   */
  @Transactional
  public void forceLogoutUser(final Long userId, final String reason) {
    // ✅ 权限验证：只有管理员才能强制下线用户
    if (!SecurityUtils.hasAnyRole("ADMIN", "admin", "SECURITY_ADMIN", "system")) {
      throw new BusinessException("权限不足：只有管理员才能强制下线用户");
    }

    List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);
    if (!sessions.isEmpty()) {
      // ✅ 批量更新替代循环更新
      for (UserSession session : sessions) {
        session.setStatus("FORCED_LOGOUT");
        session.setIsCurrent(false);
      }
      sessionRepository.updateBatchById(sessions);
    }
    log.warn(
        "强制下线用户所有会话: userId={}, count={}, reason={}, operator={}",
        userId,
        sessions.size(),
        reason,
        SecurityUtils.getUserId());
  }

  /**
   * 根据Token验证会话
   *
   * @param token 令牌
   * @return 用户会话
   */
  public UserSession validateSession(final String token) {
    return sessionRepository
        .findByToken(token)
        .filter(session -> "ACTIVE".equals(session.getStatus()))
        .filter(session -> session.getExpireTime().isAfter(LocalDateTime.now()))
        .orElse(null);
  }

  /**
   * 更新会话最后访问时间
   *
   * @param token 令牌
   */
  @Transactional
  public void updateLastAccessTime(final String token) {
    // ✅ 优化：直接通过token更新，避免先查询再更新
    sessionRepository.updateLastAccessTimeByToken(token, LocalDateTime.now());
  }

  /** 定时任务：清理过期会话 每小时执行一次 */
  @Scheduled(cron = "0 0 * * * ?")
  @Transactional
  public void cleanupExpiredSessions() {
    log.info("开始清理过期会话");
    int count = sessionRepository.updateExpiredSessions(LocalDateTime.now());
    if (count > 0) {
      log.info("清理过期会话完成，共清理{}条", count);
    }
  }

  /**
   * UserSession Entity转DTO（批量场景使用，从Map获取用户信息）
   *
   * @param session 会话实体
   * @param userMap 用户Map
   * @return 会话DTO
   */
  private UserSessionDTO toDTO(final UserSession session, final Map<Long, User> userMap) {
    UserSessionDTO dto = new UserSessionDTO();
    BeanUtils.copyProperties(session, dto);

    // ✅ 从预加载的Map获取用户信息，避免N+1查询
    if (session.getUserId() != null && userMap != null) {
      User user = userMap.get(session.getUserId());
      if (user != null) {
        dto.setUserRealName(user.getRealName());
      }
    }

    // 状态名称
    dto.setStatusName(getStatusName(session.getStatus()));
    dto.setDeviceTypeName(getDeviceTypeName(session.getDeviceType()));

    return dto;
  }

  /**
   * UserSession Entity转DTO（单条查询场景）
   *
   * @param session 会话实体
   * @return 会话DTO
   */
  private UserSessionDTO toDTO(final UserSession session) {
    UserSessionDTO dto = new UserSessionDTO();
    BeanUtils.copyProperties(session, dto);

    // 用户信息（单条查询场景，允许查询数据库）
    if (session.getUserId() != null) {
      User user = userRepository.findById(session.getUserId());
      if (user != null) {
        dto.setUserRealName(user.getRealName());
      }
    }

    // 状态名称
    dto.setStatusName(getStatusName(session.getStatus()));
    dto.setDeviceTypeName(getDeviceTypeName(session.getDeviceType()));

    return dto;
  }

  /**
   * 获取状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "ACTIVE" -> "活跃";
      case "EXPIRED" -> "已过期";
      case "FORCED_LOGOUT" -> "强制下线";
      case "LOGGED_OUT" -> "已登出";
      default -> status;
    };
  }

  /**
   * 获取设备类型名称
   *
   * @param deviceType 设备类型代码
   * @return 设备类型名称
   */
  private String getDeviceTypeName(final String deviceType) {
    if (deviceType == null) {
      return null;
    }
    return switch (deviceType) {
      case "PC" -> "电脑";
      case "MOBILE" -> "手机";
      case "TABLET" -> "平板";
      default -> deviceType;
    };
  }
}
