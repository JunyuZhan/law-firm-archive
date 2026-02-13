package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.AssetInventory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 资产盘点Mapper（M8-033） */
@Mapper
public interface AssetInventoryMapper extends BaseMapper<AssetInventory> {

  /**
   * 根据盘点编号查询.
   *
   * @param inventoryNo 盘点编号
   * @return 资产盘点记录
   */
  @Select("SELECT * FROM asset_inventory WHERE inventory_no = #{inventoryNo} AND deleted = false")
  AssetInventory selectByInventoryNo(@Param("inventoryNo") String inventoryNo);

  /**
   * 查询进行中的盘点.
   *
   * @return 进行中的盘点列表
   */
  @Select(
      "SELECT * FROM asset_inventory WHERE status = 'IN_PROGRESS' AND deleted = false ORDER BY inventory_date DESC")
  List<AssetInventory> selectInProgress();
}
