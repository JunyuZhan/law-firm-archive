package com.lawfirm.application.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 客户门户 - 项目信息 DTO
 * 专供客户查看的脱敏项目信息
 */
@Data
@Schema(description = "客户门户项目信息（脱敏）")
public class PortalMatterDTO {

    // ========== 基本信息 ==========
    @Schema(description = "项目编号")
    private String matterNo;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目类型")
    private String matterType;

    @Schema(description = "项目类型名称")
    private String matterTypeName;

    @Schema(description = "案件类型")
    private String caseType;

    @Schema(description = "案件类型名称")
    private String caseTypeName;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "项目状态名称")
    private String statusName;

    @Schema(description = "立案日期")
    private LocalDate filingDate;

    @Schema(description = "预计结案日期")
    private LocalDate expectedClosingDate;

    // ========== 进度信息 ==========
    @Schema(description = "当前阶段")
    private String currentPhase;

    @Schema(description = "当前阶段名称")
    private String currentPhaseName;

    @Schema(description = "整体进度（百分比）")
    private Integer overallProgress;

    @Schema(description = "最近更新时间")
    private String lastUpdateTime;

    @Schema(description = "最近更新说明")
    private String lastUpdateNote;

    // ========== 团队信息 ==========
    @Schema(description = "团队成员列表")
    private List<TeamMemberDTO> teamMembers;

    // ========== 任务信息 ==========
    @Schema(description = "任务列表")
    private List<TaskSummaryDTO> tasks;

    // ========== 期限信息 ==========
    @Schema(description = "关键期限列表")
    private List<DeadlineDTO> deadlines;

    // ========== 文档信息 ==========
    @Schema(description = "文档列表（仅标题）")
    private List<DocumentSummaryDTO> documents;

    @Schema(description = "文档文件列表（含下载信息，用于推送到客户服务系统）")
    private List<DocumentFileDTO> documentFiles;

    // ========== 费用信息 ==========
    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "已收款金额")
    private BigDecimal receivedAmount;

    @Schema(description = "待收款金额")
    private BigDecimal pendingAmount;

    /**
     * 团队成员 DTO
     */
    @Data
    @Schema(description = "团队成员")
    public static class TeamMemberDTO {
        @Schema(description = "姓名")
        private String name;

        @Schema(description = "角色")
        private String role;

        @Schema(description = "角色名称")
        private String roleName;

        @Schema(description = "联系电话（脱敏）")
        private String phone;

        @Schema(description = "邮箱（脱敏）")
        private String email;
    }

    /**
     * 任务摘要 DTO
     */
    @Data
    @Schema(description = "任务摘要")
    public static class TaskSummaryDTO {
        @Schema(description = "任务标题")
        private String title;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "进度")
        private Integer progress;

        @Schema(description = "截止日期")
        private LocalDate dueDate;
    }

    /**
     * 期限 DTO
     */
    @Data
    @Schema(description = "关键期限")
    public static class DeadlineDTO {
        @Schema(description = "期限名称")
        private String name;

        @Schema(description = "期限类型")
        private String type;

        @Schema(description = "期限日期")
        private LocalDate deadline;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "状态名称")
        private String statusName;

        @Schema(description = "剩余天数")
        private Integer remainingDays;
    }

    /**
     * 文档摘要 DTO（仅标题信息）
     */
    @Data
    @Schema(description = "文档摘要")
    public static class DocumentSummaryDTO {
        @Schema(description = "文档ID")
        private Long id;

        @Schema(description = "文档名称")
        private String name;

        @Schema(description = "文档类型")
        private String type;

        @Schema(description = "上传时间")
        private String uploadTime;
    }

    /**
     * 文档文件 DTO（含下载信息，用于推送到客户服务系统）
     */
    @Data
    @Schema(description = "文档文件（含下载信息）")
    public static class DocumentFileDTO {
        @Schema(description = "文档ID")
        private Long id;

        @Schema(description = "文档名称")
        private String name;

        @Schema(description = "文档类型")
        private String type;

        @Schema(description = "文件类型（扩展名）")
        private String fileType;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "文件大小（格式化）")
        private String fileSizeFormatted;

        @Schema(description = "上传时间")
        private String uploadTime;

        @Schema(description = "临时下载URL（有效期内可下载）")
        private String downloadUrl;

        @Schema(description = "下载URL有效期至")
        private String downloadUrlExpireAt;

        @Schema(description = "文件内容Base64（小文件直接传输，可选）")
        private String contentBase64;

        @Schema(description = "文件MD5校验值")
        private String md5;
    }
}

