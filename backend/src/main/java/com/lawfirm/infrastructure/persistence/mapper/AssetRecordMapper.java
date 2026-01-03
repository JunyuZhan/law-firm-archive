package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.AssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产操作记录 Mapper
 */
@Mapper
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {

    /**
     * 查询资产的操作记录
     */
    @Select("SELECT * FROM admin_asset_record WHERE asset_id = #{assetId} AND deleted = false ORDER BY operate_date DESC, created_at DESC")
    List<AssetRecord> selectByAssetId(@Param("assetId") Long assetId);

    /**
     * 查询用户的领用记录
     */
    @Select("SELECT * FROM admin_asset_record WHERE to_user_id = #{userId} AND record_type = 'RECEIVE' AND deleted = false ORDER BY operate_date DESC")
    List<AssetRecord> selectReceiveRecordsByUserId(@Param("userId") Long userId);

    /**
     * 查询待审批的记录
     */
    @Select("SELECT * FROM admin_asset_record WHERE approval_status = 'PENDING' AND deleted = false ORDER BY created_at")
    List<AssetRecord> selectPendingApproval();

    /**
     * 查询未归还的领用记录
     */
    @Select("SELECT * FROM admin_asset_record WHERE record_type = 'RECEIVE' AND actual_return_date IS NULL AND deleted = false ORDER BY expected_return_date")
    List<AssetRecord> selectUnreturnedRecords();
}
