package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.dto.LoginLogDTO;
import com.lawfirm.application.system.dto.LoginLogQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.domain.system.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录日志应用服务
 */
@Service
@RequiredArgsConstructor
public class LoginLogAppService {

    private final LoginLogRepository loginLogRepository;

    // ✅ 登录失败统计时间窗口可配置（默认1小时）
    @Value("${law-firm.security.login-failure-window-hours:1}")
    private int loginFailureWindowHours;

    // ✅ 最近登录记录查询的最大限制
    private static final int MAX_RECENT_LOGS_LIMIT = 100;

    /**
     * 查询登录日志列表
     */
    public PageResult<LoginLogDTO> listLoginLogs(LoginLogQueryDTO query) {
        // ✅ 数据权限：普通用户只能查看自己的登录日志
        if (!SecurityUtils.hasAnyRole("ADMIN", "admin", "SECURITY_ADMIN", "system")) {
            query.setUserId(SecurityUtils.getUserId());
        }

        Page<LoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getUserId() != null) {
            wrapper.eq(LoginLog::getUserId, query.getUserId());
        }
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.like(LoginLog::getUsername, query.getUsername());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(LoginLog::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(LoginLog::getLoginTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(LoginLog::getLoginTime, query.getEndTime());
        }

        wrapper.orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> result = loginLogRepository.page(page, wrapper);

        List<LoginLogDTO> dtos = result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取登录日志详情
     */
    public LoginLogDTO getLoginLog(Long id) {
        LoginLog log = loginLogRepository.findById(id);
        if (log == null) {
            return null;
        }

        // ✅ 权限验证：普通用户只能查看自己的登录日志
        if (!SecurityUtils.hasAnyRole("ADMIN", "admin", "SECURITY_ADMIN", "system")) {
            if (!log.getUserId().equals(SecurityUtils.getUserId())) {
                throw new BusinessException("权限不足：只能查看自己的登录日志");
            }
        }

        return toDTO(log);
    }

    /**
     * 查询用户最近登录记录
     */
    public List<LoginLogDTO> getRecentLogsByUserId(Long userId, int limit) {
        // ✅ 权限验证：普通用户只能查询自己的登录记录
        if (!SecurityUtils.hasAnyRole("ADMIN", "admin", "SECURITY_ADMIN", "system")) {
            if (!userId.equals(SecurityUtils.getUserId())) {
                throw new BusinessException("权限不足：只能查询自己的登录记录");
            }
        }

        // ✅ 验证limit参数（最小1，最大100）
        int safeLimit = Math.min(Math.max(limit, 1), MAX_RECENT_LOGS_LIMIT);

        List<LoginLog> logs = loginLogRepository.findByUserId(userId, 0, safeLimit);
        return logs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 统计登录失败次数
     */
    public int countFailureByUsername(String username) {
        // ✅ 使用可配置的时间窗口
        LocalDateTime windowStart = LocalDateTime.now().minusHours(loginFailureWindowHours);
        return loginLogRepository.countFailureByUsername(username, windowStart);
    }

    // ========== DTO转换 ==========

    private LoginLogDTO toDTO(LoginLog log) {
        LoginLogDTO dto = new LoginLogDTO();
        BeanUtils.copyProperties(log, dto);
        return dto;
    }
}

