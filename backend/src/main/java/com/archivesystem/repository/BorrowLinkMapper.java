package com.archivesystem.repository;

import com.archivesystem.entity.BorrowLink;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 电子借阅链接Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface BorrowLinkMapper extends BaseMapper<BorrowLink> {

    /**
     * 根据访问令牌查询.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE access_token = #{accessToken}")
    BorrowLink selectByAccessToken(@Param("accessToken") String accessToken);

    /**
     * 根据档案ID查询链接列表.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE archive_id = #{archiveId} ORDER BY created_at DESC")
    List<BorrowLink> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 根据借阅申请ID查询链接.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE borrow_id = #{borrowId}")
    BorrowLink selectByBorrowId(@Param("borrowId") Long borrowId);

    /**
     * 查询有效链接列表.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE status = 'ACTIVE' ORDER BY created_at DESC")
    List<BorrowLink> selectActiveList();

    /**
     * 查询已过期但状态未更新的链接.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE status = 'ACTIVE' AND expire_at < NOW()")
    List<BorrowLink> selectExpiredLinks();

    /**
     * 批量更新过期链接状态.
     */
    @Update("UPDATE arc_borrow_link SET status = 'EXPIRED', updated_at = NOW() WHERE status = 'ACTIVE' AND expire_at < NOW()")
    int updateExpiredStatus();

    /**
     * 根据来源系统用户ID查询链接列表.
     */
    @Select("SELECT * FROM arc_borrow_link WHERE source_user_id = #{sourceUserId} AND source_type = #{sourceType} ORDER BY created_at DESC")
    List<BorrowLink> selectBySourceUser(@Param("sourceUserId") String sourceUserId, @Param("sourceType") String sourceType);

    /**
     * 聚合统计链接总量、状态分布以及访问下载次数.
     */
    @Select("""
            SELECT COUNT(*) AS totalCount,
                   SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS activeCount,
                   SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) AS expiredCount,
                   SUM(CASE WHEN status = 'REVOKED' THEN 1 ELSE 0 END) AS revokedCount,
                   COALESCE(SUM(access_count), 0) AS totalAccessCount,
                   COALESCE(SUM(download_count), 0) AS totalDownloadCount
            FROM arc_borrow_link
            """)
    Map<String, Object> selectAggregateStats();
}
