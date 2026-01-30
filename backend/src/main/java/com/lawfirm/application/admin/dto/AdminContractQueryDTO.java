package com.lawfirm.application.admin.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 行政合同查询DTO
 *
 * <p>Requirements: 5.2, 5.3, 5.4
 */
@Data
public class AdminContractQueryDTO {

  /** 合同编号 */
  private String contractNo;

  /** 委托人名称 */
  private String clientName;

  /** 承办律师ID */
  private Long leadLawyerId;

  /** 案件类型 */
  private String caseType;

  /** 签约日期起始 */
  private LocalDate signDateFrom;

  /** 签约日期结束 */
  private LocalDate signDateTo;

  /** 页码 */
  private int pageNum = 1;

  /** 每页大小 */
  private int pageSize = 10;
}
