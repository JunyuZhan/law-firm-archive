package com.lawfirm.application.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 资产DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {

  /** 资产ID */
  private Long id;

  /** 资产编号 */
  private String assetNo;

  /** 资产名称 */
  private String name;

  /** 资产类别 */
  private String category;

  /** 资产类别名称 */
  private String categoryName;

  /** 品牌 */
  private String brand;

  /** 型号 */
  private String model;

  /** 规格 */
  private String specification;

  /** 序列号 */
  private String serialNumber;

  /** 采购日期 */
  private LocalDate purchaseDate;

  /** 采购价格 */
  private BigDecimal purchasePrice;

  /** 供应商 */
  private String supplier;

  /** 保修到期日期 */
  private LocalDate warrantyExpireDate;

  /** 使用年限（年） */
  private Integer usefulLife;

  /** 位置 */
  private String location;

  /** 当前使用人ID */
  private Long currentUserId;

  /** 当前使用人名称 */
  private String currentUserName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 图片URL */
  private String imageUrl;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;

  /** 是否在保修期内 */
  private Boolean inWarranty;
}
