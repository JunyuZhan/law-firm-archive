package com.archivesystem.repository;

import com.archivesystem.entity.DeadLetterRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 死信消息记录 Mapper.
 * @author junyuzhan
 */
@Mapper
public interface DeadLetterRecordMapper extends BaseMapper<DeadLetterRecord> {

    /**
     * 统计各状态的消息数量.
     */
    @Select("SELECT status, COUNT(*) as count FROM arc_dead_letter_record GROUP BY status")
    List<StatusCount> countByStatus();

    /**
     * 查询待处理的消息（可重试）.
     */
    @Select("SELECT * FROM arc_dead_letter_record " +
            "WHERE status IN ('PENDING', 'FAILED') " +
            "AND retry_count < max_retries " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<DeadLetterRecord> findRetryableMessages(@Param("limit") int limit);

    /**
     * 更新状态.
     */
    @Update("UPDATE arc_dead_letter_record SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 状态统计结果.
     */
    interface StatusCount {
        String getStatus();
        Integer getCount();
    }
}
