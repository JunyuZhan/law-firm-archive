package com.lawfirm.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 认证失败处理器（未登录）.
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 处理认证失败异常.
   *
   * @param request HTTP请求
   * @param response HTTP响应
   * @param authException 认证异常
   * @throws IOException IO异常
   */
  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException authException)
      throws IOException {
    log.warn("认证失败: {} - {}", request.getRequestURI(), authException.getMessage());

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    Result<Void> result = Result.unauthorized("请先登录");
    response.getWriter().write(objectMapper.writeValueAsString(result));
  }
}
