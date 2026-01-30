package com.lawfirm.application.evidence.dto;

import java.util.List;
import lombok.Data;

/** 证据清单对比结果DTO. 修复问题614: 使用类型安全的DTO替代Map<String, Object> */
@Data
public class EvidenceListCompareResult {

  /** 清单1信息 */
  private EvidenceListDTO list1;

  /** 清单2信息 */
  private EvidenceListDTO list2;

  /** 新增的证据ID列表 */
  private List<Long> addedEvidenceIds;

  /** 删除的证据ID列表 */
  private List<Long> removedEvidenceIds;

  /** 共同的证据ID列表 */
  private List<Long> commonEvidenceIds;

  /** 新增证据数量 */
  private int addedCount;

  /** 删除证据数量 */
  private int removedCount;

  /** 共同证据数量 */
  private int commonCount;

  /** 新增的证据详情（可选，按需加载） */
  private List<EvidenceDTO> addedEvidences;

  /** 删除的证据详情（可选，按需加载） */
  private List<EvidenceDTO> removedEvidences;

  /**
   * 创建对比结果.
   *
   * @param list1 清单1
   * @param list2 清单2
   * @param added 新增ID列表
   * @param removed 删除ID列表
   * @param common 共同ID列表
   * @return 对比结果
   */
  public static EvidenceListCompareResult create(
      final EvidenceListDTO list1,
      final EvidenceListDTO list2,
      final List<Long> added,
      final List<Long> removed,
      final List<Long> common) {
    EvidenceListCompareResult result = new EvidenceListCompareResult();
    result.setList1(list1);
    result.setList2(list2);
    result.setAddedEvidenceIds(added);
    result.setRemovedEvidenceIds(removed);
    result.setCommonEvidenceIds(common);
    result.setAddedCount(added != null ? added.size() : 0);
    result.setRemovedCount(removed != null ? removed.size() : 0);
    result.setCommonCount(common != null ? common.size() : 0);
    return result;
  }
}
