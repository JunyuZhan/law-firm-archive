package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.AssetRecord;
import com.lawfirm.infrastructure.persistence.mapper.AssetRecordMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 资产操作记录仓储 */
@Repository
public class AssetRecordRepository extends AbstractRepository<AssetRecordMapper, AssetRecord> {

  /**
   * 查询资产的操作记录
   *
   * @param assetId 资产ID
   * @return 操作记录列表
   */
  public List<AssetRecord> findByAssetId(final Long assetId) {
    return baseMapper.selectByAssetId(assetId);
  }

  /**
   * 查询用户的领用记录
   *
   * @param userId 用户ID
   * @return 领用记录列表
   */
  public List<AssetRecord> findReceiveRecordsByUserId(final Long userId) {
    return baseMapper.selectReceiveRecordsByUserId(userId);
  }

  /**
   * 查询待审批的记录
   *
   * @return 待审批记录列表
   */
  public List<AssetRecord> findPendingApproval() {
    return baseMapper.selectPendingApproval();
  }

  /**
   * 查询未归还的领用记录
   *
   * @return 未归还记录列表
   */
  public List<AssetRecord> findUnreturnedRecords() {
    return baseMapper.selectUnreturnedRecords();
  }

  /**
   * 统计资产的操作记录数量 问题294修复：用于删除前检查历史记录
   *
   * @param assetId 资产ID
   * @return 操作记录数量
   */
  public long countByAssetId(final Long assetId) {
    return baseMapper.countByAssetId(assetId);
  }
}
