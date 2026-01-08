package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import com.lawfirm.infrastructure.persistence.mapper.MatterDossierItemMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 项目卷宗目录仓储
 */
@Repository
public class MatterDossierItemRepository extends AbstractRepository<MatterDossierItemMapper, MatterDossierItem> {

    /**
     * 根据项目ID查询目录项
     */
    public List<MatterDossierItem> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }

    /**
     * 根据父ID查询子目录项
     */
    public List<MatterDossierItem> findByParentId(Long matterId, Long parentId) {
        return baseMapper.selectByParentId(matterId, parentId);
    }

    /**
     * 更新目录项文件数量
     */
    public void updateDocumentCount(Long itemId, Integer count) {
        baseMapper.updateDocumentCount(itemId, count);
    }

    /**
     * 检查项目是否已初始化卷宗目录
     */
    public boolean hasDossierItems(Long matterId) {
        return baseMapper.countByMatterId(matterId) > 0;
    }
}

