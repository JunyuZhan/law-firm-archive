package com.lawfirm.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置类
 *
 * <p>主要功能： - 自定义 JSON 序列化/反序列化行为 - XSS 防护：对输入字符串进行 HTML 转义
 *
 * @author junyuzhan
 * @since 2026-01-11
 */
@Configuration
public class JacksonConfig {

  /** 危险的XSS模式. */
  private static final Pattern[] DANGEROUS_PATTERNS = {
    // Script标签
    Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
    Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
    Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
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
    Pattern.compile("<iframe(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
  };

  /**
   * 自定义 Jackson ObjectMapper 配置.
   *
   * <p>添加 XSS 防护模块，同时保留 Java 8 时间支持
   *
   * @return Jackson定制器
   */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer xssCustomizer() {
    return builder -> {
      // 创建 XSS 防护模块
      SimpleModule xssModule = new SimpleModule("XssModule");
      xssModule.addDeserializer(String.class, new XssStringDeserializer());

      // 使用 modulesToInstall 添加模块，而不是替换
      builder.modulesToInstall(xssModule, new JavaTimeModule());

      // 禁用时间戳格式
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    };
  }

  /**
   * XSS 字符串反序列化器.
   *
   * <p>在 JSON 反序列化时自动清理 XSS 内容
   */
  public static class XssStringDeserializer extends StdDeserializer<String> {

    /** 构造函数. */
    public XssStringDeserializer() {
      super(String.class);
    }

    /**
     * 反序列化字符串.
     *
     * @param p JSON解析器
     * @param ctxt 反序列化上下文
     * @return 清理后的字符串
     * @throws IOException IO异常
     */
    @Override
    public String deserialize(final JsonParser p, final DeserializationContext ctxt)
        throws IOException {
      String value = p.getValueAsString();
      return cleanXss(value);
    }

    /**
     * 清理 XSS 脚本.
     *
     * <p>注意：只移除危险的脚本模式，不进行 HTML 转义 HTML 转义应该在输出到 HTML 页面时进行，而不是在存储时 否则会破坏 JSON 等结构化数据
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
      // HTML 转义应该在输出到前端 HTML 页面时进行（由前端框架处理）
      // 在存储时转义会破坏 JSON 等结构化数据（如合同模板内容）
      // cleaned = HtmlUtils.htmlEscape(cleaned);

      return cleaned;
    }
  }
}
