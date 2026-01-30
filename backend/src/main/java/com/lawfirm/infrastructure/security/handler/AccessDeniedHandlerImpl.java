package com.lawfirm.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 权限不足处理器.
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 处理权限不足异常.
   *
   * @param request HTTP请求
   * @param response HTTP响应
   * @param accessDeniedException 权限拒绝异常
   * @throws IOException IO异常
   */
  @Override
  public void handle(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AccessDeniedException accessDeniedException)
      throws IOException {
    log.warn("权限不足: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    Result<Void> result = Result.forbidden("权限不足，无法访问");
    response.getWriter().write(objectMapper.writeValueAsString(result));
  }
}
