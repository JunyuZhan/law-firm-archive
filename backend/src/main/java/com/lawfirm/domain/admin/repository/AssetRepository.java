package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.infrastructure.persistence.mapper.AssetMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** 资产仓储 */
@Repository
public class AssetRepository extends AbstractRepository<AssetMapper, Asset> {

  /**
   * 分页查询资产
   *
   * @param page 分页对象
   * @param keyword 关键词
   * @param category 分类
   * @param status 状态
   * @param departmentId 部门ID
   * @param currentUserId 当前用户ID
   * @return 资产分页结果
   */
  public IPage<Asset> findPage(
      final Page<Asset> page,
      final String keyword,
      final String category,
      final String status,
      final Long departmentId,
      final Long currentUserId) {
    return baseMapper.selectAssetPage(page, keyword, category, status, departmentId, currentUserId);
  }

  /**
   * 根据资产编号查询
   *
   * @param assetNo 资产编号
   * @return 资产
   */
  public Asset findByAssetNo(final String assetNo) {
    return baseMapper.selectByAssetNo(assetNo);
  }

  /**
   * 查询闲置资产
   *
   * @return 闲置资产列表
   */
  public List<Asset> findIdleAssets() {
    return baseMapper.selectIdleAssets();
  }

  /**
   * 查询用户领用的资产
   *
   * @param userId 用户ID
   * @return 资产列表
   */
  public List<Asset> findByCurrentUserId(final Long userId) {
    return baseMapper.selectByCurrentUserId(userId);
  }

  /**
   * 统计各状态资产数量
   *
   * @return 各状态资产数量统计列表
   */
  public List<Map<String, Object>> countByStatus() {
    return baseMapper.countByStatus();
  }

  /**
   * 统计各分类资产数量
   *
   * @return 各分类资产数量统计列表
   */
  public List<Map<String, Object>> countByCategory() {
    return baseMapper.countByCategory();
  }
}
