package com.lawfirm.application.workbench.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.workbench.command.CreateScheduledReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportLogDTO;
import com.lawfirm.application.system.command.SendNotificationCommand;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import com.lawfirm.domain.workbench.entity.ScheduledReport;
import com.lawfirm.domain.workbench.entity.ScheduledReportLog;
import com.lawfirm.domain.workbench.repository.ReportTemplateRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportLogRepository;
import com.lawfirm.domain.workbench.repository.ScheduledReportRepository;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportLogMapper;
import com.lawfirm.infrastructure.persistence.mapper.ScheduledReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时报表应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportAppService {

    private final ScheduledReportRepository scheduledReportRepository;
    private final ScheduledReportMapper scheduledReportMapper;
    private final ScheduledReportLogRepository logRepository;
    private final ScheduledReportLogMapper logMapper;
    private final ReportTemplateRepository templateRepository;
    private final CustomReportAppService customReportAppService;
    private final NotificationAppService notificationAppService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询定时报表任务
     */
    public PageResult<ScheduledReportDTO> listScheduledReports(int pageNum, int pageSize,
                                                                String keyword, String status) {
        IPage<ScheduledReport> page = scheduledReportMapper.selectScheduledReportPage(
                new Page<>(pageNum, pageSize),
                keyword, status, SecurityUtils.getUserId()
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toScheduledReportDTO).collect(Collectors.toList()),
                page.getTotal(),
                pageNum,
                pageSize
        );
    }

    /**
     * 获取定时任务详情
     */
    public ScheduledReportDTO getScheduledReportById(Long id) {
        ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");
        return toScheduledReportDTO(task);
    }

    /**
     * 创建定时报表任务
     */
    @Transactional
    public ScheduledReportDTO createScheduledReport(CreateScheduledReportCommand command) {
        // 验证模板存在
        ReportTemplate template = templateRepository.getByIdOrThrow(command.getTemplateId(), "报表模板不存在");
        
        // 验证调度配置
        validateScheduleConfig(command);

        ScheduledReport task = ScheduledReport.builder()
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
                .notifyEmails(command.getNotifyEmails() != null ? String.join(",", command.getNotifyEmails()) : null)
                .notifyUserIds(command.getNotifyUserIds() != null ? 
                        command.getNotifyUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")) : null)
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
     */
    @Transactional
    public ScheduledReportDTO updateScheduledReport(Long id, CreateScheduledReportCommand command) {
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
        task.setNotifyEmails(command.getNotifyEmails() != null ? String.join(",", command.getNotifyEmails()) : null);
        task.setNotifyUserIds(command.getNotifyUserIds() != null ? 
                command.getNotifyUserIds().stream().map(String::valueOf).collect(Collectors.joining(",")) : null);

        // 重新计算下次执行时间
        task.setNextExecuteTime(calculateNextExecuteTime(task));

        scheduledReportRepository.updateById(task);
        log.info("更新定时报表任务: {}", task.getTaskNo());
        return toScheduledReportDTO(task);
    }

    /**
     * 删除定时报表任务
     */
    @Transactional
    public void deleteScheduledReport(Long id) {
        ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");
        scheduledReportRepository.softDelete(id);
        log.info("删除定时报表任务: {}", task.getTaskNo());
    }

    /**
     * 暂停/恢复任务
     */
    @Transactional
    public void changeTaskStatus(Long id, String status) {
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
     */
    @Transactional
    public ReportDTO executeNow(Long id) {
        ScheduledReport task = scheduledReportRepository.getByIdOrThrow(id, "定时任务不存在");
        
        log.info("立即执行定时任务: {}", task.getTaskNo());
        return executeTask(task);
    }

    /**
     * 查询执行记录
     */
    public PageResult<ScheduledReportLogDTO> listExecuteLogs(Long taskId, int pageNum, int pageSize, String status) {
        IPage<ScheduledReportLog> page = logMapper.selectLogPage(
                new Page<>(pageNum, pageSize),
                taskId, status
        );

        return PageResult.of(
                page.getRecords().stream().map(this::toLogDTO).collect(Collectors.toList()),
                page.getTotal(),
                pageNum,
                pageSize
        );
    }

    /**
     * 执行待处理的定时任务（由定时调度器调用）
     */
    @Transactional
    public void executePendingTasks() {
        List<ScheduledReport> pendingTasks = scheduledReportRepository.findPendingTasks(LocalDateTime.now());
        
        for (ScheduledReport task : pendingTasks) {
            try {
                executeTask(task);
            } catch (Exception e) {
                log.error("执行定时任务失败: {}", task.getTaskNo(), e);
            }
        }
    }

    // ==================== 私有方法 ====================

    private ReportDTO executeTask(ScheduledReport task) {
        LocalDateTime startTime = LocalDateTime.now();
        ScheduledReportLog logRecord = ScheduledReportLog.builder()
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
            ReportDTO report = customReportAppService.generateReportByTemplate(
                    task.getTemplateId(), parameters, task.getOutputFormat());

            // 更新执行记录
            long duration = System.currentTimeMillis() - startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            logRecord.setStatus("SUCCESS");
            logRecord.setReportId(report.getId());
            logRecord.setFileUrl(report.getFileUrl());
            logRecord.setFileSize(report.getFileSize());
            logRecord.setDurationMs(duration);
            logRepository.updateById(logRecord);

            // 更新任务统计
            LocalDateTime nextExecuteTime = calculateNextExecuteTime(task);
            scheduledReportRepository.updateExecuteStats(task.getId(), startTime, "SUCCESS", nextExecuteTime, true);

            // 发送通知
            if (Boolean.TRUE.equals(task.getNotifyEnabled())) {
                sendNotification(task, report, logRecord);
            }

            return report;
        } catch (Exception e) {
            log.error("执行定时任务失败: {}", task.getTaskNo(), e);
            
            // 更新执行记录
            long duration = System.currentTimeMillis() - startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            logRecord.setStatus("FAILED");
            logRecord.setErrorMessage(e.getMessage());
            logRecord.setDurationMs(duration);
            logRepository.updateById(logRecord);

            // 更新任务统计
            LocalDateTime nextExecuteTime = calculateNextExecuteTime(task);
            scheduledReportRepository.updateExecuteStats(task.getId(), startTime, "FAILED", nextExecuteTime, false);

            throw new BusinessException("执行定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 发送定时报表通知
     */
    private void sendNotification(ScheduledReport task, ReportDTO report, ScheduledReportLog logRecord) {
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
            String content = String.format(
                    "您的定时报表「%s」已生成完成。\n报表编号: %s\n生成时间: %s\n状态: %s",
                    task.getTaskName(),
                    report.getReportNo(),
                    logRecord.getExecuteTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "SUCCESS".equals(logRecord.getStatus()) ? "成功" : "失败"
            );

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

            // TODO: 发送邮件通知（如果配置了邮件服务）
            sendEmailNotification(task, report, userIds);

        } catch (Exception e) {
            log.error("发送定时报表通知失败: taskNo={}", task.getTaskNo(), e);
            logRecord.setNotifyStatus("FAILED");
            logRecord.setNotifyResult("发送失败: " + e.getMessage());
            logRepository.updateById(logRecord);
        }
    }

    /**
     * 发送邮件通知（预留接口）
     */
    private void sendEmailNotification(ScheduledReport task, ReportDTO report, List<Long> userIds) {
        // 检查是否配置了邮件接收地址
        if (task.getNotifyEmails() == null || task.getNotifyEmails().isEmpty()) {
            return;
        }

        try {
            // TODO: 实现邮件发送逻辑
            // 1. 从配置中获取邮件服务器信息
            // 2. 构建邮件内容
            // 3. 发送邮件
            log.debug("邮件通知功能待实现: taskNo={}, emails={}",
                    task.getTaskNo(), task.getNotifyEmails());
        } catch (Exception e) {
            log.warn("发送邮件通知失败（不影响主流程）: taskNo={}", task.getTaskNo(), e);
        }
    }

    private void validateScheduleConfig(CreateScheduledReportCommand command) {
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
                if (command.getExecuteDayOfWeek() == null || command.getExecuteDayOfWeek() < 1 || command.getExecuteDayOfWeek() > 7) {
                    throw new BusinessException("每周执行需要指定有效的星期几(1-7)");
                }
            }
            case "MONTHLY" -> {
                if (command.getExecuteTime() == null || command.getExecuteTime().isEmpty()) {
                    throw new BusinessException("每月执行需要指定执行时间");
                }
                if (command.getExecuteDayOfMonth() == null || command.getExecuteDayOfMonth() < 1 || command.getExecuteDayOfMonth() > 31) {
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

    private LocalDateTime calculateNextExecuteTime(ScheduledReport task) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime executeTime = task.getExecuteTime() != null ? 
                LocalTime.parse(task.getExecuteTime(), DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.of(8, 0);

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
                // TODO: 解析Cron表达式计算下次执行时间
                yield now.plusDays(1).withHour(8).withMinute(0);
            }
            default -> now.plusDays(1);
        };
    }

    private ScheduledReportDTO toScheduledReportDTO(ScheduledReport task) {
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
            dto.setNotifyUserIds(Arrays.stream(task.getNotifyUserIds().split(","))
                    .map(Long::parseLong).collect(Collectors.toList()));
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

    private ScheduledReportLogDTO toLogDTO(ScheduledReportLog log) {
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

    private String generateTaskNo() {
        return "TASK" + System.currentTimeMillis();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败", e);
            return null;
        }
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败", e);
            return new HashMap<>();
        }
    }

    private String getScheduleTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "DAILY" -> "每日";
            case "WEEKLY" -> "每周";
            case "MONTHLY" -> "每月";
            case "CRON" -> "自定义";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "启用";
            case "PAUSED" -> "暂停";
            case "INACTIVE" -> "停用";
            default -> status;
        };
    }

    private String getExecuteStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "RUNNING" -> "执行中";
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            default -> status;
        };
    }

    private String getNotifyStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "SENT" -> "已发送";
            case "FAILED" -> "发送失败";
            case "SKIPPED" -> "跳过";
            default -> status;
        };
    }

    private String buildScheduleDescription(ScheduledReport task) {
        return switch (task.getScheduleType()) {
            case "DAILY" -> "每天 " + task.getExecuteTime() + " 执行";
            case "WEEKLY" -> "每周" + getDayOfWeekName(task.getExecuteDayOfWeek()) + " " + task.getExecuteTime() + " 执行";
            case "MONTHLY" -> "每月" + task.getExecuteDayOfMonth() + "日 " + task.getExecuteTime() + " 执行";
            case "CRON" -> "Cron: " + task.getCronExpression();
            default -> "";
        };
    }

    private String getDayOfWeekName(Integer dayOfWeek) {
        if (dayOfWeek == null) return "";
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

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private String formatDuration(Long ms) {
        if (ms == null || ms == 0) return "0ms";
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%.1fmin", ms / 60000.0);
    }
}
