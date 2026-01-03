package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.dto.MatterTimelineDTO;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.PaymentMapper;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目时间线应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterTimelineAppService {

    private final MatterRepository matterRepository;
    private final TaskMapper taskMapper;
    private final TimesheetMapper timesheetMapper;
    private final FinanceContractMapper financeContractMapper;
    private final PaymentMapper paymentMapper;
    private final UserRepository userRepository;

    /**
     * 获取项目时间线
     */
    public List<MatterTimelineDTO> getMatterTimeline(Long matterId) {
        // 验证项目存在
        Matter matter = matterRepository.getByIdOrThrow(matterId, "项目不存在");

        List<MatterTimelineDTO> timeline = new ArrayList<>();

        // 1. 项目创建事件
        if (matter.getCreatedAt() != null) {
            MatterTimelineDTO createdEvent = createTimelineEvent(
                    "CREATED",
                    "项目创建",
                    matter.getCreatedAt(),
                    "项目已创建",
                    matter.getCreatedBy(),
                    matterId,
                    "MATTER"
            );
            timeline.add(createdEvent);
        }

        // 2. 任务完成事件
        List<Task> completedTasks = taskMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Task>lambdaQuery()
                        .eq(Task::getMatterId, matterId)
                        .eq(Task::getStatus, "COMPLETED")
                        .isNotNull(Task::getCompletedAt)
                        .eq(Task::getDeleted, false)
        );
        for (Task task : completedTasks) {
            MatterTimelineDTO taskEvent = createTimelineEvent(
                    "TASK_COMPLETED",
                    "任务完成",
                    task.getCompletedAt(),
                    String.format("任务「%s」已完成", task.getTitle()),
                    task.getAssigneeId(),
                    task.getId(),
                    "TASK"
            );
            timeline.add(taskEvent);
        }

        // 3. 合同签署事件
        Contract contract = financeContractMapper.selectByMatterId(matterId);
        if (contract != null && contract.getSignDate() != null) {
            LocalDateTime signDateTime = contract.getSignDate().atStartOfDay();
            MatterTimelineDTO contractEvent = createTimelineEvent(
                    "CONTRACT_SIGNED",
                    "合同签署",
                    signDateTime,
                    String.format("合同「%s」已签署", contract.getName()),
                    contract.getSignerId(),
                    contract.getId(),
                    "CONTRACT"
            );
            timeline.add(contractEvent);
        }

        // 4. 收款记录事件（通过案件ID查询）
        List<Payment> payments = paymentMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Payment>lambdaQuery()
                        .eq(Payment::getMatterId, matterId)
                        .eq(Payment::getStatus, "CONFIRMED")
                        .eq(Payment::getDeleted, false)
        );
        for (Payment payment : payments) {
            if (payment.getPaymentDate() != null) {
                LocalDateTime paymentDateTime = payment.getPaymentDate().atStartOfDay();
                MatterTimelineDTO paymentEvent = createTimelineEvent(
                        "PAYMENT_RECEIVED",
                        "收款记录",
                        paymentDateTime,
                        String.format("收到收款：¥%.2f", payment.getAmount()),
                        payment.getCreatedBy(),
                        payment.getId(),
                        "PAYMENT"
                );
                timeline.add(paymentEvent);
            }
        }

        // 5. 工时记录事件（只显示重要的工时记录，如审批通过的）
        List<Timesheet> approvedTimesheets = timesheetMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Timesheet>lambdaQuery()
                        .eq(Timesheet::getMatterId, matterId)
                        .eq(Timesheet::getStatus, "APPROVED")
                        .isNotNull(Timesheet::getApprovedAt)
                        .eq(Timesheet::getDeleted, false)
        );
        // 按日期分组，每天只显示一条汇总记录
        approvedTimesheets.stream()
                .collect(Collectors.groupingBy(Timesheet::getWorkDate))
                .forEach((date, sheets) -> {
                    double totalHours = sheets.stream()
                            .mapToDouble(ts -> ts.getHours() != null ? ts.getHours().doubleValue() : 0)
                            .sum();
                    if (totalHours > 0) {
                        LocalDateTime workDateTime = date.atStartOfDay();
                        MatterTimelineDTO timesheetEvent = createTimelineEvent(
                                "TIMESHEET_RECORDED",
                                "工时记录",
                                workDateTime,
                                String.format("工时记录：%.2f小时", totalHours),
                                sheets.get(0).getUserId(),
                                sheets.get(0).getId(),
                                "TIMESHEET"
                        );
                        timeline.add(timesheetEvent);
                    }
                });

        // 6. 项目状态变更（从updated_at推断，如果有操作日志更好）
        if (matter.getUpdatedAt() != null && !matter.getUpdatedAt().equals(matter.getCreatedAt())) {
            MatterTimelineDTO statusEvent = createTimelineEvent(
                    "STATUS_CHANGED",
                    "状态更新",
                    matter.getUpdatedAt(),
                    String.format("项目状态：%s", getStatusName(matter.getStatus())),
                    matter.getUpdatedBy(),
                    matterId,
                    "MATTER"
            );
            timeline.add(statusEvent);
        }

        // 按时间倒序排序
        timeline.sort(Comparator.comparing(MatterTimelineDTO::getEventTime).reversed());

        return timeline;
    }

    /**
     * 创建时间线事件
     */
    private MatterTimelineDTO createTimelineEvent(String eventType, String eventTypeName,
                                                   LocalDateTime eventTime, String description,
                                                   Long operatorId, Long relatedId, String relatedType) {
        MatterTimelineDTO event = new MatterTimelineDTO();
        event.setEventId(generateEventId(eventType, relatedId));
        event.setEventType(eventType);
        event.setEventTypeName(eventTypeName);
        event.setEventTime(eventTime);
        event.setTitle(eventTypeName);
        event.setDescription(description);
        event.setOperatorId(operatorId);
        event.setRelatedId(relatedId);
        event.setRelatedType(relatedType);

        // 获取操作人姓名
        if (operatorId != null) {
            var user = userRepository.findById(operatorId);
            if (user != null) {
                event.setOperatorName(user.getRealName());
            }
        }

        return event;
    }

    /**
     * 生成事件ID
     */
    private String generateEventId(String eventType, Long relatedId) {
        return eventType + "_" + relatedId + "_" + System.currentTimeMillis();
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "ACTIVE" -> "进行中";
            case "SUSPENDED" -> "暂停";
            case "CLOSED" -> "结案";
            case "ARCHIVED" -> "归档";
            default -> status;
        };
    }
}

