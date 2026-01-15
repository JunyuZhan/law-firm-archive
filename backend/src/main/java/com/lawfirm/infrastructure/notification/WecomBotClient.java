package com.lawfirm.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 企业微信机器人客户端
 * 
 * 文档：https://developer.work.weixin.qq.com/document/path/91770
 * 
 * 支持消息类型：
 * - 文本消息（支持@指定人）
 * - Markdown消息（更丰富的格式）
 * - 图文消息（带图片和链接）
 * - 模板卡片消息（可交互）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomBotClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送文本消息
     * 
     * @param webhookUrl Webhook地址
     * @param content 消息内容（最长2048字节）
     * @param mentionedList 需要@的用户ID列表（企业微信UserId）
     * @param mentionedMobileList 需要@的手机号列表
     * @return 是否发送成功
     */
    public boolean sendText(String webhookUrl, String content, 
                           List<String> mentionedList, List<String> mentionedMobileList) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            
            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            
            if (mentionedList != null && !mentionedList.isEmpty()) {
                text.put("mentioned_list", mentionedList);
            }
            if (mentionedMobileList != null && !mentionedMobileList.isEmpty()) {
                text.put("mentioned_mobile_list", mentionedMobileList);
            }
            
            body.put("text", text);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信文本消息失败", e);
            return false;
        }
    }

    /**
     * 发送文本消息（简化版）
     */
    public boolean sendText(String webhookUrl, String content) {
        return sendText(webhookUrl, content, null, null);
    }

    /**
     * 发送Markdown消息
     * 
     * 支持的格式：
     * - 标题：# 一级标题 ~ ###### 六级标题
     * - 加粗：**text**
     * - 链接：[text](url)
     * - 引用：> text
     * - 字体颜色：<font color="info">绿色</font>、<font color="warning">橙色</font>、<font color="comment">灰色</font>
     * 
     * @param webhookUrl Webhook地址
     * @param content Markdown内容（最长4096字节）
     * @return 是否发送成功
     */
    public boolean sendMarkdown(String webhookUrl, String content) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", content);
            
            body.put("markdown", markdown);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信Markdown消息失败", e);
            return false;
        }
    }

    /**
     * 发送图文消息
     * 
     * @param webhookUrl Webhook地址
     * @param title 标题（最长128字节）
     * @param description 描述（最长512字节）
     * @param url 点击跳转链接
     * @param picUrl 图片链接（可选）
     * @return 是否发送成功
     */
    public boolean sendNews(String webhookUrl, String title, String description, 
                           String url, String picUrl) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "news");
            
            Map<String, Object> article = new HashMap<>();
            article.put("title", title);
            article.put("description", description);
            article.put("url", url);
            if (picUrl != null && !picUrl.isEmpty()) {
                article.put("picurl", picUrl);
            }
            
            Map<String, Object> news = new HashMap<>();
            news.put("articles", Collections.singletonList(article));
            
            body.put("news", news);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信图文消息失败", e);
            return false;
        }
    }

    /**
     * 发送模板卡片消息（文本通知型）
     * 
     * @param webhookUrl Webhook地址
     * @param title 标题
     * @param description 描述
     * @param url 点击跳转链接
     * @param source 来源描述（如"律所管理系统"）
     * @return 是否发送成功
     */
    public boolean sendTextCard(String webhookUrl, String title, String description, 
                                String url, String source) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "template_card");
            
            Map<String, Object> card = new HashMap<>();
            card.put("card_type", "text_notice");
            
            // 来源
            if (source != null) {
                Map<String, Object> sourceObj = new HashMap<>();
                sourceObj.put("desc", source);
                card.put("source", sourceObj);
            }
            
            // 主标题
            Map<String, Object> mainTitle = new HashMap<>();
            mainTitle.put("title", title);
            if (description != null) {
                mainTitle.put("desc", description);
            }
            card.put("main_title", mainTitle);
            
            // 点击跳转
            if (url != null) {
                Map<String, Object> cardAction = new HashMap<>();
                cardAction.put("type", 1);
                cardAction.put("url", url);
                card.put("card_action", cardAction);
            }
            
            body.put("template_card", card);
            
            return sendRequest(webhookUrl, body);
            
        } catch (Exception e) {
            log.error("发送企业微信卡片消息失败", e);
            return false;
        }
    }

    /**
     * 发送带关键信息的通知卡片
     * 
     * @param webhookUrl Webhook地址
     * @param title 标题
     * @param keyValues 关键信息键值对（如：案号=xxx，法院=xxx）
     * @param url 点击跳转链接
     * @return 是否发送成功
     */
    public boolean sendKeyValueCard(String webhookUrl, String title, 
                                    Map<String, String> keyValues, String url) {
        try {
            // 构建Markdown内容
            StringBuilder content = new StringBuilder();
            content.append("### ").append(title).append("\n");
            
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                content.append("> **").append(entry.getKey()).append("**：")
                       .append(entry.getValue()).append("\n");
            }
            
            if (url != null) {
                content.append("\n[点击查看详情](").append(url).append(")");
            }
            
            return sendMarkdown(webhookUrl, content.toString());
            
        } catch (Exception e) {
            log.error("发送企业微信关键信息卡片失败", e);
            return false;
        }
    }

    /**
     * 发送@所有人的消息
     */
    public boolean sendTextMentionAll(String webhookUrl, String content) {
        return sendText(webhookUrl, content, Collections.singletonList("@all"), null);
    }

    /**
     * 测试Webhook连接
     * 
     * @param webhookUrl Webhook地址
     * @return 是否连接成功
     */
    public boolean testConnection(String webhookUrl) {
        try {
            return sendText(webhookUrl, "🔔 律所管理系统连接测试成功！");
        } catch (Exception e) {
            log.error("测试企业微信机器人连接失败", e);
            return false;
        }
    }

    /**
     * 发送HTTP请求
     */
    private boolean sendRequest(String webhookUrl, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                int errcode = (Integer) result.getOrDefault("errcode", -1);
                
                if (errcode == 0) {
                    log.debug("企业微信消息发送成功");
                    return true;
                } else {
                    log.warn("企业微信消息发送失败: errcode={}, errmsg={}", 
                            errcode, result.get("errmsg"));
                }
            } else {
                log.warn("企业微信API返回非200状态: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("发送企业微信消息请求失败", e);
        }
        return false;
    }
}
