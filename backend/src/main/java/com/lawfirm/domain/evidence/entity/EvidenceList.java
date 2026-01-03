package com.lawfirm.domain.evidence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 证据清单实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("evidence_list")
public class EvidenceList extends BaseEntity {

    /** 清单编号 */
    private String listNo;

    /** 案件ID */
    private Long matterId;

    /** 清单名称 */
    private String name;

    /** 清单类型 */
    private String listType;

    /** 证据ID列表(JSON) */
    private String evidenceIds;

    /** 文件URL */
    private String fileUrl;

    /** 文件名 */
    private String fileName;

    /** 状态 */
    @lombok.Builder.Default
    private String status = STATUS_DRAFT;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_GENERATED = "GENERATED";

    public static final String TYPE_SUBMISSION = "SUBMISSION";
    public static final String TYPE_EXCHANGE = "EXCHANGE";
    public static final String TYPE_COURT = "COURT";
}
