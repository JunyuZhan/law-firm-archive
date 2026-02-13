package com.lawfirm.application.clientservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送到客户服务系统的项目数据DTO.
 *
 * <p>注意：此DTO用于向外部客户服务系统推送数据，所有敏感信息应脱敏处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalMatterDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  // ========== 项目基本信息 (MATTER_INFO) ==========

  /** 项目ID. */
  private Long matterId;

  /** 项目编号. */
  private String matterNo;

  /** 项目名称. */
  private String matterName;

  /** 项目类型. */
  private String matterType;

  /** 项目类型名称. */
  private String matterTypeName;

  /** 项目状态. */
  private String status;

  /** 项目状态名称. */
  private String statusName;

  /** 委托日期. */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime createDate;

  // ========== 项目进度 (MATTER_PROGRESS) ==========

  /** 当前阶段. */
  private String currentStage;

  /** 当前阶段名称. */
  private String currentStageName;

  /** 整体进度(0-100). */
  private Integer progress;

  /** 最后更新时间. */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime lastUpdateTime;

  // ========== 律师信息 (LAWYER_INFO) ==========

  /** 主办律师姓名. */
  private String leadLawyerName;

  /** 主办律师联系方式（脱敏）. */
  private String leadLawyerContact;

  /** 团队成员列表. */
  private List<TeamMemberDTO> teamMembers;

  // ========== 任务列表 (TASK_LIST) ==========

  /** 待办任务. */
  private List<TaskDTO> pendingTasks;

  /** 已完成任务数. */
  private Integer completedTaskCount;

  /** 总任务数. */
  private Integer totalTaskCount;

  // ========== 关键期限 (DEADLINE_INFO) ==========

  /** 关键期限列表. */
  private List<DeadlineDTO> deadlines;

  // ========== 文档列表 (DOCUMENT_LIST) ==========

  /** 文档列表（仅标题）. */
  private List<DocumentDTO> documents;

  /** 文档总数. */
  private Integer documentCount;

  // ========== 可下载文件 (DOCUMENT_FILES) ==========

  /** 可下载文件列表. */
  private List<DownloadableFileDTO> downloadableFiles;

  // ========== 费用信息 (FEE_INFO) ==========

  /** 合同金额. */
  private BigDecimal contractAmount;

  /** 已收款金额. */
  private BigDecimal receivedAmount;

  /** 待收款金额. */
  private BigDecimal pendingAmount;

  // ========== 内嵌DTO ==========

  /** 团队成员. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeamMemberDTO implements Serializable {
    /** 序列化版本号. */
    private static final long serialVersionUID = 1L;

    /** 成员姓名. */
    private String name;

    /** 成员角色. */
    private String role;

    /** 联系方式. */
    private String contact;
  }

  /** 任务. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaskDTO implements Serializable {
    /** 序列化版本号. */
    private static final long serialVersionUID = 1L;

    /** 任务标题. */
    private String title;

    /** 任务状态. */
    private String status;

    /** 状态名称. */
    private String statusName;

    /** 进度. */
    private Integer progress;

    /** 截止日期. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dueDate;
  }

  /** 期限. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DeadlineDTO implements Serializable {
    /** 序列化版本号. */
    private static final long serialVersionUID = 1L;

    /** 期限标题. */
    private String title;

    /** 截止时间. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deadline;

    /** 期限类型. */
    private String type;

    /** 是否逾期. */
    private Boolean isOverdue;
  }

  /** 文档. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DocumentDTO implements Serializable {
    /** 序列化版本号. */
    private static final long serialVersionUID = 1L;

    /** 文档标题. */
    private String title;

    /** 文档分类. */
    private String category;

    /** 创建时间. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
  }

  /** 可下载文件（推送给客服系统）. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DownloadableFileDTO implements Serializable {
    /** 序列化版本号. */
    private static final long serialVersionUID = 1L;

    /** 文档ID（管理系统内部ID）. */
    private Long documentId;

    /** 文件名. */
    private String fileName;

    /** 文件类型. */
    private String fileType;

    /** 文件大小（字节）. */
    private Long fileSize;

    /** 文件分类. */
    private String category;

    /** 临时下载URL（供客服系统下载文件）. */
    private String sourceUrl;

    /** 上传时间. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime uploadTime;
  }
}
