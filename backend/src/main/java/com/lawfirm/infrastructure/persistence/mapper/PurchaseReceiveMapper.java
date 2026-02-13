package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.PurchaseReceive;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 采购入库Mapper */
@Mapper
public interface PurchaseReceiveMapper extends BaseMapper<PurchaseReceive> {

  /**
   * 根据采购申请ID查询入库记录.
   *
   * @param requestId 采购申请ID
   * @return 采购入库记录列表
   */
  @Select(
      "SELECT * FROM admin_purchase_receive WHERE deleted = false "
          + "AND request_id = #{requestId} ORDER BY receive_date DESC")
  List<PurchaseReceive> findByRequestId(@Param("requestId") Long requestId);

  /**
   * 根据采购明细ID查询入库记录.
   *
   * @param itemId 采购明细ID
   * @return 采购入库记录列表
   */
  @Select(
      "SELECT * FROM admin_purchase_receive WHERE deleted = false AND item_id = #{itemId} ORDER BY receive_date DESC")
  List<PurchaseReceive> findByItemId(@Param("itemId") Long itemId);

  /**
   * 统计明细已入库数量.
   *
   * @param itemId 采购明细ID
   * @return 已入库数量
   */
  @Select(
      "SELECT COALESCE(SUM(quantity), 0) FROM admin_purchase_receive WHERE deleted = false AND item_id = #{itemId}")
  Integer sumQuantityByItemId(@Param("itemId") Long itemId);
}
