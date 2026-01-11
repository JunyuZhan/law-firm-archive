package com.lawfirm.domain.document.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.infrastructure.persistence.mapper.DocumentTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档模板仓储
 */
@Repository
public class DocumentTemplateRepository extends AbstractRepository<DocumentTemplateMapper, DocumentTemplate> {

    /**
     * 增加使用次数
     */
    public void incrementUseCount(Long id) {
        baseMapper.incrementUseCount(id);
    }

    /**
     * 根据模板类型查找启用的模板
     * @param templateType 模板类型
     * @return 模板列表
     */
    public List<DocumentTemplate> findByTemplateType(String templateType) {
        LambdaQueryWrapper<DocumentTemplate> query = new LambdaQueryWrapper<>();
        query.eq(DocumentTemplate::getTemplateType, templateType)
             .eq(DocumentTemplate::getStatus, "ACTIVE")
             .orderByDesc(DocumentTemplate::getUpdatedAt);
        return baseMapper.selectList(query);
    }

    /**
     * 根据模板类型查找第一个启用的模板
     * @param templateType 模板类型
     * @return 模板，未找到返回null
     */
    public DocumentTemplate findFirstByTemplateType(String templateType) {
        List<DocumentTemplate> templates = findByTemplateType(templateType);
        return templates.isEmpty() ? null : templates.get(0);
    }

    /**
     * 根据模板编号查找模板
     * @param templateNo 模板编号
     * @return 模板
     */
    public DocumentTemplate findByTemplateNo(String templateNo) {
        LambdaQueryWrapper<DocumentTemplate> query = new LambdaQueryWrapper<>();
        query.eq(DocumentTemplate::getTemplateNo, templateNo)
             .eq(DocumentTemplate::getStatus, "ACTIVE");
        return baseMapper.selectOne(query);
    }
}
