package com.lawfirm.domain.openapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 推送配置实体
 * 项目级别的客户服务推送设置
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("openapi_push_config")
public class PushConfig extends BaseEntity {

    /**
     * 项目ID（唯一）
     */
    private Long matterId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 是否启用推送
     */
    private Boolean enabled;

    /**
     * 默认推送范围（逗号分隔）
     */
    private String scopes;

    /**
     * 项目更新时是否自动推送
     */
    private Boolean autoPushOnUpdate;

    /**
     * 数据有效期（天）
     */
    private Integer validDays;
}

