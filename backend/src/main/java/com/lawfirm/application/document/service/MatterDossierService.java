package com.lawfirm.application.document.service;

import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.domain.document.entity.DossierTemplateItem;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import com.lawfirm.domain.document.repository.DossierTemplateItemRepository;
import com.lawfirm.domain.document.repository.DossierTemplateRepository;
import com.lawfirm.domain.document.repository.MatterDossierItemRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目卷宗管理服务
 * 负责卷宗目录的初始化、管理
 * 
 * 权限说明：
 * - 查看卷宗：继承项目的查看权限（通过 validateMatterAccess）
 * - 编辑卷宗：只有项目成员才能操作（通过 validateMatterOwnership）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterDossierService {

    private final MatterRepository matterRepository;
    private final DossierTemplateRepository templateRepository;
    private final DossierTemplateItemRepository templateItemRepository;
    private final MatterDossierItemRepository dossierItemRepository;

    private MatterAppService matterAppService;

    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    public void setMatterAppService(MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 获取项目的卷宗目录
     * 如果尚未初始化，则根据案件类型自动初始化
     * 
     * 权限：继承项目的查看权限
     */
    public List<MatterDossierItem> getDossierItems(Long matterId) {
        // 验证用户是否有权访问该项目
        validateMatterAccess(matterId);

        // 检查是否已初始化
        if (!dossierItemRepository.hasDossierItems(matterId)) {
            initializeDossier(matterId);
        }
        return dossierItemRepository.findByMatterId(matterId);
    }

    /**
     * 验证用户是否有权访问指定的项目（查看权限）
     */
    private void validateMatterAccess(Long matterId) {
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

        // null 表示可以访问所有项目（ALL权限）
        if (accessibleMatterIds != null && !accessibleMatterIds.contains(matterId)) {
            throw new BusinessException("无权访问该项目的卷宗");
        }
    }

    /**
     * 根据案件类型初始化卷宗目录
     */
    @Transactional
    public void initializeDossier(Long matterId) {
        Matter matter = matterRepository.getById(matterId);
        if (matter == null) {
            log.warn("项目不存在: {}", matterId);
            return;
        }

        // 如果已初始化，跳过
        if (dossierItemRepository.hasDossierItems(matterId)) {
            log.info("项目卷宗目录已初始化: {}", matterId);
            return;
        }

        // 根据案件类型确定模板
        String caseType = determineCaseType(matter);
        DossierTemplate template = templateRepository.findDefaultByCaseType(caseType);

        if (template == null) {
            // 如果没有找到对应模板，使用民事案件模板作为默认
            template = templateRepository.findDefaultByCaseType("CIVIL");
        }

        if (template == null) {
            log.warn("未找到卷宗模板，跳过初始化: matterId={}, caseType={}", matterId, caseType);
            return;
        }

        // 获取模板目录项
        List<DossierTemplateItem> templateItems = templateItemRepository.findByTemplateId(template.getId());

        // 复制模板目录到项目
        for (DossierTemplateItem templateItem : templateItems) {
            MatterDossierItem dossierItem = MatterDossierItem.builder()
                    .matterId(matterId)
                    .parentId(templateItem.getParentId())
                    .name(templateItem.getName())
                    .itemType(templateItem.getItemType())
                    .fileCategory(templateItem.getFileCategory())
                    .sortOrder(templateItem.getSortOrder())
                    .documentCount(0)
                    .build();
            dossierItemRepository.save(dossierItem);
        }

        log.info("项目卷宗目录初始化完成: matterId={}, template={}, itemCount={}",
                matterId, template.getName(), templateItems.size());
    }

    /**
     * 根据项目信息确定案件类型
     */
    private String determineCaseType(Matter matter) {
        String matterType = matter.getMatterType();
        String caseType = matter.getCaseType();

        // 优先使用 caseType（案件类型）
        if (caseType != null && !caseType.isEmpty()) {
            return switch (caseType.toUpperCase()) {
                case "CRIMINAL" -> "CRIMINAL"; // 刑事案件
                case "CIVIL", "ADMINISTRATIVE", "BANKRUPTCY", "IP", "ARBITRATION", "ENFORCEMENT" -> "CIVIL"; // 民事/行政/破产/知产/仲裁/执行
                                                                                                             // - 使用民事模板
                case "LEGAL_COUNSEL" -> "LEGAL_COUNSEL"; // 法律顾问
                case "SPECIAL_SERVICE" -> "NON_LITIGATION"; // 专项服务 - 使用非诉模板
                default -> "CIVIL";
            };
        }

        // 如果没有 caseType，根据 matterType 选择通用模板
        if (matterType != null && "NON_LITIGATION".equalsIgnoreCase(matterType)) {
            return "NON_LITIGATION"; // 非诉项目使用非诉模板
        }

        return "CIVIL"; // 默认使用民事模板
    }

    /**
     * 添加自定义目录项
     * 
     * 权限：只有项目成员才能添加目录项
     */
    @Transactional
    public MatterDossierItem addDossierItem(Long matterId, Long parentId, String name, String itemType) {
        // 验证用户是否是项目成员（有编辑权限）
        matterAppService.validateMatterOwnership(matterId);

        // 获取当前最大排序号
        List<MatterDossierItem> siblings = dossierItemRepository.findByParentId(matterId, parentId);
        int maxSort = siblings.stream()
                .mapToInt(MatterDossierItem::getSortOrder)
                .max()
                .orElse(0);

        MatterDossierItem item = MatterDossierItem.builder()
                .matterId(matterId)
                .parentId(parentId)
                .name(name)
                .itemType(itemType != null ? itemType : "FOLDER")
                .sortOrder(maxSort + 1)
                .documentCount(0)
                .build();

        dossierItemRepository.save(item);
        return item;
    }

    /**
     * 更新目录项
     * 
     * 权限：只有项目成员才能更新目录项
     */
    @Transactional
    public MatterDossierItem updateDossierItem(Long itemId, String name, Integer sortOrder) {
        MatterDossierItem item = dossierItemRepository.getById(itemId);
        if (item == null) {
            throw new BusinessException("目录项不存在");
        }

        // 验证用户是否是项目成员（有编辑权限）
        matterAppService.validateMatterOwnership(item.getMatterId());

        if (name != null) {
            item.setName(name);
        }
        if (sortOrder != null) {
            item.setSortOrder(sortOrder);
        }

        dossierItemRepository.updateById(item);
        return item;
    }

    /**
     * 删除目录项
     * 
     * 权限：只有项目成员才能删除目录项
     */
    @Transactional
    public void deleteDossierItem(Long itemId) {
        MatterDossierItem item = dossierItemRepository.getById(itemId);
        if (item == null) {
            return;
        }

        // 验证用户是否是项目成员（有编辑权限）
        matterAppService.validateMatterOwnership(item.getMatterId());

        // 检查是否有文件
        if (item.getDocumentCount() != null && item.getDocumentCount() > 0) {
            throw new BusinessException("目录下有文件，无法删除");
        }

        // 软删除
        dossierItemRepository.removeById(itemId);
    }

    /**
     * 调整目录项排序
     * 
     * 权限：只有项目成员才能调整排序
     */
    @Transactional
    public void reorderDossierItems(Long matterId, List<Long> itemIds) {
        // 验证用户是否是项目成员（有编辑权限）
        matterAppService.validateMatterOwnership(matterId);

        for (int i = 0; i < itemIds.size(); i++) {
            MatterDossierItem item = dossierItemRepository.getById(itemIds.get(i));
            if (item != null && item.getMatterId().equals(matterId)) {
                item.setSortOrder(i + 1);
                dossierItemRepository.updateById(item);
            }
        }
    }

    /**
     * 获取所有卷宗模板
     */
    public List<DossierTemplate> getAllTemplates() {
        return templateRepository.findAllTemplates();
    }

    /**
     * 创建卷宗模板
     */
    @Transactional
    public DossierTemplate createTemplate(DossierTemplate template) {
        templateRepository.save(template);
        return template;
    }

    /**
     * 更新卷宗模板
     */
    @Transactional
    public DossierTemplate updateTemplate(DossierTemplate template) {
        templateRepository.updateById(template);
        return template;
    }

    /**
     * 删除卷宗模板
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        // 先删除关联的目录项
        List<DossierTemplateItem> items = templateItemRepository.findByTemplateId(templateId);
        for (DossierTemplateItem item : items) {
            templateItemRepository.removeById(item.getId());
        }
        templateRepository.removeById(templateId);
    }

    /**
     * 获取模板目录项
     */
    public List<DossierTemplateItem> getTemplateItems(Long templateId) {
        return templateItemRepository.findByTemplateId(templateId);
    }

    /**
     * 添加模板目录项
     */
    @Transactional
    public DossierTemplateItem addTemplateItem(DossierTemplateItem item) {
        templateItemRepository.save(item);
        return item;
    }

    /**
     * 更新模板目录项
     */
    @Transactional
    public DossierTemplateItem updateTemplateItem(DossierTemplateItem item) {
        templateItemRepository.updateById(item);
        return item;
    }

    /**
     * 删除模板目录项
     */
    @Transactional
    public void deleteTemplateItem(Long itemId) {
        // 简单删除，前端应确保无子项或逻辑上处理
        templateItemRepository.removeById(itemId);
    }
}
