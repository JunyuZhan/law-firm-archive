package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 档案借阅实体类.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_borrow")
public class ArchiveBorrow extends BaseEntity {

    /** 借阅编号 */
    private String borrowNo;

    /** 档案ID */
    private Long archiveId;

    /** 借阅人ID */
    private Long borrowerId;

    /** 借阅人姓名 */
    private String borrowerName;

    /** 借阅人部门 */
    private String borrowerDept;

    /** 借阅人联系方式 */
    private String borrowerContact;

    /** 借阅原因 */
    private String borrowReason;

    /** 预计归还日期 */
    private LocalDate expectedReturnDate;

    /** 实际归还日期 */
    private LocalDate actualReturnDate;

    /** 
     * 状态：
     * PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝,
     * BORROWED-借出中, RETURNED-已归还, OVERDUE-已逾期
     */
    private String status;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批时间 */
    private LocalDateTime approvedAt;

    /** 审批意见 */
    private String approvalComment;

    /** 归还条件说明 */
    private String returnCondition;

    /** 备注 */
    private String remarks;

    // ===== 状态常量 =====
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_BORROWED = "BORROWED";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_OVERDUE = "OVERDUE";
}
