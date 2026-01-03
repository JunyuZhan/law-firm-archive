package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.AssetInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产盘点Mapper（M8-033）
 */
@Mapper
public interface AssetInventoryMapper extends BaseMapper<AssetInventory> {

    /**
     * 根据盘点编号查询
     */
    @Select("SELECT * FROM asset_inventory WHERE inventory_no = #{inventoryNo} AND deleted = false")
    AssetInventory selectByInventoryNo(@Param("inventoryNo") String inventoryNo);

    /**
     * 查询进行中的盘点
     */
    @Select("SELECT * FROM asset_inventory WHERE status = 'IN_PROGRESS' AND deleted = false ORDER BY inventory_date DESC")
    List<AssetInventory> selectInProgress();
}

