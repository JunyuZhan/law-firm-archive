package com.lawfirm.infrastructure.notification;

import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.system.entity.UserWecom;
import com.lawfirm.infrastructure.persistence.mapper.UserWecomMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 企业微信通知渠道
 *
 * <p>将系统通知推送到企业微信群机器人
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WecomNotificationChannel {

  /** 企业微信机器人客户端 */
  private final WecomBotClient wecomBotClient;

  /** 外部集成服务 */
  private final ExternalIntegrationAppService integrationService;

  /** 用户企业微信映射Mapper */
  private final UserWecomMapper userWecomMapper;

  /**
   * 异步发送单条通知到企业微信
   *
   * @param notification 通知对象
   */
  @Async
  public void sendNotification(final Notification notification) {
    try {
      // 1. 获取企业微信机器人配置
      ExternalIntegration config = getWecomConfig();
      if (config == null) {
        log.debug("企业微信机器人未配置或未启用，跳过推送");
        return;
      }

      // 2. 检查通知类型是否需要推送
      if (!shouldPush(config, notification.getType())) {
        log.debug("通知类型{}不在推送范围内，跳过", notification.getType());
        return;
      }

      // 3. 获取Webhook地址
      String webhookUrl = config.getApiKey();
      if (webhookUrl == null || webhookUrl.isEmpty()) {
        log.warn("企业微信机器人Webhook地址未配置");
        return;
      }

      // 4. 获取接收者的企业微信ID
      List<String> mentionedList = new ArrayList<>();
      if (notification.getReceiverId() != null) {
        UserWecom userWecom = userWecomMapper.selectByUserId(notification.getReceiverId());
        if (userWecom != null && userWecom.getWecomUserid() != null && userWecom.getEnabled()) {
          mentionedList.add(userWecom.getWecomUserid());
        }
      }

      // 5. 构建消息内容
      String content = buildMessageContent(notification);

      // 6. 发送消息
      boolean success = wecomBotClient.sendText(webhookUrl, content, mentionedList, null);

      if (success) {
        log.info(
            "企业微信通知发送成功: title={}, receiver={}",
            notification.getTitle(),
            notification.getReceiverId());
      } else {
        log.warn("企业微信通知发送失败: title={}", notification.getTitle());
      }

    } catch (Exception e) {
      log.error("发送企业微信通知失败: notificationId={}", notification.getId(), e);
    }
  }

  /**
   * 批量发送通知
   *
   * @param notifications 通知列表
   */
  @Async
  public void sendNotifications(final List<Notification> notifications) {
    ExternalIntegration config = getWecomConfig();
    if (config == null) {
      return;
    }

    for (Notification notification : notifications) {
      try {
        sendNotificationInternal(config, notification);
      } catch (Exception e) {
        log.error("发送企业微信通知失败: id={}", notification.getId(), e);
      }
    }
  }

  /**
   * 发送紧急通知（Markdown格式，更醒目）
   *
   * @param title 标题
   * @param content 内容
   * @param receiverIds 接收者ID列表
   */
  @Async
  public void sendUrgentNotification(
      final String title, final String content, final List<Long> receiverIds) {
    try {
      ExternalIntegration config = getWecomConfig();
      if (config == null) {
        return;
      }

      String webhookUrl = config.getApiKey();

      // 构建Markdown内容
      StringBuilder markdown = new StringBuilder();
      markdown.append("## ⚠️ ").append(title).append("\n\n");
      markdown.append(content).append("\n\n");
      markdown.append("> 请及时处理！");

      wecomBotClient.sendMarkdown(webhookUrl, markdown.toString());

      log.info("紧急通知发送成功: title={}", title);

    } catch (Exception e) {
      log.error("发送紧急通知失败: title={}", title, e);
    }
  }

  /**
   * 发送开庭提醒（带详细信息）
   *
   * @param receiverId 接收者ID
   * @param matterTitle 案件标题
   * @param caseNo 案号
   * @param courtName 法院名称
   * @param courtTime 开庭时间
   * @param location 地点
   */
  @Async
  public void sendCourtReminder(
      final Long receiverId,
      final String matterTitle,
      final String caseNo,
      final String courtName,
      final String courtTime,
      final String location) {
    try {
      ExternalIntegration config = getWecomConfig();
      if (config == null) {
        return;
      }

      String webhookUrl = config.getApiKey();

      // 构建Markdown内容
      StringBuilder markdown = new StringBuilder();
      markdown.append("## 📅 开庭提醒\n\n");
      markdown.append("> **案件**：").append(matterTitle).append("\n");
      markdown.append("> **案号**：").append(caseNo != null ? caseNo : "暂无").append("\n");
      markdown.append("> **法院**：").append(courtName != null ? courtName : "待定").append("\n");
      markdown.append("> **时间**：<font color=\"warning\">").append(courtTime).append("</font>\n");
      if (location != null) {
        markdown.append("> **地点**：").append(location).append("\n");
      }
      markdown.append("\n请提前做好准备！");

      // @相关人员
      List<String> mentionedList = new ArrayList<>();
      if (receiverId != null) {
        UserWecom userWecom = userWecomMapper.selectByUserId(receiverId);
        if (userWecom != null && userWecom.getWecomUserid() != null) {
          mentionedList.add(userWecom.getWecomUserid());
        }
      }

      wecomBotClient.sendMarkdown(webhookUrl, markdown.toString());

      log.info("开庭提醒发送成功: caseNo={}", caseNo);

    } catch (Exception e) {
      log.error("发送开庭提醒失败", e);
    }
  }

  /**
   * 发送任务到期提醒
   *
   * @param receiverId 接收者ID
   * @param taskTitle 任务标题
   * @param dueDate 到期日期
   * @param daysLeft 剩余天数
   */
  @Async
  public void sendTaskDeadlineReminder(
      final Long receiverId, final String taskTitle, final String dueDate, final int daysLeft) {
    try {
      ExternalIntegration config = getWecomConfig();
      if (config == null) {
        return;
      }

      String webhookUrl = config.getApiKey();

      final int oneDayThreshold = 1;
      String icon = daysLeft <= 0 ? "🚨" : (daysLeft <= oneDayThreshold ? "⚠️" : "📋");
      String urgency =
          daysLeft <= 0 ? "已逾期" : (daysLeft == oneDayThreshold ? "明天到期" : daysLeft + "天后到期");
      String color = daysLeft <= 0 ? "warning" : "info";

      StringBuilder markdown = new StringBuilder();
      markdown.append("## ").append(icon).append(" 任务提醒\n\n");
      markdown.append("> **任务**：").append(taskTitle).append("\n");
      markdown.append("> **截止日期**：").append(dueDate).append("\n");
      markdown
          .append("> **状态**：<font color=\"")
          .append(color)
          .append("\">")
          .append(urgency)
          .append("</font>\n");

      wecomBotClient.sendMarkdown(webhookUrl, markdown.toString());

    } catch (Exception e) {
      log.error("发送任务提醒失败", e);
    }
  }

  /**
   * 发送审批通知
   *
   * @param receiverId 接收者ID
   * @param approvalType 审批类型
   * @param applicant 申请人
   * @param summary 摘要
   */
  @Async
  public void sendApprovalNotification(
      final Long receiverId,
      final String approvalType,
      final String applicant,
      final String summary) {
    try {
      ExternalIntegration config = getWecomConfig();
      if (config == null) {
        return;
      }

      String webhookUrl = config.getApiKey();

      StringBuilder markdown = new StringBuilder();
      markdown.append("## ✅ 审批待办\n\n");
      markdown.append("> **类型**：").append(approvalType).append("\n");
      markdown.append("> **申请人**：").append(applicant).append("\n");
      markdown.append("> **摘要**：").append(summary).append("\n");
      markdown.append("\n请及时处理！");

      wecomBotClient.sendMarkdown(webhookUrl, markdown.toString());

    } catch (Exception e) {
      log.error("发送审批通知失败", e);
    }
  }

  /**
   * 检查企业微信通知是否已启用.
   *
   * @return 是否已启用
   */
  public boolean isEnabled() {
    ExternalIntegration config = getWecomConfig();
    return config != null && config.getEnabled();
  }

  /**
   * 测试企业微信连接.
   *
   * @return 连接是否成功
   */
  public boolean testConnection() {
    ExternalIntegration config = getWecomConfig();
    if (config == null || config.getApiKey() == null) {
      return false;
    }
    return wecomBotClient.testConnection(config.getApiKey());
  }

  // ==================== 私有方法 ====================

  /**
   * 获取企业微信配置
   *
   * @return 企业微信配置
   */
  private ExternalIntegration getWecomConfig() {
    try {
      return integrationService.getFirstEnabledIntegrationByType(
          ExternalIntegration.TYPE_NOTIFICATION);
    } catch (Exception e) {
      log.debug("获取企业微信配置失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 内部发送方法
   *
   * @param config 企业微信配置
   * @param notification 通知对象
   */
  private void sendNotificationInternal(
      final ExternalIntegration config, final Notification notification) {
    if (!shouldPush(config, notification.getType())) {
      return;
    }

    String webhookUrl = config.getApiKey();
    if (webhookUrl == null || webhookUrl.isEmpty()) {
      return;
    }

    List<String> mentionedList = new ArrayList<>();
    if (notification.getReceiverId() != null) {
      UserWecom userWecom = userWecomMapper.selectByUserId(notification.getReceiverId());
      if (userWecom != null && userWecom.getWecomUserid() != null && userWecom.getEnabled()) {
        mentionedList.add(userWecom.getWecomUserid());
      }
    }

    String content = buildMessageContent(notification);
    wecomBotClient.sendText(webhookUrl, content, mentionedList, null);
  }

  /**
   * 检查是否应该推送该类型通知
   *
   * @param config 企业微信配置
   * @param notificationType 通知类型
   * @return 是否应该推送
   */
  @SuppressWarnings("unchecked")
  private boolean shouldPush(final ExternalIntegration config, final String notificationType) {
    if (config.getExtraConfig() == null) {
      return true; // 默认全部推送
    }

    Object enabledTypes = config.getExtraConfig().get("enabledTypes");
    if (enabledTypes instanceof List) {
      return ((List<String>) enabledTypes).contains(notificationType);
    }

    return true;
  }

  /**
   * 构建消息内容
   *
   * @param notification 通知对象
   * @return 消息内容
   */
  private String buildMessageContent(final Notification notification) {
    StringBuilder content = new StringBuilder();

    // 添加图标
    String icon = getTypeIcon(notification.getType());
    content.append(icon).append(" ");

    // 标题
    content.append("【").append(notification.getTitle()).append("】\n");

    // 内容
    content.append(notification.getContent());

    return content.toString();
  }

  /**
   * 获取通知类型图标.
   *
   * @param type 通知类型
   * @return 图标字符串
   */
  private String getTypeIcon(final String type) {
    if (type == null) {
      return "📢";
    }
    return switch (type) {
      case "TASK" -> "📋";
      case "SCHEDULE" -> "📅";
      case "APPROVAL" -> "✅";
      case "REMINDER" -> "⏰";
      case "WARNING" -> "⚠️";
      case "CONTRACT" -> "📄";
      case "SYSTEM" -> "🔔";
      default -> "📢";
    };
  }
}
