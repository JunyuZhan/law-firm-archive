package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.AssetRecord;
import com.lawfirm.infrastructure.persistence.mapper.AssetRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资产操作记录仓储
 */
@Repository
public class AssetRecordRepository extends AbstractRepository<AssetRecordMapper, AssetRecord> {

    /**
     * 查询资产的操作记录
     */
    public List<AssetRecord> findByAssetId(Long assetId) {
        return baseMapper.selectByAssetId(assetId);
    }

    /**
     * 查询用户的领用记录
     */
    public List<AssetRecord> findReceiveRecordsByUserId(Long userId) {
        return baseMapper.selectReceiveRecordsByUserId(userId);
    }

    /**
     * 查询待审批的记录
     */
    public List<AssetRecord> findPendingApproval() {
        return baseMapper.selectPendingApproval();
    }

    /**
     * 查询未归还的领用记录
     */
    public List<AssetRecord> findUnreturnedRecords() {
        return baseMapper.selectUnreturnedRecords();
    }
}
