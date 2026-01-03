package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 利冲检查 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConflictCheckDTO extends BaseDTO {

    private String checkNo;
    private Long matterId;
    private String matterName;
    private Long clientId;
    private String clientName;
    private String checkType;
    private String checkTypeName;
    private String status;
    private String statusName;
    private String result;
    private String resultName;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String reviewComment;
    private String remark;
    
    /**
     * 检查项列表
     */
    private List<ConflictCheckItemDTO> items;
}

