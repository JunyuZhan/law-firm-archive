package com.lawfirm.application.workbench.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.system.command.SendNotificationCommand;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.command.CreateScheduledReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportLogDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import com.lawfirm.domain.workbench.repository.ReportTemplateRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportLogRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportRepository;
import com.lawfirm.infrastructure.notification.EmailService;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 定时报表应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportAppService {

  /** 定时报表仓储 */
  private final ScheduledReportRepository scheduledReportRepository;

  /** 定时报表Mapper */
  private final ScheduledReportMapper scheduledReportMapper;

  /** 定时报表日志仓储 */
  private final ScheduledReportLogRepository logRepository;

  /** 定时报表日志Mapper */
  private final ScheduledReportLogMapper logMapper;

  /** 报表模板仓储 */
  private final ReportTemplateRepository templateRepository;

  /** 自定义报表应用服务 */
  private final CustomReportAppService customReportAppService;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 邮件服务 */
  private final EmailService emailService;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /**
   * 分页查询定时报表任务
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param keyword 关键词
   * @param status 状态
   * @return 分页结果
   */
  public PageResult<ScheduledReportDTO> listScheduledReports(
      final int pageNum, final int pageSize, final String keyword, final String status) {
    IPage<ScheduledReport> page =
        scheduledReportMapper.selectScheduledReportPage(
            new Page<>(pageNum, pageSize), keyword, status, SecurityUtils.getUserId());

    return PageResult.of(
        page.getRecords().stream().map(this::toScheduledReportDTO).collect(Collectors.toList()),
        page.getTotal(),
        pageNum,
        pageSize);
  }

  /**
   * 获取定时任务详情
   *
   * @param id 任务ID
   * @return 定时报表DTO
   */
  public ScheduledReportDTO getScheduledReportById(final Long id) {
    ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");
    return toScheduledReportDTO(task);
  }

  /**
   * 创建定时报表任务
   *
   * @param command 创建命令
   * @return 定时报表DTO
   */
  @Transactional
  public ScheduledReportDTO createScheduledReport(final CreateScheduledReportCommand command) {
    // 验证模板存在
    templateRepository.getByIdOrThrow(command.getTemplateId(), "报表模板不存在");

    // 验证调度配置
    validateScheduleConfig(command);

    ScheduledReport task =
        ScheduledReport.builder()
            .taskNo(generateTaskNo())
            .taskName(command.getTaskName())
            .description(command.getDescription())
            .templateId(command.getTemplateId())
            .scheduleType(command.getScheduleType())
            .cronExpression(command.getCronExpression())
            .executeTime(command.getExecuteTime())
            .executeDayOfWeek(command.getExecuteDayOfWeek())
            .executeDayOfMonth(command.getExecuteDayOfMonth())
            .reportParameters(toJson(command.getReportParameters()))
            .outputFormat(command.getOutputFormat() != null ? command.getOutputFormat() : "EXCEL")
            .notifyEnabled(command.getNotifyEnabled() != null ? command.getNotifyEnabled() : false)
            .notifyEmails(
                command.getNotifyEmails() != null
                    ? String.join(",", command.getNotifyEmails())
                    : null)
            .notifyUserIds(
                command.getNotifyUserIds() != null
                    ? command.getNotifyUserIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
                    : null)
            .status("ACTIVE")
            .totalExecuteCount(0)
            .successCount(0)
            .failCount(0)
            .createdByName(SecurityUtils.getRealName())
            .build();

    // 计算下次执行时间
    task.setNextExecuteTime(calculateNextExecuteTime(task));

    scheduledReportRepository.save(task);
    log.info("创建定时报表任务: {}", task.getTaskNo());
    return toScheduledReportDTO(task);
  }

  /**
   * 更新定时报表任务
   *
   * @param id 任务ID
   * @param command 更新命令
   * @return 定时报表DTO
   */
  @Transactional
  public ScheduledReportDTO updateScheduledReport(
      final Long id, final CreateScheduledReportCommand command) {
    ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");

    // 验证模板存在
    templateRepository.getByIdOrThrow(command.getTemplateId(), "报表模板不存在");

    // 验证调度配置
    validateScheduleConfig(command);

    task.setTaskName(command.getTaskName());
    task.setDescription(command.getDescription());
    task.setTemplateId(command.getTemplateId());
    task.setScheduleType(command.getScheduleType());
    task.setCronExpression(command.getCronExpression());
    task.setExecuteTime(command.getExecuteTime());
    task.setExecuteDayOfWeek(command.getExecuteDayOfWeek());
    task.setExecuteDayOfMonth(command.getExecuteDayOfMonth());
    task.setReportParameters(toJson(command.getReportParameters()));
    task.setOutputFormat(command.getOutputFormat());
    task.setNotifyEnabled(command.getNotifyEnabled());
    task.setNotifyEmails(
        command.getNotifyEmails() != null ? String.join(",", command.getNotifyEmails()) : null);
    task.setNotifyUserIds(
        command.getNotifyUserIds() != null
            ? command.getNotifyUserIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","))
            : null);

    // 重新计算下次执行时间
    task.setNextExecuteTime(calculateNextExecuteTime(task));

    scheduledReportRepository.updateById(task);
    log.info("更新定时报表任务: {}", task.getTaskNo());
    return toScheduledReportDTO(task);
  }

  /**
   * 删除定时报表任务
   *
   * @param id 任务ID
   */
  @Transactional
  public void deleteScheduledReport(final Long id) {
    ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");
    scheduledReportRepository.softDelete(id);
    log.info("删除定时报表任务: {}", task.getTaskNo());
  }

  /**
   * 暂停/恢复任务
   *
   * @param id 任务ID
   * @param status 状态
   */
  @Transactional
  public void changeTaskStatus(final Long id, final String status) {
    ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");

    if ("ACTIVE".equals(status)) {
      // 恢复任务时重新计算下次执行时间
      task.setNextExecuteTime(calculateNextExecuteTime(task));
    }

    task.setStatus(status);
    scheduledReportRepository.updateById(task);
    log.info("修改定时任务状态: {} -> {}", task.getTaskNo(), status);
  }

  /**
   * 立即执行任务
   *
   * @param id 任务ID
   * @return 报表DTO
   */
  @Transactional
  public ReportDTO executeNow(final Long id) {
    ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");

    log.info("立即执行定时任务: {}", task.getTaskNo());
    return executeTask(task);
  }

  /**
   * 查询执行记录
   *
   * @param taskId 任务ID
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param status 状态
   * @return 分页结果
   */
  public PageResult<ScheduledReportLogDTO> listExecuteLogs(
      final Long taskId, final int pageNum, final int pageSize, final String status) {
    IPage<ScheduledReportLog> page =
        logMapper.selectLogPage(new Page<>(pageNum, pageSize), taskId, status);

    return PageResult.of(
        page.getRecords().stream().map(this::toLogDTO).collect(Collectors.toList()),
        page.getTotal(),
        pageNum,
        pageSize);
  }

  /** 执行待处理的定时任务（由定时调度器调用）. */
  @Transactional
  public void executePendingTasks() {
    List<ScheduledReport> pendingTasks =
        scheduledReportRepository.findPendingTasks(LocalDateTime.now());

    for (ScheduledReport task : pendingTasks) {
      try {
        executeTask(task);
      } catch (Exception e) {
        log.error("执行定时任务失败: {}", task.getTaskNo(), e);
      }
    }
  }

  // ==================== 私有方法 ====================

  /**
   * 执行定时报表任务.
   *
   * @param task 定时报表任务
   * @return 报表DTO
   * @throws BusinessException 如果执行失败
   */
  private ReportDTO executeTask(final ScheduledReport task) {
    LocalDateTime startTime = LocalDateTime.now();
    ScheduledReportLog logRecord =
        ScheduledReportLog.builder()
            .taskId(task.getId())
            .taskNo(task.getTaskNo())
            .executeTime(startTime)
            .status("RUNNING")
            .build();
    logRepository.save(logRecord);

    try {
      // 解析报表参数
      Map<String, Object> parameters = parseJson(task.getReportParameters());

      // 生成报表
      ReportDTO report =
          customReportAppService.generateReportByTemplate(
              task.getTemplateId(), parameters, task.getOutputFormat());

      // 更新执行记录
      long duration =
          System.currentTimeMillis()
              - startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
      logRecord.setStatus("SUCCESS");
      logRecord.setReportId(report.getId());
      logRecord.setFileUrl(report.getFileUrl());
      logRecord.setFileSize(report.getFileSize());
      logRecord.setDurationMs(duration);
      logRepository.updateById(logRecord);

      // 更新任务统计
      LocalDateTime nextExecuteTime = calculateNextExecuteTime(task);
      scheduledReportRepository.updateExecuteStats(
          task.getId(), startTime, "SUCCESS", nextExecuteTime, true);

      // 发送通知
      if (Boolean.TRUE.equals(task.getNotifyEnabled())) {
        sendNotification(task, report, logRecord);
      }

      return report;
    } catch (Exception e) {
      log.error("执行定时任务失败: {}", task.getTaskNo(), e);

      // 更新执行记录
      long duration =
          System.currentTimeMillis()
              - startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
      logRecord.setStatus("FAILED");
      logRecord.setErrorMessage(e.getMessage());
      logRecord.setDurationMs(duration);
      logRepository.updateById(logRecord);

      // 更新任务统计
      LocalDateTime nextExecuteTime = calculateNextExecuteTime(task);
      scheduledReportRepository.updateExecuteStats(
          task.getId(), startTime, "FAILED", nextExecuteTime, false);

      throw new BusinessException("执行定时任务失败: " + e.getMessage());
    }
  }

  /**
   * 发送定时报表通知
   *
   * @param task 定时报表任务
   * @param report 报表DTO
   * @param logRecord 日志记录
   */
  private void sendNotification(
      final ScheduledReport task, final ReportDTO report, final ScheduledReportLog logRecord) {
    try {
      // 解析接收人
      List<Long> userIds = new ArrayList<>();

      // 添加任务创建者
      if (task.getCreatedBy() != null) {
        userIds.add(task.getCreatedBy());
      }

      // 添加指定的通知用户
      if (task.getNotifyUserIds() != null && !task.getNotifyUserIds().isEmpty()) {
        Arrays.stream(task.getNotifyUserIds().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .forEach(userIds::add);
      }

      // 去重
      userIds = userIds.stream().distinct().collect(Collectors.toList());

      if (userIds.isEmpty()) {
        logRecord.setNotifyStatus("SKIPPED");
        logRecord.setNotifyResult("无接收人");
        logRepository.updateById(logRecord);
        return;
      }

      // 构建通知内容
      String title = "定时报表生成完成";
      String content =
          String.format(
              "您的定时报表「%s」已生成完成。\n报表编号: %s\n生成时间: %s\n状态: %s",
              task.getTaskName(),
              report.getReportNo(),
              logRecord.getExecuteTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
              "SUCCESS".equals(logRecord.getStatus()) ? "成功" : "失败");

      // 发送站内通知
      SendNotificationCommand command = new SendNotificationCommand();
      command.setReceiverIds(userIds);
      command.setTitle(title);
      command.setContent(content);
      command.setType(Notification.TYPE_SYSTEM);
      command.setBusinessType("SCHEDULED_REPORT");
      command.setBusinessId(report.getId());

      notificationAppService.sendNotification(command);

      // 更新通知状态
      logRecord.setNotifyStatus("SENT");
      logRecord.setNotifyResult(String.format("已发送给%d个用户", userIds.size()));
      logRepository.updateById(logRecord);

      log.info("发送定时报表通知成功: taskNo={}, receivers={}", task.getTaskNo(), userIds.size());

      // 发送邮件通知（如果配置了邮件服务）
      sendEmailNotification(task, report, userIds);

    } catch (Exception e) {
      log.error("发送定时报表通知失败: taskNo={}", task.getTaskNo(), e);
      logRecord.setNotifyStatus("FAILED");
      logRecord.setNotifyResult("发送失败: " + e.getMessage());
      logRepository.updateById(logRecord);
    }
  }

  /**
   * 发送邮件通知
   *
   * @param task 定时报表任务
   * @param report 报表DTO
   * @param userIds 用户ID列表
   */
  private void sendEmailNotification(
      final ScheduledReport task, final ReportDTO report, final List<Long> userIds) {
    // 检查邮件服务是否启用
    if (!emailService.isEnabled()) {
      log.debug("邮件服务未启用，跳过邮件通知: taskNo={}", task.getTaskNo());
      return;
    }

    // 收集所有需要发送邮件的地址
    Set<String> emailAddresses = new HashSet<>();

    // 1. 添加配置的邮件地址
    if (task.getNotifyEmails() != null && !task.getNotifyEmails().isEmpty()) {
      Arrays.stream(task.getNotifyEmails().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .forEach(emailAddresses::add);
    }

    // 2. 从用户ID获取邮箱地址
    if (userIds != null && !userIds.isEmpty()) {
      List<User> users = userRepository.listByIds(userIds);
      users.stream()
          .map(User::getEmail)
          .filter(email -> email != null && !email.trim().isEmpty())
          .forEach(emailAddresses::add);
    }

    if (emailAddresses.isEmpty()) {
      log.debug("无有效的邮件接收地址，跳过邮件通知: taskNo={}", task.getTaskNo());
      return;
    }

    try {
      // 构建邮件主题
      String subject = String.format("定时报表生成完成 - %s", task.getTaskName());

      // 构建HTML邮件内容
      String htmlContent = buildEmailContent(task, report);

      // 发送邮件给所有收件人
      int successCount = 0;
      int failCount = 0;
      for (String email : emailAddresses) {
        try {
          emailService.sendHtmlEmail(email, subject, htmlContent);
          successCount++;
          log.debug("定时报表邮件发送成功: taskNo={}, email={}", task.getTaskNo(), email);
        } catch (Exception e) {
          failCount++;
          log.warn("定时报表邮件发送失败: taskNo={}, email={}", task.getTaskNo(), email, e);
        }
      }

      log.info(
          "定时报表邮件通知完成: taskNo={}, total={}, success={}, fail={}",
          task.getTaskNo(),
          emailAddresses.size(),
          successCount,
          failCount);
    } catch (Exception e) {
      log.warn("发送邮件通知失败（不影响主流程）: taskNo={}", task.getTaskNo(), e);
    }
  }

  /**
   * 构建邮件HTML内容
   *
   * @param task 定时报表任务
   * @param report 报表DTO
   * @return HTML内容
   */
  private String buildEmailContent(final ScheduledReport task, final ReportDTO report) {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html>");
    html.append("<head>");
    html.append("<meta charset=\"UTF-8\">");
    html.append("<style>");
    html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
    html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
    html.append(
        ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
    html.append(".content { padding: 20px; background-color: #f9f9f9; }");
    html.append(".info-row { margin: 10px 0; }");
    html.append(".label { font-weight: bold; color: #555; }");
    html.append(".value { color: #333; }");
    html.append(".status-success { color: #4CAF50; font-weight: bold; }");
    html.append(".status-failed { color: #f44336; font-weight: bold; }");
    html.append(".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }");
    html.append("</style>");
    html.append("</head>");
    html.append("<body>");
    html.append("<div class=\"container\">");
    html.append("<div class=\"header\">");
    html.append("<h2>定时报表生成完成</h2>");
    html.append("</div>");
    html.append("<div class=\"content\">");
    html.append("<p>您好，</p>");
    html.append("<p>您的定时报表已生成完成，详情如下：</p>");
    html.append("<div class=\"info-row\">");
    html.append("<span class=\"label\">任务名称：</span>");
    html.append("<span class=\"value\">").append(escapeHtml(task.getTaskName())).append("</span>");
    html.append("</div>");
    html.append("<div class=\"info-row\">");
    html.append("<span class=\"label\">报表编号：</span>");
    html.append("<span class=\"value\">")
        .append(escapeHtml(report.getReportNo()))
        .append("</span>");
    html.append("</div>");
    html.append("<div class=\"info-row\">");
    html.append("<span class=\"label\">生成时间：</span>");
    html.append("<span class=\"value\">")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .append("</span>");
    html.append("</div>");
    if (report.getFileUrl() != null) {
      html.append("<div class=\"info-row\">");
      html.append("<span class=\"label\">报表文件：</span>");
      html.append("<span class=\"value\"><a href=\"")
          .append(escapeHtml(report.getFileUrl()))
          .append("\">点击下载</a></span>");
      html.append("</div>");
    }
    if (report.getFileSize() != null) {
      html.append("<div class=\"info-row\">");
      html.append("<span class=\"label\">文件大小：</span>");
      html.append("<span class=\"value\">")
          .append(formatFileSize(report.getFileSize()))
          .append("</span>");
      html.append("</div>");
    }
    html.append("</div>");
    html.append("<div class=\"footer\">");
    html.append("<p>此邮件由系统自动发送，请勿回复。</p>");
    html.append("</div>");
    html.append("</div>");
    html.append("</body>");
    html.append("</html>");
    return html.toString();
  }

  /**
   * HTML转义，防止XSS攻击
   *
   * @param text 文本
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

  /**
   * 验证调度配置.
   *
   * @param command 创建命令
   * @throws BusinessException 如果配置无效
   */
  private void validateScheduleConfig(final CreateScheduledReportCommand command) {
    switch (command.getScheduleType()) {
      case "DAILY" -> {
        if (command.getExecuteTime() == null || command.getExecuteTime().isEmpty()) {
          throw new BusinessException("每日执行需要指定执行时间");
        }
      }
      case "WEEKLY" -> {
        if (command.getExecuteTime() == null || command.getExecuteTime().isEmpty()) {
          throw new BusinessException("每周执行需要指定执行时间");
        }
        if (command.getExecuteDayOfWeek() == null
            || command.getExecuteDayOfWeek() < 1
            || command.getExecuteDayOfWeek() > 7) {
          throw new BusinessException("每周执行需要指定有效的星期几(1-7)");
        }
      }
      case "MONTHLY" -> {
        if (command.getExecuteTime() == null || command.getExecuteTime().isEmpty()) {
          throw new BusinessException("每月执行需要指定执行时间");
        }
        final int minDayOfMonth = 1;
        final int maxDayOfMonth = 31;
        if (command.getExecuteDayOfMonth() == null
            || command.getExecuteDayOfMonth() < minDayOfMonth
            || command.getExecuteDayOfMonth() > maxDayOfMonth) {
          throw new BusinessException("每月执行需要指定有效的日期(1-31)");
        }
      }
      case "CRON" -> {
        if (command.getCronExpression() == null || command.getCronExpression().isEmpty()) {
          throw new BusinessException("自定义调度需要指定Cron表达式");
        }
      }
      default -> throw new BusinessException("不支持的调度类型: " + command.getScheduleType());
    }
  }

  /**
   * 计算下次执行时间.
   *
   * @param task 定时报表任务
   * @return 下次执行时间
   */
  private LocalDateTime calculateNextExecuteTime(final ScheduledReport task) {
    LocalDateTime now = LocalDateTime.now();
    LocalTime executeTime =
        task.getExecuteTime() != null
            ? LocalTime.parse(task.getExecuteTime(), DateTimeFormatter.ofPattern("HH:mm"))
            : LocalTime.of(8, 0);

    return switch (task.getScheduleType()) {
      case "DAILY" -> {
        LocalDateTime next = now.toLocalDate().atTime(executeTime);
        yield next.isAfter(now) ? next : next.plusDays(1);
      }
      case "WEEKLY" -> {
        int targetDayOfWeek = task.getExecuteDayOfWeek();
        int currentDayOfWeek = now.getDayOfWeek().getValue();
        int daysToAdd = targetDayOfWeek - currentDayOfWeek;
        if (daysToAdd < 0 || (daysToAdd == 0 && now.toLocalTime().isAfter(executeTime))) {
          daysToAdd += 7;
        }
        yield now.toLocalDate().plusDays(daysToAdd).atTime(executeTime);
      }
      case "MONTHLY" -> {
        int targetDay = Math.min(task.getExecuteDayOfMonth(), now.toLocalDate().lengthOfMonth());
        LocalDateTime next = now.toLocalDate().withDayOfMonth(targetDay).atTime(executeTime);
        yield next.isAfter(now) ? next : next.plusMonths(1);
      }
      case "CRON" -> {
        if (task.getCronExpression() == null || task.getCronExpression().isEmpty()) {
          log.warn("CRON表达式为空，使用默认值: 明天8点");
          yield now.plusDays(1).withHour(8).withMinute(0);
        }
        try {
          // 解析Cron表达式并计算下次执行时间
          CronExpression cronExpression = CronExpression.parse(task.getCronExpression());
          LocalDateTime nextExecution = cronExpression.next(now);
          if (nextExecution == null) {
            log.warn("无法计算CRON表达式的下次执行时间: {}, 使用默认值: 明天8点", task.getCronExpression());
            yield now.plusDays(1).withHour(8).withMinute(0);
          }
          yield nextExecution;
        } catch (Exception e) {
          log.error("解析CRON表达式失败: {}, 使用默认值: 明天8点", task.getCronExpression(), e);
          yield now.plusDays(1).withHour(8).withMinute(0);
        }
      }
      default -> now.plusDays(1);
    };
  }

  /**
   * Entity转DTO.
   *
   * @param task 定时报表任务实体
   * @return 定时报表DTO
   */
  private ScheduledReportDTO toScheduledReportDTO(final ScheduledReport task) {
    ScheduledReportDTO dto = new ScheduledReportDTO();
    dto.setId(task.getId());
    dto.setTaskNo(task.getTaskNo());
    dto.setTaskName(task.getTaskName());
    dto.setDescription(task.getDescription());
    dto.setTemplateId(task.getTemplateId());

    // 获取模板名称
    ReportTemplate template = templateRepository.findById(task.getTemplateId());
    if (template != null) {
      dto.setTemplateName(template.getTemplateName());
    }

    dto.setScheduleType(task.getScheduleType());
    dto.setScheduleTypeName(getScheduleTypeName(task.getScheduleType()));
    dto.setCronExpression(task.getCronExpression());
    dto.setExecuteTime(task.getExecuteTime());
    dto.setExecuteDayOfWeek(task.getExecuteDayOfWeek());
    dto.setExecuteDayOfMonth(task.getExecuteDayOfMonth());
    dto.setScheduleDescription(buildScheduleDescription(task));

    dto.setReportParameters(parseJson(task.getReportParameters()));
    dto.setOutputFormat(task.getOutputFormat());

    dto.setNotifyEnabled(task.getNotifyEnabled());
    if (task.getNotifyEmails() != null && !task.getNotifyEmails().isEmpty()) {
      dto.setNotifyEmails(Arrays.asList(task.getNotifyEmails().split(",")));
    }
    if (task.getNotifyUserIds() != null && !task.getNotifyUserIds().isEmpty()) {
      dto.setNotifyUserIds(
          Arrays.stream(task.getNotifyUserIds().split(","))
              .map(Long::parseLong)
              .collect(Collectors.toList()));
    }

    dto.setStatus(task.getStatus());
    dto.setStatusName(getStatusName(task.getStatus()));

    dto.setLastExecuteTime(task.getLastExecuteTime());
    dto.setLastExecuteStatus(task.getLastExecuteStatus());
    dto.setLastExecuteStatusName(getExecuteStatusName(task.getLastExecuteStatus()));
    dto.setNextExecuteTime(task.getNextExecuteTime());

    dto.setTotalExecuteCount(task.getTotalExecuteCount());
    dto.setSuccessCount(task.getSuccessCount());
    dto.setFailCount(task.getFailCount());

    dto.setCreatedBy(task.getCreatedBy());
    dto.setCreatedByName(task.getCreatedByName());
    dto.setCreatedAt(task.getCreatedAt());
    dto.setUpdatedAt(task.getUpdatedAt());

    return dto;
  }

  /**
   * Entity转DTO.
   *
   * @param log 定时报表日志实体
   * @return 定时报表日志DTO
   */
  private ScheduledReportLogDTO toLogDTO(final ScheduledReportLog log) {
    ScheduledReportLogDTO dto = new ScheduledReportLogDTO();
    dto.setId(log.getId());
    dto.setTaskId(log.getTaskId());
    dto.setTaskNo(log.getTaskNo());
    dto.setExecuteTime(log.getExecuteTime());
    dto.setStatus(log.getStatus());
    dto.setStatusName(getExecuteStatusName(log.getStatus()));
    dto.setReportId(log.getReportId());
    dto.setFileUrl(log.getFileUrl());
    dto.setFileSize(log.getFileSize());
    dto.setFileSizeDisplay(formatFileSize(log.getFileSize()));
    dto.setDurationMs(log.getDurationMs());
    dto.setDurationDisplay(formatDuration(log.getDurationMs()));
    dto.setErrorMessage(log.getErrorMessage());
    dto.setNotifyStatus(log.getNotifyStatus());
    dto.setNotifyStatusName(getNotifyStatusName(log.getNotifyStatus()));
    dto.setNotifyResult(log.getNotifyResult());
    dto.setCreatedAt(log.getCreatedAt());
    return dto;
  }

  /**
   * 生成任务编号.
   *
   * @return 任务编号
   */
  private String generateTaskNo() {
    return "TASK" + System.currentTimeMillis();
  }

  /**
   * 转换为JSON字符串.
   *
   * @param obj 对象
   * @return JSON字符串
   */
  private String toJson(final Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.warn("JSON序列化失败", e);
      return null;
    }
  }

  /**
   * 解析JSON字符串为Map.
   *
   * @param json JSON字符串
   * @return Map对象
   */
  private Map<String, Object> parseJson(final String json) {
    if (json == null || json.isEmpty()) {
      return new HashMap<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      log.warn("JSON解析失败", e);
      return new HashMap<>();
    }
  }

  /**
   * 获取调度类型名称.
   *
   * @param type 调度类型代码
   * @return 调度类型名称
   */
  private String getScheduleTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "DAILY" -> "每日";
      case "WEEKLY" -> "每周";
      case "MONTHLY" -> "每月";
      case "CRON" -> "自定义";
      default -> type;
    };
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "ACTIVE" -> "启用";
      case "PAUSED" -> "暂停";
      case "INACTIVE" -> "停用";
      default -> status;
    };
  }

  /**
   * 获取执行状态名称.
   *
   * @param status 执行状态代码
   * @return 执行状态名称
   */
  private String getExecuteStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "RUNNING" -> "执行中";
      case "SUCCESS" -> "成功";
      case "FAILED" -> "失败";
      default -> status;
    };
  }

  /**
   * 获取通知状态名称.
   *
   * @param status 通知状态代码
   * @return 通知状态名称
   */
  private String getNotifyStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "SENT" -> "已发送";
      case "FAILED" -> "发送失败";
      case "SKIPPED" -> "跳过";
      default -> status;
    };
  }

  /**
   * 构建调度描述.
   *
   * @param task 定时报表任务
   * @return 调度描述字符串
   */
  private String buildScheduleDescription(final ScheduledReport task) {
    return switch (task.getScheduleType()) {
      case "DAILY" -> "每天 " + task.getExecuteTime() + " 执行";
      case "WEEKLY" -> "每周"
          + getDayOfWeekName(task.getExecuteDayOfWeek())
          + " "
          + task.getExecuteTime()
          + " 执行";
      case "MONTHLY" -> "每月" + task.getExecuteDayOfMonth() + "日 " + task.getExecuteTime() + " 执行";
      case "CRON" -> "Cron: " + task.getCronExpression();
      default -> "";
    };
  }

  /**
   * 获取星期名称.
   *
   * @param dayOfWeek 星期数（1-7）
   * @return 星期名称
   */
  private String getDayOfWeekName(final Integer dayOfWeek) {
    if (dayOfWeek == null) {
      return "";
    }
    return switch (dayOfWeek) {
      case 1 -> "一";
      case 2 -> "二";
      case 3 -> "三";
      case 4 -> "四";
      case 5 -> "五";
      case 6 -> "六";
      case 7 -> "日";
      default -> "";
    };
  }

  /**
   * 格式化文件大小.
   *
   * @param bytes 字节数
   * @return 格式化后的文件大小字符串
   */
  private String formatFileSize(final Long bytes) {
    if (bytes == null || bytes == 0) {
      return "0 B";
    }
    final int bytesPerKb = 1024;
    String[] units = {"B", "KB", "MB", "GB"};
    int unitIndex = 0;
    double size = bytes;
    while (size >= bytesPerKb && unitIndex < units.length - 1) {
      size /= bytesPerKb;
      unitIndex++;
    }
    return String.format("%.2f %s", size, units[unitIndex]);
  }

  /**
   * 格式化持续时间.
   *
   * @param ms 毫秒数
   * @return 格式化后的持续时间字符串
   */
  private String formatDuration(final Long ms) {
    if (ms == null || ms == 0) {
      return "0ms";
    }
    final int millisecondsPerSecond = 1000;
    final int millisecondsPerMinute = 60000;
    if (ms < millisecondsPerSecond) {
      return ms + "ms";
    }
    if (ms < millisecondsPerMinute) {
      return String.format("%.1fs", ms / (double) millisecondsPerSecond);
    }
    return String.format("%.1fmin", ms / (double) millisecondsPerMinute);
  }
}
