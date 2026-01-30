package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateCauseCommand;
import com.lawfirm.application.system.command.UpdateCauseCommand;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.CauseOfAction;
import com.lawfirm.domain.system.repository.CauseOfActionRepository;
import com.lawfirm.infrastructure.persistence.mapper.CauseOfActionMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 案由/罪名服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CauseOfActionService {

  /** CauseOfAction Mapper. */
  private final CauseOfActionMapper causeOfActionMapper;

  /** CauseOfAction Repository. */
  private final CauseOfActionRepository causeOfActionRepository;

  /** 案由类型常量 - 民事. */
  public static final String TYPE_CIVIL = "CIVIL";

  /** 案由类型常量 - 刑事. */
  public static final String TYPE_CRIMINAL = "CRIMINAL";

  /** 案由类型常量 - 行政. */
  public static final String TYPE_ADMIN = "ADMIN";

  /**
   * 根据代码获取案由名称 如果找不到，返回原代码
   *
   * @param code 代码
   * @param causeType 案由类型
   * @return 名称
   */
  @Cacheable(value = "causeName", key = "#code + '_' + #causeType")
  public String getCauseName(final String code, final String causeType) {
    if (!StringUtils.hasText(code)) {
      return "";
    }

    // 如果已经是中文名称，直接返回
    if (isChineseName(code)) {
      return code;
    }

    String name = causeOfActionMapper.findNameByCode(code, causeType);
    return StringUtils.hasText(name) ? name : code;
  }

  /**
   * 获取民事案由名称
   *
   * @param code 代码
   * @return 名称
   */
  public String getCivilCauseName(final String code) {
    return getCauseName(code, TYPE_CIVIL);
  }

  /**
   * 获取刑事罪名
   *
   * @param code 代码
   * @return 名称
   */
  public String getCriminalChargeName(final String code) {
    return getCauseName(code, TYPE_CRIMINAL);
  }

  /**
   * 获取行政案由名称
   *
   * @param code 代码
   * @return 名称
   */
  public String getAdminCauseName(final String code) {
    return getCauseName(code, TYPE_ADMIN);
  }

  /**
   * 获取所有民事案由（树形结构）
   *
   * @return 民事案由树形列表
   */
  @Cacheable(value = "causeTree", key = "'civil'")
  public List<CauseTreeNode> getCivilCauseTree() {
    return buildCauseTree(TYPE_CIVIL);
  }

  /**
   * 获取所有刑事罪名（树形结构）
   *
   * @return 刑事罪名树形列表
   */
  @Cacheable(value = "causeTree", key = "'criminal'")
  public List<CauseTreeNode> getCriminalChargeTree() {
    return buildCauseTree(TYPE_CRIMINAL);
  }

  /**
   * 获取所有行政案由（树形结构）
   *
   * @return 行政案由树形列表
   */
  @Cacheable(value = "causeTree", key = "'admin'")
  public List<CauseTreeNode> getAdminCauseTree() {
    return buildCauseTree(TYPE_ADMIN);
  }

  /**
   * 搜索案由
   *
   * @param causeType 案由类型
   * @param keyword 关键词
   * @return 案由列表
   */
  public List<CauseOfAction> searchCauses(final String causeType, final String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return new ArrayList<>();
    }
    return causeOfActionMapper.searchByName(causeType, keyword);
  }

  /**
   * 构建树形结构
   *
   * @param causeType 案由类型
   * @return 树节点列表
   */
  private List<CauseTreeNode> buildCauseTree(final String causeType) {
    List<CauseOfAction> allCauses = causeOfActionMapper.findAllByType(causeType);

    // 按分类分组
    Map<String, List<CauseOfAction>> groupedByCategory =
        allCauses.stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.getCategoryCode() + "|" + c.getCategoryName(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    List<CauseTreeNode> result = new ArrayList<>();

    for (Map.Entry<String, List<CauseOfAction>> entry : groupedByCategory.entrySet()) {
      String[] parts = entry.getKey().split("\\|");
      String categoryCode = parts[0];
      String categoryName = parts.length > 1 ? parts[1] : categoryCode;

      CauseTreeNode categoryNode = new CauseTreeNode();
      categoryNode.setCode(categoryCode);
      categoryNode.setName(categoryName);
      categoryNode.setLevel(0);
      categoryNode.setCategoryCode(categoryCode);
      categoryNode.setCategoryName(categoryName);
      categoryNode.setIsLeaf(false);
      categoryNode.setChildren(new ArrayList<>());

      // 分离一级和二级案由
      List<CauseOfAction> level1Causes =
          entry.getValue().stream()
              .filter(c -> c.getLevel() == null || c.getLevel() == 1)
              .collect(Collectors.toList());

      List<CauseOfAction> level2Causes =
          entry.getValue().stream()
              .filter(c -> c.getLevel() != null && c.getLevel() == 2)
              .collect(Collectors.toList());

      // 构建一级案由节点
      for (CauseOfAction cause : level1Causes) {
        CauseTreeNode node = convertToTreeNode(cause);
        node.setChildren(new ArrayList<>());
        node.setIsLeaf(false);

        // 添加子案由
        String parentCode = cause.getCode();
        for (CauseOfAction child : level2Causes) {
          if (child.getCode().startsWith(parentCode + ".")) {
            CauseTreeNode childNode = convertToTreeNode(child);
            childNode.setParentCode(parentCode);
            childNode.setIsLeaf(true);
            node.getChildren().add(childNode);
          }
        }

        // 如果没有子节点，设置为叶子节点
        if (node.getChildren().isEmpty()) {
          node.setIsLeaf(true);
        }

        categoryNode.getChildren().add(node);
      }

      result.add(categoryNode);
    }

    return result;
  }

  /**
   * 将实体转换为树节点
   *
   * @param cause 案由实体
   * @return 树节点
   */
  private CauseTreeNode convertToTreeNode(final CauseOfAction cause) {
    CauseTreeNode node = new CauseTreeNode();
    node.setId(cause.getId());
    node.setCode(cause.getCode());
    node.setName(cause.getName());
    node.setLevel(cause.getLevel() != null ? cause.getLevel() : 1);
    node.setCategoryCode(cause.getCategoryCode());
    node.setCategoryName(cause.getCategoryName());
    node.setParentCode(cause.getParentCode());
    node.setIsActive(cause.getIsActive() != null ? cause.getIsActive() : true);
    return node;
  }

  /**
   * 判断是否已经是中文名称
   *
   * @param text 文本
   * @return 是否包含中文
   */
  private boolean isChineseName(final String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    // 如果包含中文字符，认为是名称而非代码
    return text.chars().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
  }

  // ==================== CRUD 操作 ====================

  /**
   * 创建案由/罪名
   *
   * @param command 创建命令
   * @return 案由实体
   */
  @Transactional
  @CacheEvict(
      value = {"causeTree", "causeName"},
      allEntries = true)
  public CauseOfAction createCause(final CreateCauseCommand command) {
    // 验证代码格式
    validateCodeFormat(command.getCode(), command.getCauseType());

    // 检查代码是否已存在
    if (causeOfActionMapper.existsByCodeAndType(command.getCode(), command.getCauseType())) {
      throw new BusinessException("案由代码已存在: " + command.getCode());
    }

    // 验证层级关系
    if (command.getLevel() == 2) {
      if (!StringUtils.hasText(command.getParentCode())) {
        throw new BusinessException("二级案由必须指定父级案由代码");
      }
      CauseOfAction parent =
          causeOfActionMapper.findByCodeAndType(command.getParentCode(), command.getCauseType());
      if (parent == null) {
        throw new BusinessException("父级案由不存在: " + command.getParentCode());
      }
      if (parent.getLevel() != 1) {
        throw new BusinessException("父级案由必须是一级案由");
      }
    }

    // 自动生成排序号
    if (command.getSortOrder() == null) {
      Integer maxSort =
          causeOfActionMapper.getMaxSortOrder(command.getCauseType(), command.getCategoryCode());
      command.setSortOrder(maxSort != null ? maxSort + 1 : 1);
    }

    CauseOfAction cause = new CauseOfAction();
    cause.setCode(command.getCode());
    cause.setName(command.getName());
    cause.setCauseType(command.getCauseType());
    cause.setCategoryCode(command.getCategoryCode());
    cause.setCategoryName(command.getCategoryName());
    cause.setParentCode(command.getParentCode());
    cause.setLevel(command.getLevel());
    cause.setSortOrder(command.getSortOrder());
    cause.setIsActive(command.getIsActive() != null ? command.getIsActive() : true);

    causeOfActionRepository.save(cause);
    log.info("案由创建成功: {} - {}", command.getCode(), command.getName());
    return cause;
  }

  /**
   * 更新案由/罪名
   *
   * @param id 案由ID
   * @param command 更新命令
   * @return 案由实体
   */
  @Transactional
  @CacheEvict(
      value = {"causeTree", "causeName"},
      allEntries = true)
  public CauseOfAction updateCause(final Long id, final UpdateCauseCommand command) {
    CauseOfAction cause = causeOfActionRepository.getByIdOrThrow(id, "案由不存在");

    // 验证层级关系
    if (command.getLevel() != null && command.getLevel() == 2) {
      if (!StringUtils.hasText(command.getParentCode())) {
        throw new BusinessException("二级案由必须指定父级案由代码");
      }
      CauseOfAction parent =
          causeOfActionMapper.findByCodeAndType(command.getParentCode(), cause.getCauseType());
      if (parent == null) {
        throw new BusinessException("父级案由不存在: " + command.getParentCode());
      }
      if (parent.getLevel() != 1) {
        throw new BusinessException("父级案由必须是一级案由");
      }
    }

    // 更新字段
    if (StringUtils.hasText(command.getName())) {
      cause.setName(command.getName());
    }
    if (StringUtils.hasText(command.getCategoryCode())) {
      cause.setCategoryCode(command.getCategoryCode());
    }
    if (StringUtils.hasText(command.getCategoryName())) {
      cause.setCategoryName(command.getCategoryName());
    }
    if (command.getParentCode() != null) {
      cause.setParentCode(command.getParentCode());
    }
    if (command.getLevel() != null) {
      cause.setLevel(command.getLevel());
    }
    if (command.getSortOrder() != null) {
      cause.setSortOrder(command.getSortOrder());
    }
    if (command.getIsActive() != null) {
      cause.setIsActive(command.getIsActive());
    }

    causeOfActionRepository.updateById(cause);
    log.info("案由更新成功: {} - {}", cause.getCode(), cause.getName());
    return cause;
  }

  /**
   * 删除案由/罪名（软删除：设置为禁用）
   *
   * @param id 案由ID
   */
  @Transactional
  @CacheEvict(
      value = {"causeTree", "causeName"},
      allEntries = true)
  public void deleteCause(final Long id) {
    CauseOfAction cause = causeOfActionRepository.getByIdOrThrow(id, "案由不存在");

    // 检查是否有子案由
    if (cause.getLevel() == 1) {
      List<CauseOfAction> children =
          causeOfActionMapper.findByParentCode(cause.getCode(), cause.getCauseType());
      if (!children.isEmpty()) {
        throw new BusinessException("该案由下存在子案由，无法删除");
      }
    }

    // 软删除：设置为禁用
    cause.setIsActive(false);
    causeOfActionRepository.updateById(cause);
    log.info("案由已禁用: {} - {}", cause.getCode(), cause.getName());
  }

  /**
   * 获取案由详情
   *
   * @param id 案由ID
   * @return 案由实体
   */
  public CauseOfAction getCauseById(final Long id) {
    return causeOfActionRepository.getByIdOrThrow(id, "案由不存在");
  }

  /**
   * 启用/禁用案由
   *
   * @param id 案由ID
   */
  @Transactional
  @CacheEvict(
      value = {"causeTree", "causeName"},
      allEntries = true)
  public void toggleCauseStatus(final Long id) {
    CauseOfAction cause = causeOfActionRepository.getByIdOrThrow(id, "案由不存在");
    cause.setIsActive(!cause.getIsActive());
    causeOfActionRepository.updateById(cause);
    log.info(
        "案由状态已{}: {} - {}", cause.getIsActive() ? "启用" : "禁用", cause.getCode(), cause.getName());
  }

  /**
   * 验证代码格式
   *
   * @param code 代码
   * @param causeType 案由类型
   */
  private void validateCodeFormat(final String code, final String causeType) {
    if (!StringUtils.hasText(code)) {
      throw new BusinessException("案由代码不能为空");
    }

    switch (causeType) {
      case TYPE_CIVIL:
        // 民事案由：数字或 数字.数字
        if (!code.matches("^\\d+(\\.\\d+)?$")) {
          throw new BusinessException("民事案由代码格式错误，应为数字或数字.数字，如：1, 8.1");
        }
        break;
      case TYPE_CRIMINAL:
        // 刑事罪名：C + 数字
        if (!code.matches("^C\\d+$")) {
          throw new BusinessException("刑事罪名代码格式错误，应为C+数字，如：C101");
        }
        break;
      case TYPE_ADMIN:
        // 行政案由：A + 数字 或 A + 数字.数字
        if (!code.matches("^A\\d+(\\.\\d+)?$")) {
          throw new BusinessException("行政案由代码格式错误，应为A+数字或A+数字.数字，如：A1, A1.1");
        }
        break;
      default:
        throw new BusinessException("未知的案由类型: " + causeType);
    }
  }

  /** 案由树节点. */
  @lombok.Data
  public static class CauseTreeNode {
    /** ID. */
    private Long id;

    /** 代码. */
    private String code;

    /** 名称. */
    private String name;

    /** 层级. */
    private Integer level;

    /** 分类代码. */
    private String categoryCode;

    /** 分类名称. */
    private String categoryName;

    /** 父级代码. */
    private String parentCode;

    /** 是否激活. */
    private Boolean isActive;

    /** 是否叶子节点. */
    private Boolean isLeaf;

    /** 子节点列表. */
    private List<CauseTreeNode> children;
  }
}
