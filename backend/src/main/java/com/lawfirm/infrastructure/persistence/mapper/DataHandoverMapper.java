package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.DataHandover;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 数据交接记录Mapper */
@Mapper
public interface DataHandoverMapper extends BaseMapper<DataHandover> {

  /**
   * 分页查询交接记录.
   *
   * @param page 分页对象
   * @param fromUserId 移交人ID
   * @param toUserId 接收人ID
   * @param handoverType 交接类型
   * @param status 状态
   * @return 交接记录分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM sys_data_handover WHERE deleted = false "
          + "<if test='fromUserId != null'> AND from_user_id = #{fromUserId} </if>"
          + "<if test='toUserId != null'> AND to_user_id = #{toUserId} </if>"
          + "<if test='handoverType != null and handoverType != \"\"'> AND handover_type = #{handoverType} </if>"
          + "<if test='status != null and status != \"\"'> AND status = #{status} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<DataHandover> selectHandoverPage(
      Page<DataHandover> page,
      @Param("fromUserId") Long fromUserId,
      @Param("toUserId") Long toUserId,
      @Param("handoverType") String handoverType,
      @Param("status") String status);

  /**
   * 根据移交人ID查询.
   *
   * @param fromUserId 移交人ID
   * @return 交接记录列表
   */
  @Select(
      "SELECT * FROM sys_data_handover WHERE from_user_id = #{fromUserId} AND deleted = false ORDER BY created_at DESC")
  List<DataHandover> selectByFromUserId(@Param("fromUserId") Long fromUserId);

  /**
   * 根据接收人ID查询.
   *
   * @param toUserId 接收人ID
   * @return 交接记录列表
   */
  @Select(
      "SELECT * FROM sys_data_handover WHERE to_user_id = #{toUserId} AND deleted = false ORDER BY created_at DESC")
  List<DataHandover> selectByToUserId(@Param("toUserId") Long toUserId);

  /**
   * 根据交接单号查询.
   *
   * @param handoverNo 交接单号
   * @return 交接记录
   */
  @Select("SELECT * FROM sys_data_handover WHERE handover_no = #{handoverNo} AND deleted = false")
  DataHandover selectByHandoverNo(@Param("handoverNo") String handoverNo);
}
