package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTaskCommand;
import com.lawfirm.application.matter.dto.TaskDTO;
import com.lawfirm.application.matter.dto.TaskQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAppService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    /**
     * 分页查询任务
     */
    public PageResult<TaskDTO> listTasks(TaskQueryDTO query) {
        IPage<Task> page = taskMapper.selectTaskPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getMatterId(),
                query.getAssigneeId(),
                query.getStatus(),
                query.getPriority(),
                query.getTitle()
        );

        List<TaskDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建任务
     */
    @Transactional
    public TaskDTO createTask(CreateTaskCommand command) {
        String taskNo = generateTaskNo();

        Task task = Task.builder()
                .taskNo(taskNo)
                .matterId(command.getMatterId())
                .parentId(command.getParentId())
                .title(command.getTitle())
                .description(command.getDescription())
                .priority(command.getPriority() != null ? command.getPriority() : "MEDIUM")
                .assigneeId(command.getAssigneeId())
                .assigneeName(command.getAssigneeName())
                .startDate(command.getStartDate())
                .dueDate(command.getDueDate())
                .reminderDate(command.getReminderDate())
                .status("TODO")
                .progress(0)
                .build();

        taskRepository.save(task);
        log.info("任务创建成功: {} ({})", task.getTitle(), task.getTaskNo());
        return toDTO(task);
    }

    /**
     * 获取任务详情
     */
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");
        return toDTO(task);
    }

    /**
     * 更新任务
     */
    @Transactional
    public TaskDTO updateTask(Long id, String title, String description, String priority,
                              Long assigneeId, String assigneeName, LocalDate startDate,
                              LocalDate dueDate, LocalDateTime reminderDate) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");

        if (StringUtils.hasText(title)) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (StringUtils.hasText(priority)) task.setPriority(priority);
        if (assigneeId != null) {
            task.setAssigneeId(assigneeId);
            task.setAssigneeName(assigneeName);
        }
        if (startDate != null) task.setStartDate(startDate);
        if (dueDate != null) task.setDueDate(dueDate);
        if (reminderDate != null) task.setReminderDate(reminderDate);

        taskRepository.updateById(task);
        log.info("任务更新成功: {}", task.getTitle());
        return toDTO(task);
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");

        // 检查是否有子任务
        long subTaskCount = taskRepository.count(
                new LambdaQueryWrapper<Task>().eq(Task::getParentId, id));
        if (subTaskCount > 0) {
            throw new BusinessException("该任务有子任务，请先删除子任务");
        }

        taskRepository.removeById(id);
        log.info("任务删除成功: {}", task.getTitle());
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public TaskDTO updateStatus(Long id, String status) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");
        task.setStatus(status);

        if ("COMPLETED".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);
        } else if ("TODO".equals(status)) {
            task.setCompletedAt(null);
            task.setProgress(0);
        }

        taskRepository.updateById(task);
        log.info("任务状态更新: {} -> {}", task.getTitle(), status);
        return toDTO(task);
    }

    /**
     * 更新任务进度
     */
    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");

        if (progress < 0 || progress > 100) {
            throw new BusinessException("进度必须在0-100之间");
        }

        task.setProgress(progress);
        if (progress == 100) {
            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
        } else if (progress > 0) {
            task.setStatus("IN_PROGRESS");
        }

        taskRepository.updateById(task);
        log.info("任务进度更新: {} -> {}%", task.getTitle(), progress);
        return toDTO(task);
    }

    /**
     * 获取我的待办任务
     */
    public List<TaskDTO> getMyTodoTasks() {
        Long userId = SecurityUtils.getUserId();
        List<Task> tasks = taskRepository.findMyTodoTasks(userId);
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取即将到期的任务
     */
    public List<TaskDTO> getUpcomingTasks(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        List<Task> tasks = taskRepository.findUpcomingTasks(today, deadline);
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取逾期任务
     */
    public List<TaskDTO> getOverdueTasks() {
        List<Task> tasks = taskRepository.findOverdueTasks(LocalDate.now());
        return tasks.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取案件任务统计
     */
    public int[] getMatterTaskStats(Long matterId) {
        int total = taskRepository.countByMatter(matterId);
        int completed = taskRepository.countCompletedByMatter(matterId);
        return new int[]{total, completed};
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        String prefix = "TK" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取优先级名称
     */
    private String getPriorityName(String priority) {
        if (priority == null) return null;
        return switch (priority) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "TODO" -> "待办";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private TaskDTO toDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTaskNo(task.getTaskNo());
        dto.setMatterId(task.getMatterId());
        dto.setParentId(task.getParentId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setPriority(task.getPriority());
        dto.setPriorityName(getPriorityName(task.getPriority()));
        dto.setAssigneeId(task.getAssigneeId());
        dto.setAssigneeName(task.getAssigneeName());
        dto.setStartDate(task.getStartDate());
        dto.setDueDate(task.getDueDate());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setStatus(task.getStatus());
        dto.setStatusName(getStatusName(task.getStatus()));
        dto.setProgress(task.getProgress());
        dto.setReminderDate(task.getReminderDate());
        dto.setReminderSent(task.getReminderSent());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());

        // 判断是否逾期
        if (task.getDueDate() != null && !"COMPLETED".equals(task.getStatus())
                && !"CANCELLED".equals(task.getStatus())) {
            dto.setOverdue(task.getDueDate().isBefore(LocalDate.now()));
        } else {
            dto.setOverdue(false);
        }

        return dto;
    }
}
