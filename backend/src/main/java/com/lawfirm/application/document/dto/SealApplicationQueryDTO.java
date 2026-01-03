package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用印申请查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealApplicationQueryDTO extends PageQuery {

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 印章ID
     */
    private Long sealId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 状态
     */
    private String status;
}
