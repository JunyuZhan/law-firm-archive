package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.archive.entity.Archive;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 档案 Mapper */
@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {

  /**
   * 查询待归档的案件列表（已结案但未归档）.
   *
   * @return 待归档案件列表
   */
  @Select(
      """
        SELECT m.id, m.matter_no, m.name as matter_name, m.client_id,
               c.name as client_name, m.lead_lawyer_id, u.real_name as main_lawyer_name,
               m.actual_end_date as case_close_date
        FROM matter m
        LEFT JOIN crm_client c ON m.client_id = c.id
        LEFT JOIN sys_user u ON m.lead_lawyer_id = u.id
        LEFT JOIN archive a ON m.id = a.matter_id
        WHERE m.status = 'CLOSED' AND a.id IS NULL
        ORDER BY m.actual_end_date DESC
        """)
  List<Object> selectPendingArchives();

  /**
   * 查询档案的借阅记录.
   *
   * @param archiveId 档案ID
   * @return 档案借阅记录列表
   */
  @Select(
      "SELECT * FROM archive_borrow WHERE archive_id = #{archiveId} AND deleted = false ORDER BY created_at DESC")
  List<com.lawfirm.domain.archive.entity.ArchiveBorrow> selectBorrowsByArchiveId(
      @Param("archiveId") Long archiveId);

  /**
   * 按库位查询档案（M7-022）.
   *
   * @param locationId 库位ID
   * @return 档案列表
   */
  @Select(
      "SELECT * FROM archive WHERE location_id = #{locationId} "
          + "AND deleted = false AND status = 'STORED' "
          + "ORDER BY box_no, archive_no")
  List<Archive> selectByLocationId(@Param("locationId") Long locationId);

  /**
   * 查询即将到期的档案（M7-041）.
   *
   * @param deadline 截止日期
   * @return 即将到期的档案列表
   */
  @Select(
      "SELECT * FROM archive "
          + "WHERE retention_expire_date IS NOT NULL "
          + "AND retention_expire_date <= #{deadline} "
          + "AND retention_expire_date > CURRENT_DATE "
          + "AND status != 'DESTROYED' "
          + "AND deleted = false "
          + "ORDER BY retention_expire_date ASC")
  List<Archive> selectExpiringArchives(@Param("deadline") java.time.LocalDate deadline);
}
