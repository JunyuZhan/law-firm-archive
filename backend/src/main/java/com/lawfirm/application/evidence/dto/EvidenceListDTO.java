package com.lawfirm.application.evidence.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 证据清单DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class EvidenceListDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 清单编号 */
  private String listNo;

  /** 项目ID */
  private Long matterId;

  /** 清单名称 */
  private String name;

  /** 清单类型 */
  private String listType;

  /** 清单类型名称 */
  private String listTypeName;

  /** 证据ID列表 */
  private String evidenceIds;

  /** 证据ID列表 */
  private List<Long> evidenceIdList;

  /** 文件URL */
  private String fileUrl;

  /** 文件名 */
  private String fileName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 包含的证据列表 */
  private List<EvidenceDTO> evidences;
}
