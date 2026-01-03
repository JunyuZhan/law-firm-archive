package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务评论 Mapper
 */
@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {

    /**
     * 根据任务ID查询所有评论
     */
    @Select("SELECT * FROM task_comment WHERE task_id = #{taskId} ORDER BY created_at ASC")
    List<TaskComment> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 统计任务的评论数
     */
    @Select("SELECT COUNT(*) FROM task_comment WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") Long taskId);
}

