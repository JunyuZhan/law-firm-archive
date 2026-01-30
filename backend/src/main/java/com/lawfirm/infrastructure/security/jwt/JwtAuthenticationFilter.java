package com.lawfirm.infrastructure.security.jwt;

import com.lawfirm.infrastructure.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT认证过滤器.
 *
 * <p>安全增强： - 添加 Token 黑名单检查，防止已登出/已刷新的 Token 被重用
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** JWT令牌提供者. */
  private final JwtTokenProvider jwtTokenProvider;

  /** 用户详情服务. */
  private final UserDetailsService userDetailsService;

  /** 令牌黑名单服务. */
  private final TokenBlacklistService tokenBlacklistService;

  /** 授权请求头名称. */
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /** Bearer令牌前缀. */
  private static final String BEARER_PREFIX = "Bearer ";

  /**
   * 执行JWT认证过滤逻辑.
   *
   * @param request HTTP请求对象
   * @param response HTTP响应对象
   * @param filterChain 过滤器链
   * @throws ServletException Servlet异常
   * @throws IOException IO异常
   */
  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    try {
      // 1. 从请求头获取Token
      String token = extractToken(request);

      // 2. 验证Token（包括黑名单检查）
      if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
        // 2.1 检查 Token 是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(token)) {
          log.warn("Token 已在黑名单中，拒绝访问");
          filterChain.doFilter(request, response);
          return;
        }

        // 3. 从Token获取用户信息
        String username = jwtTokenProvider.getUsernameFromToken(token);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // 3.1 检查用户的 Token 是否被全部失效
        long issuedAt = jwtTokenProvider.getIssuedAtFromToken(token);
        if (tokenBlacklistService.isTokenInvalidatedByUser(userId, issuedAt)) {
          log.warn("用户 {} 的 Token 已被全部失效，拒绝访问", userId);
          filterChain.doFilter(request, response);
          return;
        }

        // 4. 加载用户信息
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(username);

        // 5. 创建认证对象
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 6. 设置到SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("用户认证成功: {}", username);
      }
    } catch (Exception e) {
      log.error("JWT认证失败: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 从请求头提取Token.
   *
   * @param request HTTP请求对象
   * @return JWT Token，如果不存在则返回null
   */
  private String extractToken(final HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
