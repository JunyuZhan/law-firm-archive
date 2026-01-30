package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.ibatis.type.Alias;

/** 劳动合同实体. */
@Alias("HrContract")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_contract")
public class Contract extends BaseEntity {

  /** 员工ID */
  private Long employeeId;

  /** 合同编号 */
  private String contractNo;

  /** 合同类型：FIXED-固定期限, UNFIXED-无固定期限, PROJECT-项目合同, INTERN-实习 */
  private String contractType;

  /** 合同开始日期 */
  private LocalDate startDate;

  /** 合同结束日期 */
  private LocalDate endDate;

  /** 试用期月数 */
  private Integer probationMonths;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 基本工资 */
  private BigDecimal baseSalary;

  /** 绩效奖金 */
  private BigDecimal performanceBonus;

  /** 其他津贴 */
  private BigDecimal otherAllowance;

  /** 状态：ACTIVE-生效中, EXPIRED-已到期, TERMINATED-已终止 */
  private String status;

  /** 签订日期 */
  private LocalDate signDate;

  /** 到期日期 */
  private LocalDate expireDate;

  /** 续签次数 */
  private Integer renewCount;

  /** 合同文件URL */
  private String contractFileUrl;

  /** 备注 */
  private String remark;
}
