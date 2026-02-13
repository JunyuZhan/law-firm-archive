package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.PurchaseItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 采购明细Mapper */
@Mapper
public interface PurchaseItemMapper extends BaseMapper<PurchaseItem> {

  /**
   * 根据采购申请ID查询明细.
   *
   * @param requestId 采购申请ID
   * @return 采购明细列表
   */
  @Select(
      "SELECT * FROM admin_purchase_item WHERE deleted = false AND request_id = #{requestId} ORDER BY id ASC")
  List<PurchaseItem> findByRequestId(@Param("requestId") Long requestId);

  /**
   * 删除采购申请的所有明细.
   *
   * @param requestId 采购申请ID
   */
  @Select("UPDATE admin_purchase_item SET deleted = true WHERE request_id = #{requestId}")
  void deleteByRequestId(@Param("requestId") Long requestId);
}
