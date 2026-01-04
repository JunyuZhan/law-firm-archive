package com.lawfirm.domain.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 合同模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("contract_template")
public class ContractTemplate extends BaseEntity {

    /**
     * 模板编号
     */
    private String templateNo;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目
     */
    private String contractType;

    /**
     * 默认收费方式
     */
    private String feeType;

    /**
     * 模板内容（支持变量替换）
     */
    private String content;

    /**
     * 标准条款（JSON格式）
     */
    private String clauses;

    /**
     * 模板说明
     */
    private String description;

    /**
     * 状态：ACTIVE-启用, INACTIVE-停用
     */
    private String status;

    /**
     * 排序
     */
    private Integer sortOrder;
}
