package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 客户联系记录 Mapper */
@Mapper
public interface ClientContactRecordMapper extends BaseMapper<ClientContactRecord> {

  /**
   * 根据客户ID分页查询联系记录.
   *
   * @param page 分页对象
   * @param clientId 客户ID
   * @return 联系记录分页结果
   */
  @Select(
      "SELECT * FROM crm_client_contact_record WHERE client_id = #{clientId} "
          + "AND deleted = false ORDER BY contact_date DESC")
  IPage<ClientContactRecord> selectByClientId(
      Page<ClientContactRecord> page, @Param("clientId") Long clientId);

  /**
   * 根据客户ID查询所有联系记录.
   *
   * @param clientId 客户ID
   * @return 联系记录列表
   */
  @Select(
      "SELECT * FROM crm_client_contact_record WHERE client_id = #{clientId} "
          + "AND deleted = false ORDER BY contact_date DESC")
  List<ClientContactRecord> selectAllByClientId(@Param("clientId") Long clientId);

  /**
   * 查询需要跟进的联系记录.
   *
   * @param date 日期
   * @return 联系记录列表
   */
  @Select(
      "SELECT * FROM crm_client_contact_record "
          + "WHERE next_follow_up_date IS NOT NULL "
          + "AND next_follow_up_date <= #{date} "
          + "AND follow_up_reminder = true "
          + "AND deleted = false "
          + "ORDER BY next_follow_up_date ASC")
  List<ClientContactRecord> selectFollowUpRecords(@Param("date") LocalDate date);

  /**
   * 复杂查询联系记录（支持多条件筛选）.
   *
   * @param page 分页对象
   * @param clientId 客户ID
   * @param contactId 联系人ID
   * @param contactMethod 联系方式
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param followUpReminder 跟进提醒
   * @return 联系记录分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM crm_client_contact_record "
          + "WHERE deleted = false "
          + "<if test='clientId != null'> AND client_id = #{clientId} </if>"
          + "<if test='contactId != null'> AND contact_id = #{contactId} </if>"
          + "<if test='contactMethod != null and contactMethod != \"\"'> AND contact_method = #{contactMethod} </if>"
          + "<if test='startDate != null'> AND contact_date::date &gt;= #{startDate} </if>"
          + "<if test='endDate != null'> AND contact_date::date &lt;= #{endDate} </if>"
          + "<if test='followUpReminder != null'> AND follow_up_reminder = #{followUpReminder} </if>"
          + "ORDER BY contact_date DESC"
          + "</script>")
  IPage<ClientContactRecord> selectByConditions(
      Page<ClientContactRecord> page,
      @Param("clientId") Long clientId,
      @Param("contactId") Long contactId,
      @Param("contactMethod") String contactMethod,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("followUpReminder") Boolean followUpReminder);
}
