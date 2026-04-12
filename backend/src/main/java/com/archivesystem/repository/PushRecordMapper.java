package com.archivesystem.repository;

import com.archivesystem.entity.PushRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 推送记录Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {

    /**
     * 根据来源类型和来源ID查询
     */
    @Select("SELECT * FROM arc_push_record WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND deleted = false ORDER BY pushed_at DESC LIMIT 1")
    PushRecord selectBySourceTypeAndId(@Param("sourceType") String sourceType, @Param("sourceId") String sourceId);

    /**
     * 查询待处理的推送记录
     */
    @Select("SELECT * FROM arc_push_record WHERE push_status = 'PENDING' AND deleted = false ORDER BY pushed_at ASC")
    List<PushRecord> selectPendingRecords();

    /**
     * 查询失败的推送记录（可重试）
     */
    @Select("SELECT * FROM arc_push_record WHERE push_status = 'FAILED' AND deleted = false ORDER BY pushed_at DESC")
    List<PushRecord> selectFailedRecords();

    /**
     * 更新推送状态
     */
    @Update("UPDATE arc_push_record SET push_status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 统计各状态数量
     */
    @Select("SELECT push_status, COUNT(*) as count FROM arc_push_record WHERE deleted = false GROUP BY push_status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 统计今日推送数
     */
    @Select("SELECT COUNT(*) FROM arc_push_record WHERE DATE(pushed_at) = CURRENT_DATE AND deleted = false")
    int countToday();
}
