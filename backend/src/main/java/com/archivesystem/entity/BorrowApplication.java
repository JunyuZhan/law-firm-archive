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
 * 借阅申请实体类.
 * 对应数据库表: arc_borrow_application
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("arc_borrow_application")
public class BorrowApplication extends BaseEntity {

    /** 申请编号 */
    private String applicationNo;
    
    /** 档案ID */
    private Long archiveId;
    
    /** 档案号（冗余） */
    private String archiveNo;
    
    /** 档案题名（冗余） */
    private String archiveTitle;

    // ===== 申请人信息 =====
    
    /** 申请人ID */
    private Long applicantId;
    
    /** 申请人姓名 */
    private String applicantName;
    
    /** 申请人部门 */
    private String applicantDept;
    
    /** 联系电话 */
    private String applicantPhone;
    
    /** 申请时间 */
    private LocalDateTime applyTime;

    // ===== 借阅信息 =====
    
    /** 借阅目的 */
    private String borrowPurpose;
    
    /** 借阅方式 */
    @Builder.Default
    private String borrowType = TYPE_ONLINE;
    
    /** 预计归还日期 */
    private LocalDate expectedReturnDate;
    
    /** 实际归还日期 */
    private LocalDate actualReturnDate;
    
    /** 借出时间 */
    private LocalDateTime borrowTime;
    
    /** 续借次数 */
    @Builder.Default
    private Integer renewCount = 0;

    // ===== 审批信息 =====
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_PENDING;
    
    /** 审批人ID */
    private Long approverId;
    
    /** 审批人姓名 */
    private String approverName;
    
    /** 审批时间 */
    private LocalDateTime approveTime;
    
    /** 审批意见 */
    private String approveRemarks;
    
    /** 拒绝原因 */
    private String rejectReason;
    
    /** 归还备注 */
    private String returnRemarks;

    // ===== 使用记录 =====
    
    /** 下载次数 */
    @Builder.Default
    private Integer downloadCount = 0;
    
    /** 阅览次数 */
    @Builder.Default
    private Integer viewCount = 0;
    
    /** 最后访问时间 */
    private LocalDateTime lastAccessAt;
    
    /** 备注 */
    private String remarks;

    // ===== 状态常量 =====
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_BORROWED = "BORROWED";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ===== 借阅方式常量 =====
    
    public static final String TYPE_ONLINE = "ONLINE";
    public static final String TYPE_DOWNLOAD = "DOWNLOAD";
    public static final String TYPE_COPY = "COPY";
}
