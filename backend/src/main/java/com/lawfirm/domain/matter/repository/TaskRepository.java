package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 任务仓储。
 *
 * <p>提供任务数据的持久化操作。
 */
@Repository
public class TaskRepository extends AbstractRepository<TaskMapper, Task> {

  /**
   * 查询我的待办任务。
   *
   * @param userId 用户ID
   * @return 待办任务列表
   */
  public List<Task> findMyTodoTasks(final Long userId) {
    return baseMapper.selectMyTodoTasks(userId);
  }

  /**
   * 查询即将到期的任务。
   *
   * @param today 今天日期
   * @param deadline 截止日期
   * @return 即将到期的任务列表
   */
  public List<Task> findUpcomingTasks(final LocalDate today, final LocalDate deadline) {
    return baseMapper.selectUpcomingTasks(today, deadline);
  }

  /**
   * 查询逾期任务。
   *
   * @param today 今天日期
   * @return 逾期任务列表
   */
  public List<Task> findOverdueTasks(final LocalDate today) {
    return baseMapper.selectOverdueTasks(today);
  }

  /**
   * 统计案件任务数。
   *
   * @param matterId 案件ID
   * @return 任务数量
   */
  public int countByMatter(final Long matterId) {
    return baseMapper.countByMatter(matterId);
  }

  /**
   * 统计案件已完成任务数。
   *
   * @param matterId 案件ID
   * @return 已完成任务数量
   */
  public int countCompletedByMatter(final Long matterId) {
    return baseMapper.countCompletedByMatter(matterId);
  }

  /**
   * 统计用户待办任务数。
   *
   * @param userId 用户ID
   * @return 待办任务数量
   */
  public int countPendingByAssigneeId(final Long userId) {
    return baseMapper.countPendingByAssigneeId(userId);
  }

  /**
   * 统计用户逾期任务数。
   *
   * @param userId 用户ID
   * @return 逾期任务数量
   */
  public int countOverdueByAssigneeId(final Long userId) {
    return baseMapper.countOverdueByAssigneeId(userId);
  }

  /**
   * 查询用户待办任务（限制数量）。
   *
   * @param userId 用户ID
   * @param limit 查询数量限制
   * @return 待办任务列表
   */
  public List<Task> findPendingByAssigneeId(final Long userId, final int limit) {
    return baseMapper.selectPendingByAssigneeId(userId, limit);
  }
}
