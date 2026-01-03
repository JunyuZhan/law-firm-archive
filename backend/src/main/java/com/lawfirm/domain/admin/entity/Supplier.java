package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 供应商实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_supplier")
public class Supplier extends BaseEntity {

    /**
     * 供应商编号
     */
    private String supplierNo;

    /**
     * 供应商名称
     */
    private String name;

    /**
     * 供应商类型：GOODS-物品供应商, SERVICE-服务供应商, BOTH-综合供应商
     */
    private String supplierType;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 地址
     */
    private String address;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行账号
     */
    private String bankAccount;

    /**
     * 供应范围
     */
    private String supplyScope;

    /**
     * 评级：A-优秀, B-良好, C-一般, D-较差
     */
    private String rating;

    /**
     * 状态：ACTIVE-正常, INACTIVE-停用
     */
    private String status;

    /**
     * 备注
     */
    private String remarks;
}
