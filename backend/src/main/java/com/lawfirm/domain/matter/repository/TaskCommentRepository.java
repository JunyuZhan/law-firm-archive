package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.TaskComment;
import com.lawfirm.infrastructure.persistence.mapper.TaskCommentMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务评论仓储
 */
@Repository
public class TaskCommentRepository extends AbstractRepository<TaskCommentMapper, TaskComment> {

    /**
     * 根据任务ID查询所有评论
     */
    public List<TaskComment> findByTaskId(Long taskId) {
        return baseMapper.selectByTaskId(taskId);
    }

    /**
     * 统计任务的评论数
     */
    public int countByTaskId(Long taskId) {
        return baseMapper.countByTaskId(taskId);
    }
}

