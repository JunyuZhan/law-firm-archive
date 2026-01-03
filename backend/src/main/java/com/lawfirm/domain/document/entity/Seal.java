package com.lawfirm.domain.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 印章实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("seal_info")
public class Seal extends BaseEntity {

    /**
     * 印章编号
     */
    private String sealNo;

    /**
     * 印章名称
     */
    private String name;

    /**
     * 印章类型：OFFICIAL-公章, CONTRACT-合同章, FINANCE-财务章, LEGAL-法人章, OTHER-其他
     */
    private String sealType;

    /**
     * 保管人ID
     */
    private Long keeperId;

    /**
     * 保管人姓名
     */
    private String keeperName;

    /**
     * 印章图片URL
     */
    private String imageUrl;

    /**
     * 状态：ACTIVE-在用, DISABLED-停用, LOST-遗失, DESTROYED-销毁
     */
    @lombok.Builder.Default
    private String status = "ACTIVE";

    /**
     * 描述
     */
    private String description;
}
