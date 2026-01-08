package com.lawfirm.domain.document.repository;

import com.lawfirm.domain.document.entity.DossierTemplateItem;
import com.lawfirm.infrastructure.persistence.mapper.DossierTemplateItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 卷宗目录项仓储
 */
@Repository
@RequiredArgsConstructor
public class DossierTemplateItemRepository {

    private final DossierTemplateItemMapper mapper;

    /**
     * 根据模板ID查询目录项
     */
    public List<DossierTemplateItem> findByTemplateId(Long templateId) {
        return mapper.selectByTemplateId(templateId);
    }

    /**
     * 根据父ID查询子目录项
     */
    public List<DossierTemplateItem> findByParentId(Long templateId, Long parentId) {
        return mapper.selectByParentId(templateId, parentId);
    }
}

