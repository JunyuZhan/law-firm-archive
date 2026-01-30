package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Invoice;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 发票 Mapper */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

  /**
   * 查询收费的所有发票.
   *
   * @param feeId 收费ID
   * @return 发票列表
   */
  @Select(
      "SELECT * FROM finance_invoice WHERE fee_id = #{feeId} AND deleted = false ORDER BY invoice_date DESC")
  List<Invoice> selectByFeeId(@Param("feeId") Long feeId);

  /**
   * 查询合同的所有发票.
   *
   * @param contractId 合同ID
   * @return 发票列表
   */
  @Select(
      "SELECT * FROM finance_invoice WHERE contract_id = #{contractId} AND deleted = false ORDER BY invoice_date DESC")
  List<Invoice> selectByContractId(@Param("contractId") Long contractId);

  /**
   * 统计总开票金额（已开票状态）（M4-034）。
   *
   * @return 总开票金额
   */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM finance_invoice WHERE status = 'ISSUED' AND deleted = false")
  BigDecimal sumTotalInvoiceAmount();

  /**
   * 统计本月开票金额（M4-034）。
   *
   * @return 本月开票金额
   */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM finance_invoice "
          + "WHERE status = 'ISSUED' AND deleted = false "
          + "AND DATE_TRUNC('month', invoice_date) = DATE_TRUNC('month', CURRENT_DATE)")
  BigDecimal sumMonthlyInvoiceAmount();

  /**
   * 统计本年开票金额（M4-034）。
   *
   * @return 本年开票金额
   */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM finance_invoice "
          + "WHERE status = 'ISSUED' AND deleted = false "
          + "AND DATE_TRUNC('year', invoice_date) = DATE_TRUNC('year', CURRENT_DATE)")
  BigDecimal sumYearlyInvoiceAmount();

  /**
   * 按客户统计开票金额（M4-034）。
   *
   * @return 按客户统计结果列表
   */
  @Select(
      "SELECT "
          + "i.client_id, "
          + "c.name as client_name, "
          + "COUNT(*) as invoice_count, "
          + "COALESCE(SUM(i.amount), 0) as total_amount "
          + "FROM finance_invoice i "
          + "LEFT JOIN crm_client c ON i.client_id = c.id "
          + "WHERE i.status = 'ISSUED' AND i.deleted = false "
          + "GROUP BY i.client_id, c.name "
          + "ORDER BY total_amount DESC")
  List<Map<String, Object>> countByClient();

  /**
   * 按发票类型统计开票金额（M4-034）。
   *
   * @return 按发票类型统计结果列表
   */
  @Select(
      "SELECT "
          + "invoice_type, "
          + "COUNT(*) as invoice_count, "
          + "COALESCE(SUM(amount), 0) as total_amount "
          + "FROM finance_invoice "
          + "WHERE status = 'ISSUED' AND deleted = false "
          + "GROUP BY invoice_type "
          + "ORDER BY total_amount DESC")
  List<Map<String, Object>> countByType();

  /**
   * 按状态统计开票金额（M4-034）。
   *
   * @return 按状态统计结果列表
   */
  @Select(
      "SELECT "
          + "status, "
          + "COUNT(*) as invoice_count, "
          + "COALESCE(SUM(amount), 0) as total_amount "
          + "FROM finance_invoice "
          + "WHERE deleted = false "
          + "GROUP BY status "
          + "ORDER BY total_amount DESC")
  List<Map<String, Object>> countByStatus();

  /**
   * 按时间统计开票金额趋势（最近12个月）（M4-034）。
   *
   * @return 开票金额趋势列表
   */
  @Select(
      "SELECT "
          + "TO_CHAR(DATE_TRUNC('month', invoice_date), 'YYYY-MM') as period, "
          + "COUNT(*) as invoice_count, "
          + "COALESCE(SUM(amount), 0) as total_amount "
          + "FROM finance_invoice "
          + "WHERE status = 'ISSUED' AND deleted = false "
          + "AND invoice_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months') "
          + "GROUP BY DATE_TRUNC('month', invoice_date) "
          + "ORDER BY period")
  List<Map<String, Object>> countByDate();
}
