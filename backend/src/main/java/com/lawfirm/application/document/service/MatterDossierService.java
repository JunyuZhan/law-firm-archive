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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目卷宗管理服务 负责卷宗目录的初始化、管理
 *
 * <p>权限说明： - 查看卷宗：继承项目的查看权限（通过 validateMatterAccess） - 编辑卷宗：只有项目成员才能操作（通过 validateMatterOwnership）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterDossierService {

  /** 案件类型：刑事案件 */
  private static final String CASE_TYPE_CRIMINAL = "CRIMINAL";

  /** 案件类型：民事案件 */
  private static final String CASE_TYPE_CIVIL = "CIVIL";

  /** 案件类型：行政案件 */
  private static final String CASE_TYPE_ADMINISTRATIVE = "ADMINISTRATIVE";

  /** 案件类型：破产案件 */
  private static final String CASE_TYPE_BANKRUPTCY = "BANKRUPTCY";

  /** 案件类型：知识产权案件 */
  private static final String CASE_TYPE_IP = "IP";

  /** 案件类型：仲裁案件 */
  private static final String CASE_TYPE_ARBITRATION = "ARBITRATION";

  /** 案件类型：执行案件 */
  private static final String CASE_TYPE_ENFORCEMENT = "ENFORCEMENT";

  /** 案件类型：法律顾问 */
  private static final String CASE_TYPE_LEGAL_COUNSEL = "LEGAL_COUNSEL";

  /** 案件类型：专项服务 */
  private static final String CASE_TYPE_SPECIAL_SERVICE = "SPECIAL_SERVICE";

  /** 案件类型：非诉项目 */
  private static final String CASE_TYPE_NON_LITIGATION = "NON_LITIGATION";

  /** 排序增量 */
  private static final int SORT_INCREMENT = 1;

  /** 项目仓储. */
  private final MatterRepository matterRepository;

  /** 卷宗模板仓储. */
  private final DossierTemplateRepository templateRepository;

  /** 卷宗模板项仓储. */
  private final DossierTemplateItemRepository templateItemRepository;

  /** 项目卷宗项仓储. */
  private final MatterDossierItemRepository dossierItemRepository;

  /** 项目应用服务. */
  private MatterAppService matterAppService;

  /**
   * 设置MatterAppService依赖。
   *
   * @param matterAppService 项目应用服务
   */
  @org.springframework.beans.factory.annotation.Autowired
  @Lazy
  public void setMatterAppService(final MatterAppService matterAppService) {
    this.matterAppService = matterAppService;
  }

  /**
   * 获取项目的卷宗目录 如果尚未初始化，则根据案件类型自动初始化。
   *
   * <p>权限：继承项目的查看权限
   *
   * @param matterId 项目ID
   * @return 卷宗目录项列表
   */
  public List<MatterDossierItem> getDossierItems(final Long matterId) {
    // 验证用户是否有权访问该项目
    validateMatterAccess(matterId);

    // 检查是否已初始化
    if (!dossierItemRepository.hasDossierItems(matterId)) {
      initializeDossier(matterId);
    }
    return dossierItemRepository.findByMatterId(matterId);
  }

  /**
   * 验证用户是否有权访问指定的项目（查看权限）。
   *
   * @param matterId 项目ID
   */
  private void validateMatterAccess(final Long matterId) {
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();
    List<Long> accessibleMatterIds =
        matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    // null 表示可以访问所有项目（ALL权限）
    if (accessibleMatterIds != null && !accessibleMatterIds.contains(matterId)) {
      throw new BusinessException("无权访问该项目的卷宗");
    }
  }

  /**
   * 根据案件类型初始化卷宗目录。
   *
   * @param matterId 项目ID
   */
  @Transactional
  public void initializeDossier(final Long matterId) {
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
    List<DossierTemplateItem> templateItems =
        templateItemRepository.findByTemplateId(template.getId());

    // 复制模板目录到项目
    for (DossierTemplateItem templateItem : templateItems) {
      MatterDossierItem dossierItem =
          MatterDossierItem.builder()
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

    log.info(
        "项目卷宗目录初始化完成: matterId={}, template={}, itemCount={}",
        matterId,
        template.getName(),
        templateItems.size());
  }

  /**
   * 根据项目信息确定案件类型。
   *
   * @param matter 项目实体
   * @return 案件类型代码
   */
  private String determineCaseType(final Matter matter) {
    String matterType = matter.getMatterType();
    String caseType = matter.getCaseType();

    // 优先使用 caseType（案件类型）
    if (caseType != null && !caseType.isEmpty()) {
      return switch (caseType.toUpperCase()) {
        case CASE_TYPE_CRIMINAL -> CASE_TYPE_CRIMINAL; // 刑事案件
        case CASE_TYPE_CIVIL,
            CASE_TYPE_ADMINISTRATIVE,
            CASE_TYPE_BANKRUPTCY,
            CASE_TYPE_IP,
            CASE_TYPE_ARBITRATION,
            CASE_TYPE_ENFORCEMENT -> CASE_TYPE_CIVIL; // 民事/行政/破产/知产/仲裁/执行
          // - 使用民事模板
        case CASE_TYPE_LEGAL_COUNSEL -> CASE_TYPE_LEGAL_COUNSEL; // 法律顾问
        case CASE_TYPE_SPECIAL_SERVICE -> CASE_TYPE_NON_LITIGATION; // 专项服务 - 使用非诉模板
        default -> CASE_TYPE_CIVIL;
      };
    }

    // 如果没有 caseType，根据 matterType 选择通用模板
    if (matterType != null && CASE_TYPE_NON_LITIGATION.equalsIgnoreCase(matterType)) {
      return CASE_TYPE_NON_LITIGATION; // 非诉项目使用非诉模板
    }

    return CASE_TYPE_CIVIL; // 默认使用民事模板
  }

  /**
   * 添加自定义目录项。
   *
   * <p>权限：只有项目成员才能添加目录项
   *
   * @param matterId 项目ID
   * @param parentId 父目录项ID
   * @param name 目录项名称
   * @param itemType 目录项类型
   * @return 创建的卷宗目录项
   */
  @Transactional
  public MatterDossierItem addDossierItem(
      final Long matterId, final Long parentId, final String name, final String itemType) {
    // 验证用户是否是项目成员（有编辑权限）
    matterAppService.validateMatterOwnership(matterId);

    // 获取当前最大排序号
    List<MatterDossierItem> siblings = dossierItemRepository.findByParentId(matterId, parentId);
    int maxSort = siblings.stream().mapToInt(MatterDossierItem::getSortOrder).max().orElse(0);

    MatterDossierItem item =
        MatterDossierItem.builder()
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
   * 更新目录项。
   *
   * <p>权限：只有项目成员才能更新目录项
   *
   * @param itemId 目录项ID
   * @param name 目录项名称
   * @param sortOrder 排序号
   * @return 更新后的卷宗目录项
   */
  @Transactional
  public MatterDossierItem updateDossierItem(
      final Long itemId, final String name, final Integer sortOrder) {
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
   * 删除目录项。
   *
   * <p>权限：只有项目成员才能删除目录项
   *
   * @param itemId 目录项ID
   */
  @Transactional
  public void deleteDossierItem(final Long itemId) {
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
   * 调整目录项排序。
   *
   * <p>权限：只有项目成员才能调整排序
   *
   * @param matterId 项目ID
   * @param itemIds 目录项ID列表（按新的排序顺序）
   */
  @Transactional
  public void reorderDossierItems(final Long matterId, final List<Long> itemIds) {
    // 验证用户是否是项目成员（有编辑权限）
    matterAppService.validateMatterOwnership(matterId);

    for (int i = 0; i < itemIds.size(); i++) {
      MatterDossierItem item = dossierItemRepository.getById(itemIds.get(i));
      if (item != null && item.getMatterId().equals(matterId)) {
        item.setSortOrder(i + SORT_INCREMENT);
        dossierItemRepository.updateById(item);
      }
    }
  }

  /**
   * 获取所有卷宗模板。
   *
   * @return 卷宗模板列表
   */
  public List<DossierTemplate> getAllTemplates() {
    return templateRepository.findAllTemplates();
  }

  /**
   * 创建卷宗模板。
   *
   * @param template 卷宗模板实体
   * @return 创建的卷宗模板
   */
  @Transactional
  public DossierTemplate createTemplate(final DossierTemplate template) {
    templateRepository.save(template);
    return template;
  }

  /**
   * 更新卷宗模板。
   *
   * @param template 卷宗模板实体
   * @return 更新后的卷宗模板
   */
  @Transactional
  public DossierTemplate updateTemplate(final DossierTemplate template) {
    templateRepository.updateById(template);
    return template;
  }

  /**
   * 删除卷宗模板。
   *
   * @param templateId 模板ID
   */
  @Transactional
  public void deleteTemplate(final Long templateId) {
    // 先删除关联的目录项
    List<DossierTemplateItem> items = templateItemRepository.findByTemplateId(templateId);
    for (DossierTemplateItem item : items) {
      templateItemRepository.removeById(item.getId());
    }
    templateRepository.removeById(templateId);
  }

  /**
   * 获取模板目录项。
   *
   * @param templateId 模板ID
   * @return 模板目录项列表
   */
  public List<DossierTemplateItem> getTemplateItems(final Long templateId) {
    return templateItemRepository.findByTemplateId(templateId);
  }

  /**
   * 添加模板目录项。
   *
   * @param item 模板目录项实体
   * @return 创建的模板目录项
   */
  @Transactional
  public DossierTemplateItem addTemplateItem(final DossierTemplateItem item) {
    templateItemRepository.save(item);
    return item;
  }

  /**
   * 更新模板目录项。
   *
   * @param item 模板目录项实体
   * @return 更新后的模板目录项
   */
  @Transactional
  public DossierTemplateItem updateTemplateItem(final DossierTemplateItem item) {
    templateItemRepository.updateById(item);
    return item;
  }

  /**
   * 删除模板目录项。
   *
   * @param itemId 目录项ID
   */
  @Transactional
  public void deleteTemplateItem(final Long itemId) {
    // 简单删除，前端应确保无子项或逻辑上处理
    templateItemRepository.removeById(itemId);
  }
}
