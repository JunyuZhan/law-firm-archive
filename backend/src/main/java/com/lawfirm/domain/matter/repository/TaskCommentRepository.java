package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.TaskComment;
import com.lawfirm.infrastructure.persistence.mapper.TaskCommentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 任务评论仓储。
 *
 * <p>提供任务评论数据的持久化操作。
 */
@Repository
public class TaskCommentRepository extends AbstractRepository<TaskCommentMapper, TaskComment> {

  /**
   * 根据任务ID查询所有评论。
   *
   * @param taskId 任务ID
   * @return 评论列表
   */
  public List<TaskComment> findByTaskId(final Long taskId) {
    return baseMapper.selectByTaskId(taskId);
  }

  /**
   * 统计任务的评论数。
   *
   * @param taskId 任务ID
   * @return 评论数量
   */
  public int countByTaskId(final Long taskId) {
    return baseMapper.countByTaskId(taskId);
  }
}
