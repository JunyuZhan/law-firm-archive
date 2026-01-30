package com.lawfirm.application.admin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 函件模板格式化工具 支持结构化模板格式的解析和格式化. */
@Slf4j
@Component
public class LetterTemplateFormatter {

  /** JSON转换器. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * 检查内容是否为结构化格式
   *
   * @param content 内容字符串
   * @return 如果是结构化格式返回true，否则返回false
   */
  public boolean isStructuredFormat(final String content) {
    if (content == null || content.trim().isEmpty()) {
      return false;
    }

    String trimmed = content.trim();
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
      return false;
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(trimmed);
      return root.has("_structured") && root.get("_structured").asBoolean();
    } catch (Exception e) {
      log.debug("内容不是有效的结构化JSON格式: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 格式化结构化函件模板为打印 HTML
   *
   * @param content 结构化JSON内容
   * @param variables 变量替换映射
   * @return 格式化后的HTML内容
   */
  public String formatStructuredLetter(final String content, final Map<String, String> variables) {
    if (content == null || content.trim().isEmpty()) {
      return "<p>模板内容为空</p>";
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(content.trim());
      if (!root.has("_structured") || !root.get("_structured").asBoolean()) {
        // 不是结构化格式，直接返回原内容（让后续的变量替换处理）
        return content;
      }

      JsonNode blocks = root.get("blocks");
      if (blocks == null || !blocks.isObject()) {
        return "<p style=\"color: red;\">⚠️ 模板内容格式错误：缺少 blocks 字段</p>";
      }

      StringBuilder html = new StringBuilder();

      // 1. 标题区（居中显示）- 公文格式：二号方正小标宋/仿宋
      if (blocks.has("title")) {
        JsonNode title = blocks.get("title");
        if (title.has("letterTitle")) {
          String letterTitle = title.get("letterTitle").asText("");
          if (!letterTitle.trim().isEmpty()) {
            letterTitle = replaceVariables(letterTitle, variables);
            html.append("<div style=\"text-align: center; margin-bottom: 30px;\">\n");
            html.append(
                    "  <h2 style=\"text-align: center; font-family: 'FZXiaoBiaoSong-B05S', "
                        + "'方正小标宋简体', '方正小标宋', 'FZXBS', 'FangSong', '仿宋', serif; "
                        + "font-size: 22pt; font-weight: normal; letter-spacing: 2pt; "
                        + "margin: 0 0 10px 0;\">")
                .append(escapeHtml(letterTitle))
                .append("</h2>\n");
            html.append("</div>\n");
          }
        }

        // 编号（如果有）- 公文格式：四号仿宋GB2312，右对齐
        if (title.has("letterNo")) {
          String letterNo = title.get("letterNo").asText("");
          if (!letterNo.trim().isEmpty()) {
            letterNo = replaceVariables(letterNo, variables);
            html.append(
                    "<p style=\"text-align: right; font-family: 'FangSong', '仿宋_GB2312', "
                        + "'仿宋', serif; font-size: 14pt; line-height: 24pt; "
                        + "margin: 0 0 20px 0;\">")
                .append(escapeHtml(letterNo))
                .append("</p>\n");
          }
        }
      }

      // 2. 收件单位（左对齐）- 公文格式：三号仿宋GB2312
      if (blocks.has("recipient")) {
        String recipient = blocks.get("recipient").asText("");
        if (!recipient.trim().isEmpty()) {
          recipient = replaceVariables(recipient, variables);
          html.append(
                  "<p style=\"text-indent: 0; margin-bottom: 20px; font-family: 'FangSong', "
                      + "'仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;\">"
                      + "<strong>")
              .append(escapeHtml(recipient))
              .append("</strong></p>\n");
        }
      }

      // 3. 正文内容（段落缩进）- 公文格式：三号仿宋GB2312
      if (blocks.has("body")) {
        String body = blocks.get("body").asText("");
        if (!body.trim().isEmpty()) {
          body = replaceVariables(body, variables);
          html.append("<div style=\"margin: 20px 0;\">\n");
          html.append(formatParagraphs(body, true));
          html.append("</div>\n");
        }
      }

      // 4. 落款区（右对齐）- 公文格式：三号仿宋GB2312
      if (blocks.has("signature")) {
        JsonNode signature = blocks.get("signature");
        html.append(
            "<div style=\"text-align: right; margin-top: 40px; font-family: 'FangSong', "
                + "'仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;\">\n");

        if (signature.has("lawyerNames")) {
          String lawyerNames = signature.get("lawyerNames").asText("");
          if (!lawyerNames.trim().isEmpty()) {
            lawyerNames = replaceVariables(lawyerNames, variables);
            html.append("  <p style=\"text-indent: 0; margin: 0 0 8px 0;\">")
                .append(escapeHtml(lawyerNames))
                .append("</p>\n");
          }
        }

        if (signature.has("contactInfo")) {
          String contactInfo = signature.get("contactInfo").asText("");
          if (!contactInfo.trim().isEmpty()) {
            contactInfo = replaceVariables(contactInfo, variables);
            html.append("  <p style=\"text-indent: 0; margin: 0 0 8px 0;\">")
                .append(escapeHtml(contactInfo))
                .append("</p>\n");
          }
        }

        if (signature.has("firmName")) {
          String firmName = signature.get("firmName").asText("");
          if (!firmName.trim().isEmpty()) {
            firmName = replaceVariables(firmName, variables);
            html.append("  <p style=\"text-indent: 0; margin: 0 0 8px 0;\">")
                .append(escapeHtml(firmName))
                .append("</p>\n");
          }
        }

        if (signature.has("date")) {
          String date = signature.get("date").asText("");
          if (!date.trim().isEmpty()) {
            date = replaceVariables(date, variables);
            html.append("  <p style=\"text-indent: 0; margin: 0;\">")
                .append(escapeHtml(date))
                .append("</p>\n");
          }
        }

        html.append("</div>\n");
      }

      if (html.length() == 0) {
        return "<p style=\"color: orange;\">⚠️ 模板内容为空，请填写至少一个区块的内容</p>";
      }

      return html.toString();

    } catch (Exception e) {
      log.error("格式化结构化函件模板失败", e);
      return "<p style=\"color: red;\">⚠️ 模板内容格式错误，无法解析: " + escapeHtml(e.getMessage()) + "</p>";
    }
  }

  /**
   * 替换文本中的变量
   *
   * @param text 原始文本
   * @param variables 变量映射表
   * @return 替换后的文本
   */
  private String replaceVariables(final String text, final Map<String, String> variables) {
    if (text == null || variables == null) {
      return text != null ? text : "";
    }

    String result = text;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue() != null ? entry.getValue() : "";
      // 转义特殊字符，避免在正则表达式中出错
      String escapedKey = Pattern.quote(key);
      result = result.replaceAll("\\$\\{" + escapedKey + "\\}", value);
    }
    return result;
  }

  /**
   * 将文本转换为HTML段落（公文格式：三号仿宋GB2312）
   *
   * @param text 原始文本
   * @param indent 是否缩进
   * @return HTML格式的段落
   */
  private String formatParagraphs(final String text, final boolean indent) {
    if (text == null || text.trim().isEmpty()) {
      return "";
    }

    String[] lines = text.split("\n");
    StringBuilder html = new StringBuilder();
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        // 公文格式：三号仿宋GB2312，行距28pt
        html.append(
                "<p style=\"font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt; ")
            .append(indent ? "text-indent: 2em; " : "")
            .append("margin: 4px 0;\">")
            .append(escapeHtml(trimmed))
            .append("</p>\n");
      }
    }
    return html.toString();
  }

  /**
   * 转义HTML特殊字符
   *
   * @param text 原始文本
   * @return 转义后的文本
   */
  private String escapeHtml(final String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
