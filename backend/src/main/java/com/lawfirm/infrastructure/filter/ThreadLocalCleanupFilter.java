package com.lawfirm.infrastructure.filter;

import com.lawfirm.application.common.service.ContractDataPermissionService;
import com.lawfirm.application.workbench.service.ReportAppService;
import com.lawfirm.application.workbench.service.StatisticsAppService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal 清理过滤器.
 *
 * <p>确保每个请求结束后清理 ThreadLocal 缓存，防止内存泄漏。
 *
 * <p>安全增强：修复 ThreadLocal 内存泄漏问题。 当使用线程池时，线程会被复用，如果 ThreadLocal 没有被清理， 会导致内存泄漏和数据污染。
 *
 * @author system
 * @since 2026-02-10
 */
@Slf4j
public class ThreadLocalCleanupFilter implements Filter {

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    log.debug("ThreadLocal 清理过滤器初始化");
  }

  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    try {
      // 继续执行过滤器链
      chain.doFilter(request, response);
    } finally {
      // 无论请求成功还是失败，都清理 ThreadLocal
      cleanupThreadLocals();
    }
  }

  /** 清理所有 ThreadLocal 缓存. */
  private void cleanupThreadLocals() {
    try {
      // 清理统计服务的 ThreadLocal 缓存
      StatisticsAppService.clearCache();
    } catch (Exception e) {
      log.warn("清理 StatisticsAppService ThreadLocal 失败", e);
    }

    try {
      // 清理合同数据权限服务的 ThreadLocal 缓存
      ContractDataPermissionService.clearCache();
    } catch (Exception e) {
      log.warn("清理 ContractDataPermissionService ThreadLocal 失败", e);
    }

    try {
      // 清理报表服务的 ThreadLocal 缓存
      ReportAppService.clearCache();
    } catch (Exception e) {
      log.warn("清理 ReportAppService ThreadLocal 失败", e);
    }
  }

  @Override
  public void destroy() {
    log.debug("ThreadLocal 清理过滤器销毁");
  }
}
