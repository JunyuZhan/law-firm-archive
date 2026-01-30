package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.AssetInventoryDetail;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 资产盘点明细Mapper（M8-033） */
@Mapper
public interface AssetInventoryDetailMapper extends BaseMapper<AssetInventoryDetail> {

  /**
   * 查询盘点的所有明细.
   *
   * @param inventoryId 盘点ID
   * @return 盘点明细列表
   */
  @Select("SELECT * FROM asset_inventory_detail WHERE inventory_id = #{inventoryId} ORDER BY id")
  List<AssetInventoryDetail> selectByInventoryId(@Param("inventoryId") Long inventoryId);

  /**
   * 查询有差异的明细.
   *
   * @param inventoryId 盘点ID
   * @return 有差异的盘点明细列表
   */
  @Select(
      "SELECT * FROM asset_inventory_detail "
          + "WHERE inventory_id = #{inventoryId} "
          + "AND discrepancy_type != 'NORMAL' "
          + "ORDER BY discrepancy_type, id")
  List<AssetInventoryDetail> selectDiscrepancies(@Param("inventoryId") Long inventoryId);
}
