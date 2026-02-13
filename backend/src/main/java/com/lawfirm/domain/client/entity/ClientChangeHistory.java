package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.ibatis.type.Alias;

/** 企业变更历史记录实体（M2-014）。 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_client_change_history")
@Alias("ClientChangeHistory")
public class ClientChangeHistory extends BaseEntity {

  /** 客户ID */
  private Long clientId;

  /**
   * 变更类型：NAME-名称, REGISTERED_CAPITAL-注册资本, LEGAL_REPRESENTATIVE-法定代表人, ADDRESS-地址,
   * BUSINESS_SCOPE-经营范围, SHAREHOLDER-股东, OTHER-其他
   */
  private String changeType;

  /** 变更日期 */
  private LocalDate changeDate;

  /** 变更前值 */
  private String beforeValue;

  /** 变更后值 */
  private String afterValue;

  /** 变更描述 */
  private String changeDescription;

  /** 登记机关 */
  private String registrationAuthority;

  /** 登记编号 */
  private String registrationNumber;

  /** 附件URL（变更通知书等） */
  private String attachmentUrl;

  // 变更类型常量

  /** 名称变更 */
  public static final String TYPE_NAME = "NAME";

  /** 注册资本变更 */
  public static final String TYPE_REGISTERED_CAPITAL = "REGISTERED_CAPITAL";

  /** 法定代表人变更 */
  public static final String TYPE_LEGAL_REPRESENTATIVE = "LEGAL_REPRESENTATIVE";

  /** 地址变更 */
  public static final String TYPE_ADDRESS = "ADDRESS";

  /** 经营范围变更 */
  public static final String TYPE_BUSINESS_SCOPE = "BUSINESS_SCOPE";

  /** 股东变更 */
  public static final String TYPE_SHAREHOLDER = "SHAREHOLDER";

  /** 其他变更 */
  public static final String TYPE_OTHER = "OTHER";
}
