package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 卷宗目录模板实体
 * 定义不同案件类型的标准卷宗目录结构
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dossier_template")
public class DossierTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    private String name;

    /**
     * 案件类型: CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务
     */
    private String caseType;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否默认模板
     */
    @lombok.Builder.Default
    private Boolean isDefault = false;
}

