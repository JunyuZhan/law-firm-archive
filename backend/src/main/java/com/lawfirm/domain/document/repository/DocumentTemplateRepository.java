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
     * 根据模板类型和案件类型查找模板
     * 优先匹配特定案件类型，如果没有则回退到通用模板(caseType=ALL)
     * 
     * @param templateType 模板类型
     * @param caseType 案件类型（如 CIVIL, CRIMINAL, ADMINISTRATIVE 等）
     * @return 匹配的模板，未找到返回null
     */
    public DocumentTemplate findByTemplateTypeAndCaseType(String templateType, String caseType) {
        // 1. 优先查找特定案件类型的模板
        if (caseType != null && !caseType.isEmpty() && !"ALL".equals(caseType)) {
            LambdaQueryWrapper<DocumentTemplate> specificQuery = new LambdaQueryWrapper<>();
            specificQuery.eq(DocumentTemplate::getTemplateType, templateType)
                        .eq(DocumentTemplate::getCaseType, caseType)
                        .eq(DocumentTemplate::getStatus, "ACTIVE")
                        .orderByDesc(DocumentTemplate::getUpdatedAt)
                        .last("LIMIT 1");
            DocumentTemplate specific = baseMapper.selectOne(specificQuery);
            if (specific != null) {
                return specific;
            }
        }
        
        // 2. 回退到通用模板
        LambdaQueryWrapper<DocumentTemplate> generalQuery = new LambdaQueryWrapper<>();
        generalQuery.eq(DocumentTemplate::getTemplateType, templateType)
                    .eq(DocumentTemplate::getCaseType, "ALL")
                    .eq(DocumentTemplate::getStatus, "ACTIVE")
                    .orderByDesc(DocumentTemplate::getUpdatedAt)
                    .last("LIMIT 1");
        DocumentTemplate general = baseMapper.selectOne(generalQuery);
        if (general != null) {
            return general;
        }
        
        // 3. 最后回退到任意模板（兼容旧数据，caseType可能为null）
        return findFirstByTemplateType(templateType);
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
