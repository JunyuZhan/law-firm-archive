package com.lawfirm.infrastructure.security;

import com.lawfirm.application.system.service.SysConfigAppService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 回调接口安全过滤器
 * 用于验证客户服务系统的回调请求
 * 
 * 安全策略（二选一）：
 * 1. IP 白名单验证（推荐）：只允许客户服务系统的 IP 访问，适用于固定 IP 场景
 * 2. API Key 验证：使用共享密钥验证，适用于动态 IP 或内网穿透场景
 * 
 * 验证逻辑：
 * - 如果启用 IP 白名单验证，则验证 IP
 * - 如果禁用 IP 白名单但配置了 API Key，则验证请求头中的 X-Callback-Key
 * - 如果两者都禁用/未配置，则拒绝所有请求
 * 
 * 配置优先级：数据库 sys_config 表 > 环境变量 > 配置文件默认值
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackSecurityFilter extends OncePerRequestFilter {

  /** 系统配置服务 */
  private final SysConfigAppService sysConfigAppService;

  /** 客户服务系统 IP 白名单（配置文件默认值） */
  @Value("${client-service.callback.ip-whitelist:}")
  private String defaultIpWhitelist;

  /** 是否启用 IP 白名单验证（配置文件默认值） */
  @Value("${client-service.callback.ip-whitelist-enabled:true}")
  private boolean defaultIpWhitelistEnabled;

  /** 回调 API Key（配置文件默认值，用于替代 IP 白名单验证） */
  @Value("${client-service.callback.api-key:}")
  private String defaultApiKey;

  /**
   * 检查 IP 白名单验证是否启用（优先从数据库读取）
   */
  private boolean isIpWhitelistEnabled() {
    String value = sysConfigAppService.getConfigValue("client-service.callback.ip-whitelist-enabled");
    if (value != null && !value.isEmpty()) {
      return Boolean.parseBoolean(value);
    }
    return defaultIpWhitelistEnabled;
  }

  /**
   * 获取 IP 白名单（优先从数据库读取）
   */
  private String getIpWhitelist() {
    String value = sysConfigAppService.getConfigValue("client-service.callback.ip-whitelist");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultIpWhitelist;
  }

  /**
   * 获取回调 API Key（优先从数据库读取）
   */
  private String getApiKey() {
    String value = sysConfigAppService.getConfigValue("client-service.callback.api-key");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultApiKey;
  }

  /** 回调 API Key 请求头名称 */
  private static final String API_KEY_HEADER = "X-Callback-Key";

  /** 回调接口路径前缀 */
  private static final String CALLBACK_PATH_PREFIX = "/open/client/";

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    String requestPath = request.getRequestURI();
    
    // 只处理回调接口
    if (!requestPath.startsWith(CALLBACK_PATH_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // 获取客户端 IP 和请求头中的 API Key
    String clientIp = getClientIp(request);
    String requestApiKey = request.getHeader(API_KEY_HEADER);
    log.debug("回调请求: path={}, clientIp={}, hasApiKey={}", 
        requestPath, clientIp, requestApiKey != null && !requestApiKey.isEmpty());

    // 安全验证（IP 白名单 或 API Key，二选一）
    boolean ipWhitelistEnabled = isIpWhitelistEnabled();
    String configuredApiKey = getApiKey();
    boolean apiKeyConfigured = configuredApiKey != null && !configuredApiKey.isEmpty();

    if (ipWhitelistEnabled) {
      // 方式1：IP 白名单验证
      if (!isIpAllowed(clientIp)) {
        log.warn("回调请求被拒绝：IP 不在白名单中 - path={}, clientIp={}", requestPath, clientIp);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"403\",\"message\":\"IP 地址不在白名单中\"}");
        return;
      }
    } else if (apiKeyConfigured) {
      // 方式2：API Key 验证（当 IP 白名单禁用时）
      if (requestApiKey == null || requestApiKey.isEmpty()) {
        log.warn("回调请求被拒绝：缺少 API Key - path={}, clientIp={}", requestPath, clientIp);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401\",\"message\":\"缺少回调认证密钥\"}");
        return;
      }
      if (!java.util.Objects.equals(configuredApiKey, requestApiKey)) {
        log.warn("回调请求被拒绝：API Key 不匹配 - path={}, clientIp={}", requestPath, clientIp);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401\",\"message\":\"回调认证密钥无效\"}");
        return;
      }
      log.debug("API Key 验证通过: path={}", requestPath);
    } else {
      // 既没有启用 IP 白名单，也没有配置 API Key，拒绝请求
      log.warn("回调请求被拒绝：未配置任何安全验证方式 - path={}, clientIp={}", requestPath, clientIp);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":\"403\",\"message\":\"回调安全验证未配置，请配置 IP 白名单或 API Key\"}");
      return;
    }

    // 继续处理请求
    filterChain.doFilter(request, response);
  }

  /**
   * 检查 IP 是否在白名单中
   *
   * @param clientIp 客户端 IP
   * @return 是否允许
   */
  private boolean isIpAllowed(final String clientIp) {
    if (clientIp == null || clientIp.isEmpty()) {
      return false;
    }

    // 获取 IP 白名单（优先从数据库读取）
    String whitelist = getIpWhitelist();

    // 如果没有配置白名单，拒绝所有请求（安全默认值）
    if (whitelist == null || whitelist.trim().isEmpty()) {
      log.warn("回调 IP 白名单未配置，拒绝所有回调请求");
      return false;
    }

    // 解析白名单（支持多个 IP，用逗号分隔）
    List<String> allowedIps = Arrays.asList(whitelist.split(","));
    
    // 检查 IP 是否在白名单中（支持精确匹配和 CIDR 格式）
    for (String allowedIp : allowedIps) {
      String trimmedIp = allowedIp.trim();
      if (trimmedIp.isEmpty()) {
        continue;
      }

      // 精确匹配
      if (clientIp.equals(trimmedIp)) {
        return true;
      }

      // CIDR 格式匹配（如：192.168.1.0/24）
      if (trimmedIp.contains("/")) {
        if (isIpInCidr(clientIp, trimmedIp)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 检查 IP 是否在 CIDR 网段中
   *
   * @param ip IP 地址
   * @param cidr CIDR 格式（如：192.168.1.0/24）
   * @return 是否在网段中
   */
  private boolean isIpInCidr(final String ip, final String cidr) {
    try {
      String[] parts = cidr.split("/");
      if (parts.length != 2) {
        return false;
      }

      String networkIp = parts[0].trim();
      int prefixLength = Integer.parseInt(parts[1].trim());

      // 简化实现：只支持 IPv4
      long ipLong = ipToLong(ip);
      long networkLong = ipToLong(networkIp);
      long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;

      return (ipLong & mask) == (networkLong & mask);
    } catch (Exception e) {
      log.warn("CIDR 格式解析失败: cidr={}, error={}", cidr, e.getMessage());
      return false;
    }
  }

  /**
   * 将 IP 地址转换为长整型
   *
   * @param ip IP 地址
   * @return 长整型值
   */
  private long ipToLong(final String ip) {
    String[] parts = ip.split("\\.");
    if (parts.length != 4) {
      throw new IllegalArgumentException("Invalid IP address: " + ip);
    }
    long result = 0;
    for (int i = 0; i < 4; i++) {
      result = (result << 8) + Integer.parseInt(parts[i]);
    }
    return result;
  }

  /**
   * 获取客户端真实 IP 地址
   *
   * @param request HTTP 请求
   * @return IP 地址
   */
  private String getClientIp(final HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    // 处理多个 IP 的情况（X-Forwarded-For 可能包含多个 IP，取第一个）
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }

    return ip;
  }
}
