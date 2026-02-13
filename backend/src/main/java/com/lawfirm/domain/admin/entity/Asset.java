package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 固定资产实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("admin_asset")
public class Asset extends BaseEntity {

  /** 资产编号 */
  private String assetNo;

  /** 资产名称 */
  private String name;

  /** 资产分类：OFFICE-办公设备, IT-IT设备, FURNITURE-家具, VEHICLE-车辆, OTHER-其他 */
  private String category;

  /** 品牌 */
  private String brand;

  /** 型号 */
  private String model;

  /** 规格 */
  private String specification;

  /** 序列号 */
  private String serialNumber;

  /** 购买日期 */
  private LocalDate purchaseDate;

  /** 购买价格 */
  private BigDecimal purchasePrice;

  /** 供应商 */
  private String supplier;

  /** 保修期至 */
  private LocalDate warrantyExpireDate;

  /** 使用年限（年） */
  private Integer usefulLife;

  /** 存放位置 */
  private String location;

  /** 当前使用人ID */
  private Long currentUserId;

  /** 所属部门ID */
  private Long departmentId;

  /** 状态：IDLE-闲置, IN_USE-使用中, MAINTENANCE-维修中, SCRAPPED-已报废 */
  private String status;

  /** 资产图片URL */
  private String imageUrl;

  /** 备注 */
  private String remarks;
}
