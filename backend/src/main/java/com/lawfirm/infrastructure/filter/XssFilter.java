package com.lawfirm.infrastructure.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * XSS过滤器
 *
 * <p>功能： - 过滤请求参数中的XSS脚本 - 过滤请求头中的XSS脚本 - 可配置排除路径
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public class XssFilter implements Filter {

  /** 排除的路径前缀（不进行XSS过滤）. */
  private static final List<String> EXCLUDE_PATHS =
      Arrays.asList("/health", "/actuator", "/doc.html", "/swagger-ui", "/v3/api-docs", "/webjars");

  /** 需要跳过的Content-Type. */
  private static final List<String> SKIP_CONTENT_TYPES =
      Arrays.asList("application/json", "multipart/form-data");

  /**
   * 初始化过滤器.
   *
   * @param filterConfig 过滤器配置
   * @throws ServletException Servlet异常
   */
  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    log.info("XssFilter initialized");
  }

  /**
   * 执行过滤.
   *
   * @param request 请求
   * @param response 响应
   * @param chain 过滤器链
   * @throws IOException IO异常
   * @throws ServletException Servlet异常
   */
  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getRequestURI();

    // 检查是否在排除路径中
    if (isExcludePath(path)) {
      chain.doFilter(request, response);
      return;
    }

    // 检查Content-Type是否需要跳过
    String contentType = httpRequest.getContentType();
    if (contentType != null && shouldSkipContentType(contentType)) {
      // JSON请求体由Jackson处理，这里只过滤参数和头
      chain.doFilter(new XssHttpServletRequestWrapper(httpRequest, false), response);
      return;
    }

    // 使用XSS包装器
    chain.doFilter(new XssHttpServletRequestWrapper(httpRequest, true), response);
  }

  /** 销毁过滤器. */
  @Override
  public void destroy() {
    log.info("XssFilter destroyed");
  }

  /**
   * 检查是否是排除路径.
   *
   * @param path 请求路径
   * @return 是否排除
   */
  private boolean isExcludePath(final String path) {
    if (path == null) {
      return false;
    }
    return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
  }

  /**
   * 检查是否需要跳过的Content-Type.
   *
   * @param contentType 内容类型
   * @return 是否跳过
   */
  private boolean shouldSkipContentType(final String contentType) {
    return SKIP_CONTENT_TYPES.stream().anyMatch(contentType::contains);
  }

  /** XSS请求包装器. */
  private static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /** 是否过滤请求体（保留用于未来扩展）. */
    @SuppressWarnings("unused")
    private final boolean filterBody;

    /** 危险的脚本模式. */
    private static final Pattern[] DANGEROUS_PATTERNS = {
      // Script标签
      Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
      Pattern.compile(
          "<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
      // Eval表达式
      Pattern.compile(
          "eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
      // JavaScript协议
      Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
      // VBScript协议
      Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
      // onXXX事件处理器
      Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
      // Expression
      Pattern.compile(
          "expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
      // src属性中的javascript
      Pattern.compile("src\\s*=\\s*['\"]?\\s*javascript:", Pattern.CASE_INSENSITIVE),
      // iframe
      Pattern.compile(
          "<iframe(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    /**
     * 构造函数.
     *
     * @param request HTTP请求
     * @param filterBody 是否过滤请求体
     */
    XssHttpServletRequestWrapper(final HttpServletRequest request, final boolean filterBody) {
      super(request);
      this.filterBody = filterBody;
    }

    /**
     * 获取参数.
     *
     * @param name 参数名
     * @return 参数值
     */
    @Override
    public String getParameter(final String name) {
      String value = super.getParameter(name);
      return cleanXss(value);
    }

    /**
     * 获取参数值数组.
     *
     * @param name 参数名
     * @return 参数值数组
     */
    @Override
    public String[] getParameterValues(final String name) {
      String[] values = super.getParameterValues(name);
      if (values == null) {
        return null;
      }
      String[] cleanedValues = new String[values.length];
      for (int i = 0; i < values.length; i++) {
        cleanedValues[i] = cleanXss(values[i]);
      }
      return cleanedValues;
    }

    /**
     * 获取请求头.
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    @Override
    public String getHeader(final String name) {
      String value = super.getHeader(name);
      return cleanXss(value);
    }

    /**
     * 清理XSS脚本.
     *
     * <p>注意：只移除危险的脚本模式，不进行 HTML 转义 HTML 转义应该在输出到 HTML 页面时进行，由前端框架处理
     *
     * @param value 原始值
     * @return 清理后的值
     */
    private String cleanXss(final String value) {
      if (value == null || value.isEmpty()) {
        return value;
      }

      String cleaned = value;

      // 使用正则表达式移除危险模式
      for (Pattern pattern : DANGEROUS_PATTERNS) {
        cleaned = pattern.matcher(cleaned).replaceAll("");
      }

      // 注意：不在这里进行 HTML 转义
      // HTML 转义应该在输出到前端 HTML 页面时进行
      // cleaned = HtmlUtils.htmlEscape(cleaned);

      // 如果值被修改，记录日志
      if (!value.equals(cleaned)) {
        log.warn(
            "XSS attack detected and cleaned. Original: [{}], Cleaned: [{}]",
            truncate(value, 100),
            truncate(cleaned, 100));
      }

      return cleaned;
    }

    /**
     * 截断字符串用于日志.
     *
     * @param str 原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(final String str, final int maxLength) {
      if (str == null) {
        return null;
      }
      if (str.length() <= maxLength) {
        return str;
      }
      return str.substring(0, maxLength) + "...";
    }
  }
}
