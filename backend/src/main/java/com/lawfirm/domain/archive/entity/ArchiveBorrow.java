package com.lawfirm.domain.archive.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 档案借阅实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_borrow")
public class ArchiveBorrow extends BaseEntity {

    /**
     * 借阅编号
     */
    private String borrowNo;

    /**
     * 档案ID
     */
    private Long archiveId;

    /**
     * 借阅人ID
     */
    private Long borrowerId;

    /**
     * 借阅人姓名
     */
    private String borrowerName;

    /**
     * 部门
     */
    private String department;

    /**
     * 借阅原因
     */
    private String borrowReason;

    /**
     * 借阅日期
     */
    private LocalDate borrowDate;

    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;

    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;

    /**
     * 状态：PENDING(待审批), APPROVED(已批准), REJECTED(已拒绝), BORROWED(借出中), RETURNED(已归还), OVERDUE(逾期)
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 归还处理人ID
     */
    private Long returnHandlerId;

    /**
     * 归还状态：GOOD(完好), DAMAGED(损坏), LOST(遗失)
     */
    private String returnCondition;

    /**
     * 归还备注
     */
    private String returnRemarks;
}

