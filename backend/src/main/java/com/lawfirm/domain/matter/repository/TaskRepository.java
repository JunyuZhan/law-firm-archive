package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务仓储
 */
@Repository
public class TaskRepository extends AbstractRepository<TaskMapper, Task> {

    /**
     * 查询我的待办任务
     */
    public List<Task> findMyTodoTasks(Long userId) {
        return baseMapper.selectMyTodoTasks(userId);
    }

    /**
     * 查询即将到期的任务
     */
    public List<Task> findUpcomingTasks(LocalDate today, LocalDate deadline) {
        return baseMapper.selectUpcomingTasks(today, deadline);
    }

    /**
     * 查询逾期任务
     */
    public List<Task> findOverdueTasks(LocalDate today) {
        return baseMapper.selectOverdueTasks(today);
    }

    /**
     * 统计案件任务数
     */
    public int countByMatter(Long matterId) {
        return baseMapper.countByMatter(matterId);
    }

    /**
     * 统计案件已完成任务数
     */
    public int countCompletedByMatter(Long matterId) {
        return baseMapper.countCompletedByMatter(matterId);
    }

    /**
     * 统计用户待办任务数
     */
    public int countPendingByAssigneeId(Long userId) {
        return baseMapper.countPendingByAssigneeId(userId);
    }

    /**
     * 统计用户逾期任务数
     */
    public int countOverdueByAssigneeId(Long userId) {
        return baseMapper.countOverdueByAssigneeId(userId);
    }

    /**
     * 查询用户待办任务（限制数量）
     */
    public List<Task> findPendingByAssigneeId(Long userId, int limit) {
        return baseMapper.selectPendingByAssigneeId(userId, limit);
    }
}
