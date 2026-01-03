package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.infrastructure.persistence.mapper.AssetMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 资产仓储
 */
@Repository
public class AssetRepository extends AbstractRepository<AssetMapper, Asset> {

    /**
     * 分页查询资产
     */
    public IPage<Asset> findPage(Page<Asset> page, String keyword, String category, 
                                  String status, Long departmentId, Long currentUserId) {
        return baseMapper.selectAssetPage(page, keyword, category, status, departmentId, currentUserId);
    }

    /**
     * 根据资产编号查询
     */
    public Asset findByAssetNo(String assetNo) {
        return baseMapper.selectByAssetNo(assetNo);
    }

    /**
     * 查询闲置资产
     */
    public List<Asset> findIdleAssets() {
        return baseMapper.selectIdleAssets();
    }

    /**
     * 查询用户领用的资产
     */
    public List<Asset> findByCurrentUserId(Long userId) {
        return baseMapper.selectByCurrentUserId(userId);
    }

    /**
     * 统计各状态资产数量
     */
    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus();
    }

    /**
     * 统计各分类资产数量
     */
    public List<Map<String, Object>> countByCategory() {
        return baseMapper.countByCategory();
    }
}
