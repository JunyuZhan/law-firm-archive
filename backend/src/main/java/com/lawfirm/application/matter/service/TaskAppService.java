package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateTaskCommand;
import com.lawfirm.application.matter.dto.TaskDTO;
import com.lawfirm.application.matter.dto.TaskQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final MatterRepository matterRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationAppService notificationAppService;

    /**
     * 分页查询任务
     */
    public PageResult<TaskDTO> listTasks(TaskQueryDTO query) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getDeleted, false);
        
        if (query.getMatterId() != null) {
            wrapper.eq(Task::getMatterId, query.getMatterId());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(Task::getAssigneeId, query.getAssigneeId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Task::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getPriority())) {
            wrapper.eq(Task::getPriority, query.getPriority());
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Task::getTitle, query.getTitle());
        }
        
        // 应用数据权限过滤
        applyDataScopeFilter(wrapper);
        
        // 排序：截止日期 -> 创建时间（优先级排序在应用层处理）
        wrapper.orderByAsc(Task::getDueDate);
        wrapper.orderByDesc(Task::getCreatedAt);

        IPage<Task> page = taskRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<TaskDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .sorted((a, b) -> {
                    // 按优先级排序
                    int priorityOrderA = getPriorityOrder(a.getPriority());
                    int priorityOrderB = getPriorityOrder(b.getPriority());
                    if (priorityOrderA != priorityOrderB) {
                        return Integer.compare(priorityOrderA, priorityOrderB);
                    }
                    // 优先级相同，按截止日期排序
                    if (a.getDueDate() != null && b.getDueDate() != null) {
                        return a.getDueDate().compareTo(b.getDueDate());
                    }
                    if (a.getDueDate() != null) return -1;
                    if (b.getDueDate() != null) return 1;
                    return 0;
                })
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }
    
    /**
     * 获取优先级排序值
     */
    private int getPriorityOrder(String priority) {
        if (priority == null) return 4;
        return switch (priority) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
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
        
        // 发送任务分配通知
        sendTaskAssignNotification(task);
        
        return toDTO(task);
    }
    
    /**
     * 发送任务分配通知
     */
    private void sendTaskAssignNotification(Task task) {
        try {
            if (task.getAssigneeId() == null) return;
            
            Long currentUserId = SecurityUtils.getUserId();
            // 如果任务分配给自己，不发通知
            if (task.getAssigneeId().equals(currentUserId)) return;
            
            String currentUserName = SecurityUtils.getRealName();
            String priorityText = getPriorityText(task.getPriority());
            String title = "您有新任务";
            String content = String.format("%s 给您分配了任务【%s】", currentUserName, task.getTitle());
            if (task.getDueDate() != null) {
                content += String.format("，截止日期：%s", task.getDueDate());
            }
            if (!"MEDIUM".equals(task.getPriority())) {
                content += String.format("（%s）", priorityText);
            }
            
            notificationAppService.sendSystemNotification(
                    task.getAssigneeId(),
                    title,
                    content,
                    "TASK",
                    task.getId()
            );
            log.info("任务分配通知已发送: taskId={}, assigneeId={}", task.getId(), task.getAssigneeId());
        } catch (Exception e) {
            log.warn("发送任务分配通知失败: taskId={}", task.getId(), e);
        }
    }
    
    private String getPriorityText(String priority) {
        if (priority == null) return "普通";
        return switch (priority) {
            case "URGENT" -> "紧急";
            case "HIGH" -> "高优先级";
            case "MEDIUM" -> "普通";
            case "LOW" -> "低优先级";
            default -> priority;
        };
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
        
        // 验证操作权限：只有任务创建者或负责人可以编辑
        validateTaskOperationPermission(task);

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
        
        // 验证操作权限：只有任务创建者或负责人可以删除
        validateTaskOperationPermission(task);

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
        
        // 验证操作权限：只有任务创建者或负责人可以更新状态
        validateTaskOperationPermission(task);
        
        String oldStatus = task.getStatus();
        
        // 如果负责人点击"完成"，状态变为"待验收"而不是"已完成"
        if ("COMPLETED".equals(status)) {
            task.setStatus("PENDING_REVIEW");
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);
            task.setReviewStatus("PENDING_REVIEW");
        } else {
            task.setStatus(status);
            if ("TODO".equals(status)) {
                task.setCompletedAt(null);
                task.setProgress(0);
                task.setReviewStatus(null);
            }
        }

        taskRepository.updateById(task);
        log.info("任务状态更新: {} -> {}", task.getTitle(), task.getStatus());
        
        // 发送任务状态变更通知
        sendTaskStatusNotification(task, oldStatus, task.getStatus());
        
        return toDTO(task);
    }
    
    /**
     * 验收任务（通过）
     */
    @Transactional
    public TaskDTO approveTask(Long id) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");
        
        // 验证任务状态为待验收
        if (!"PENDING_REVIEW".equals(task.getStatus())) {
            throw new BusinessException("只有待验收状态的任务才能进行验收");
        }
        
        // 验证只有任务创建者才能验收
        Long currentUserId = SecurityUtils.getUserId();
        if (!currentUserId.equals(task.getCreatedBy())) {
            throw new BusinessException("只有任务创建者才能进行验收");
        }
        
        task.setStatus("COMPLETED");
        task.setReviewStatus("APPROVED");
        task.setReviewedAt(LocalDateTime.now());
        task.setReviewedBy(currentUserId);
        
        taskRepository.updateById(task);
        log.info("任务验收通过: taskId={}, reviewedBy={}", id, currentUserId);
        
        // 通知任务负责人验收通过
        sendTaskReviewNotification(task, "APPROVED", null);
        
        return toDTO(task);
    }
    
    /**
     * 验收任务（退回）
     */
    @Transactional
    public TaskDTO rejectTask(Long id, String comment) {
        Task task = taskRepository.getByIdOrThrow(id, "任务不存在");
        
        // 验证任务状态为待验收
        if (!"PENDING_REVIEW".equals(task.getStatus())) {
            throw new BusinessException("只有待验收状态的任务才能进行验收");
        }
        
        // 验证只有任务创建者才能验收
        Long currentUserId = SecurityUtils.getUserId();
        if (!currentUserId.equals(task.getCreatedBy())) {
            throw new BusinessException("只有任务创建者才能进行验收");
        }
        
        if (!StringUtils.hasText(comment)) {
            throw new BusinessException("退回时必须填写验收意见");
        }
        
        task.setStatus("IN_PROGRESS");
        task.setReviewStatus("REJECTED");
        task.setReviewComment(comment);
        task.setReviewedAt(LocalDateTime.now());
        task.setReviewedBy(currentUserId);
        task.setCompletedAt(null);
        task.setProgress(0);
        
        taskRepository.updateById(task);
        log.info("任务验收退回: taskId={}, reviewedBy={}, comment={}", id, currentUserId, comment);
        
        // 通知任务负责人验收退回
        sendTaskReviewNotification(task, "REJECTED", comment);
        
        return toDTO(task);
    }
    
    /**
     * 发送任务验收通知
     */
    private void sendTaskReviewNotification(Task task, String reviewResult, String comment) {
        try {
            if (task.getAssigneeId() == null) return;
            
            String currentUserName = SecurityUtils.getRealName();
            String title;
            String content;
            
            if ("APPROVED".equals(reviewResult)) {
                title = "任务验收通过";
                content = String.format("%s 已验收通过任务【%s】", currentUserName, task.getTitle());
            } else {
                title = "任务验收退回";
                content = String.format("%s 退回任务【%s】", currentUserName, task.getTitle());
                if (StringUtils.hasText(comment)) {
                    content += String.format("，退回意见：%s", comment);
                }
            }
            
            notificationAppService.sendSystemNotification(
                    task.getAssigneeId(),
                    title,
                    content,
                    "TASK",
                    task.getId()
            );
            log.info("任务验收通知已发送: taskId={}, assigneeId={}, result={}", 
                    task.getId(), task.getAssigneeId(), reviewResult);
        } catch (Exception e) {
            log.warn("发送任务验收通知失败: taskId={}", task.getId(), e);
        }
    }
    
    /**
     * 发送任务状态变更通知
     */
    private void sendTaskStatusNotification(Task task, String oldStatus, String newStatus) {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            String currentUserName = SecurityUtils.getRealName();
            
            // 只对有意义的状态变更发送通知
            if (oldStatus != null && oldStatus.equals(newStatus)) return;
            
            String statusName = getStatusName(newStatus);
            String title = String.format("任务【%s】状态变更", task.getTitle());
            String content = String.format("%s 将任务【%s】状态修改为：%s", 
                    currentUserName, task.getTitle(), statusName);
            
            // 通知任务负责人（如果不是自己操作的）
            if (task.getAssigneeId() != null && !task.getAssigneeId().equals(currentUserId)) {
                notificationAppService.sendSystemNotification(
                        task.getAssigneeId(),
                        title,
                        content,
                        "TASK",
                        task.getId()
                );
            }
            
            // 如果任务提交验收，通知任务创建者需要验收
            if ("PENDING_REVIEW".equals(newStatus) && task.getCreatedBy() != null) {
                if (!task.getCreatedBy().equals(currentUserId)) {
                    notificationAppService.sendSystemNotification(
                            task.getCreatedBy(),
                            "待验收任务",
                            String.format("%s 已完成任务【%s】，等待您的验收", task.getAssigneeName(), task.getTitle()),
                            "TASK",
                            task.getId()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("发送任务状态变更通知失败: taskId={}", task.getId(), e);
        }
    }
    
    private String getStatusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "TODO" -> "待处理";
            case "IN_PROGRESS" -> "进行中";
            case "PENDING_REVIEW" -> "待验收";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
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
            // 进度100%时，状态变为待验收
            task.setStatus("PENDING_REVIEW");
            task.setCompletedAt(LocalDateTime.now());
            task.setReviewStatus("PENDING_REVIEW");
        } else if (progress > 0) {
            task.setStatus("IN_PROGRESS");
            task.setReviewStatus(null);
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
        
        // 验收相关字段
        dto.setReviewStatus(task.getReviewStatus());
        dto.setReviewComment(task.getReviewComment());
        dto.setReviewedAt(task.getReviewedAt());
        dto.setReviewedBy(task.getReviewedBy());

        // 判断是否逾期（排除已完成、已取消、待验收状态）
        if (task.getDueDate() != null && !"COMPLETED".equals(task.getStatus())
                && !"CANCELLED".equals(task.getStatus())
                && !"PENDING_REVIEW".equals(task.getStatus())) {
            dto.setOverdue(task.getDueDate().isBefore(LocalDate.now()));
        } else {
            dto.setOverdue(false);
        }

        return dto;
    }
    
    /**
     * 验证任务操作权限
     * 只有以下用户可以操作任务：
     * 1. 系统管理员
     * 2. 任务创建者
     * 3. 任务负责人（被分配人）
     */
    private void validateTaskOperationPermission(Task task) {
        // 管理员拥有所有权限
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        
        // 检查是否是任务创建者
        if (currentUserId.equals(task.getCreatedBy())) {
            return;
        }
        
        // 检查是否是任务负责人
        if (currentUserId.equals(task.getAssigneeId())) {
            return;
        }
        
        throw new BusinessException("只有任务创建者或负责人才能执行此操作");
    }
    
    /**
     * 应用数据范围过滤
     * ALL: 可看全部任务
     * DEPT_AND_CHILD: 可看本部门及下级部门的项目任务，或自己创建/负责的任务
     * DEPT: 可看本部门的项目任务，或自己创建/负责的任务
     * SELF: 只能看自己创建或分配给自己的任务
     */
    private void applyDataScopeFilter(LambdaQueryWrapper<Task> wrapper) {
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        
        switch (dataScope) {
            case "ALL":
                // 可看全部任务，不加过滤条件
                break;
            case "DEPT_AND_CHILD":
                // 可看本部门及下级部门的项目任务，或自己创建/负责的任务
                if (deptId != null) {
                    List<Long> deptIds = getAllChildDepartmentIds(deptId);
                    deptIds.add(deptId);
                    // 查询符合条件的项目ID列表（使用SQL查询优化性能）
                    List<Long> matterIds = matterRepository.getBaseMapper().selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Matter>()
                                    .select(Matter::getId)
                                    .in(Matter::getDepartmentId, deptIds)
                                    .eq(Matter::getDeleted, false)
                    ).stream().map(Matter::getId).collect(Collectors.toList());
                    
                    wrapper.and(w -> {
                        if (!matterIds.isEmpty()) {
                            w.in(Task::getMatterId, matterIds)
                             .or()
                             .eq(Task::getCreatedBy, currentUserId)
                             .or()
                             .eq(Task::getAssigneeId, currentUserId);
                        } else {
                            w.eq(Task::getCreatedBy, currentUserId)
                             .or()
                             .eq(Task::getAssigneeId, currentUserId);
                        }
                    });
                } else {
                    // 没有部门，只能看自己创建或负责的任务
                    wrapper.and(w -> w
                        .eq(Task::getCreatedBy, currentUserId)
                        .or()
                        .eq(Task::getAssigneeId, currentUserId)
                    );
                }
                break;
            case "DEPT":
                // 仅本部门的项目任务，或自己创建/负责的任务
                if (deptId != null) {
                    // 查询本部门的项目ID列表（使用SQL查询优化性能）
                    List<Long> matterIds = matterRepository.getBaseMapper().selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Matter>()
                                    .select(Matter::getId)
                                    .eq(Matter::getDepartmentId, deptId)
                                    .eq(Matter::getDeleted, false)
                    ).stream().map(Matter::getId).collect(Collectors.toList());
                    
                    wrapper.and(w -> {
                        if (!matterIds.isEmpty()) {
                            w.in(Task::getMatterId, matterIds)
                             .or()
                             .eq(Task::getCreatedBy, currentUserId)
                             .or()
                             .eq(Task::getAssigneeId, currentUserId);
                        } else {
                            w.eq(Task::getCreatedBy, currentUserId)
                             .or()
                             .eq(Task::getAssigneeId, currentUserId);
                        }
                    });
                } else {
                    // 没有部门，只能看自己创建或负责的任务
                    wrapper.and(w -> w
                        .eq(Task::getCreatedBy, currentUserId)
                        .or()
                        .eq(Task::getAssigneeId, currentUserId)
                    );
                }
                break;
            default: // SELF
                // 只能看自己创建或分配给自己的任务
                wrapper.and(w -> w
                    .eq(Task::getCreatedBy, currentUserId)
                    .or()
                    .eq(Task::getAssigneeId, currentUserId)
                );
                break;
        }
    }
    
    /**
     * 获取所有下级部门ID
     */
    private List<Long> getAllChildDepartmentIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        var children = departmentRepository.findByParentId(parentId);
        for (var child : children) {
            result.add(child.getId());
            result.addAll(getAllChildDepartmentIds(child.getId()));
        }
        return result;
    }
}
