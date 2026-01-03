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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAppService {

    private final UserSessionRepository sessionRepository;
    private final UserSessionMapper sessionMapper;
    private final UserRepository userRepository;

    /**
     * 创建会话（登录时调用）
     */
    @Transactional
    public UserSession createSession(Long userId, String username, String token, String refreshToken,
                                     String ipAddress, String userAgent, String deviceType, String browser, String os, String location) {
        // 单点登录：将用户的其他活跃会话标记为已登出
        List<UserSession> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);
        for (UserSession session : activeSessions) {
            session.setStatus("LOGGED_OUT");
            session.setIsCurrent(false);
            sessionRepository.updateById(session);
        }

        // 创建新会话
        UserSession session = UserSession.builder()
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
                .expireTime(LocalDateTime.now().plusHours(24)) // 24小时过期
                .status("ACTIVE")
                .isCurrent(true)
                .build();

        sessionRepository.save(session);
        log.info("创建用户会话成功: userId={}, username={}", userId, username);
        return session;
    }

    /**
     * 分页查询会话列表
     */
    public PageResult<UserSessionDTO> listSessions(UserSessionQueryDTO query) {
        // 数据权限：普通用户只能看自己的会话
        var roles = SecurityUtils.getRoles();
        if (roles == null || (!roles.contains("admin") && !roles.contains("system"))) {
            query.setUserId(SecurityUtils.getUserId());
        }

        IPage<UserSession> page = sessionMapper.selectSessionPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query
        );

        List<UserSessionDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取当前用户的活跃会话
     */
    public List<UserSessionDTO> getMyActiveSessions() {
        Long userId = SecurityUtils.getUserId();
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        return sessions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据Token登出会话
     */
    @Transactional
    public void logoutSessionByToken(String token) {
        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setStatus("LOGGED_OUT");
            session.setIsCurrent(false);
            sessionRepository.updateById(session);
            log.info("登出会话成功: sessionId={}, userId={}", session.getId(), session.getUserId());
        });
    }

    /**
     * 登出会话
     */
    @Transactional
    public void logoutSession(Long sessionId) {
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
     */
    @Transactional
    public void forceLogout(Long sessionId, String reason) {
        UserSession session = sessionRepository.getByIdOrThrow(sessionId, "会话不存在");

        session.setStatus("FORCED_LOGOUT");
        session.setIsCurrent(false);
        sessionRepository.updateById(session);

        log.warn("强制下线会话: sessionId={}, userId={}, reason={}", sessionId, session.getUserId(), reason);
    }

    /**
     * 强制下线用户的所有会话
     */
    @Transactional
    public void forceLogoutUser(Long userId, String reason) {
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        for (UserSession session : sessions) {
            session.setStatus("FORCED_LOGOUT");
            session.setIsCurrent(false);
            sessionRepository.updateById(session);
        }
        log.warn("强制下线用户所有会话: userId={}, count={}, reason={}", userId, sessions.size(), reason);
    }

    /**
     * 根据Token验证会话
     */
    public UserSession validateSession(String token) {
        return sessionRepository.findByToken(token)
                .filter(session -> "ACTIVE".equals(session.getStatus()))
                .filter(session -> session.getExpireTime().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    /**
     * 更新会话最后访问时间
     */
    @Transactional
    public void updateLastAccessTime(String token) {
        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setLastAccessTime(LocalDateTime.now());
            sessionRepository.updateLastAccessTime(session.getId(), LocalDateTime.now());
        });
    }

    /**
     * 定时任务：清理过期会话
     * 每小时执行一次
     */
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
     * UserSession Entity 转 DTO
     */
    private UserSessionDTO toDTO(UserSession session) {
        UserSessionDTO dto = new UserSessionDTO();
        BeanUtils.copyProperties(session, dto);

        // 用户信息
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

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "活跃";
            case "EXPIRED" -> "已过期";
            case "FORCED_LOGOUT" -> "强制下线";
            case "LOGGED_OUT" -> "已登出";
            default -> status;
        };
    }

    private String getDeviceTypeName(String deviceType) {
        if (deviceType == null) return null;
        return switch (deviceType) {
            case "PC" -> "电脑";
            case "MOBILE" -> "手机";
            case "TABLET" -> "平板";
            default -> deviceType;
        };
    }
}

