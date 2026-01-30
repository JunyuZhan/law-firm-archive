package com.lawfirm.application.finance.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 智能匹配结果DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResultDTO {

  /** 匹配候选列表（按匹配度降序） */
  private List<MatchCandidateDTO> candidates;

  /** 推荐的匹配记录（匹配度最高且超过阈值） */
  private MatchCandidateDTO recommended;

  /** 是否有推荐记录 */
  private Boolean hasRecommended;

  /** 是否可以自动核销（匹配度超过自动核销阈值） */
  private Boolean canAutoReconcile;
}
