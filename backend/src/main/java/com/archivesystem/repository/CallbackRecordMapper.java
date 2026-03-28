package com.archivesystem.repository;

import com.archivesystem.entity.CallbackRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 回调记录Mapper接口.
 */
@Mapper
public interface CallbackRecordMapper extends BaseMapper<CallbackRecord> {

    /**
     * 根据档案ID查询回调记录
     */
    @Select("SELECT * FROM arc_callback_record WHERE archive_id = #{archiveId} AND deleted = false ORDER BY callback_at DESC")
    List<CallbackRecord> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据推送记录ID查询回调记录
     */
    @Select("SELECT * FROM arc_callback_record WHERE push_record_id = #{pushRecordId} AND deleted = false ORDER BY callback_at DESC")
    List<CallbackRecord> selectByPushRecordId(@Param("pushRecordId") Long pushRecordId);

    /**
     * 查询待重试的回调记录
     */
    @Select("SELECT * FROM arc_callback_record WHERE callback_status = 'FAILED' AND retry_count < max_retries AND next_retry_at <= CURRENT_TIMESTAMP AND deleted = false ORDER BY next_retry_at ASC")
    List<CallbackRecord> selectPendingRetries();

    /**
     * 查询失败的回调记录
     */
    @Select("SELECT * FROM arc_callback_record WHERE callback_status = 'FAILED' AND deleted = false ORDER BY callback_at DESC")
    List<CallbackRecord> selectFailedRecords();

    /**
     * 更新回调状态
     */
    @Update("UPDATE arc_callback_record SET callback_status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 增加重试次数
     */
    @Update("UPDATE arc_callback_record SET retry_count = retry_count + 1, next_retry_at = #{nextRetryAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int incrementRetryCount(@Param("id") Long id, @Param("nextRetryAt") java.time.LocalDateTime nextRetryAt);

    /**
     * 统计失败回调数
     */
    @Select("SELECT COUNT(*) FROM arc_callback_record WHERE callback_status = 'FAILED' AND deleted = false")
    int countFailed();

    /**
     * 统计今日回调数
     */
    @Select("SELECT COUNT(*) FROM arc_callback_record WHERE DATE(callback_at) = CURRENT_DATE AND deleted = false")
    int countToday();
}
