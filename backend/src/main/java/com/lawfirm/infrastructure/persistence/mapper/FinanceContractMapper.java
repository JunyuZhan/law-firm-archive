package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 委托合同 Mapper（财务模块）
 */
@Mapper
public interface FinanceContractMapper extends BaseMapper<Contract> {

    /**
     * 根据合同编号查询
     */
    @Select("SELECT * FROM finance_contract WHERE contract_no = #{contractNo} AND deleted = false")
    Contract selectByContractNo(@Param("contractNo") String contractNo);

    /**
     * 根据案件ID查询合同
     */
    @Select("SELECT * FROM finance_contract WHERE matter_id = #{matterId} AND deleted = false LIMIT 1")
    Contract selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 统计指定日期创建的合同数量
     * 只统计已审批通过的合同（ACTIVE状态），只有真正生效的合同才占用编号位置
     * 草稿状态（DRAFT）和已拒绝状态（REJECTED）不占用编号位置
     */
    @Select("SELECT COUNT(*) FROM finance_contract WHERE DATE(created_at) = #{date} AND status = 'ACTIVE' AND deleted = false")
    long countByCreatedDate(@Param("date") LocalDate date);

    /**
     * 统计指定年份创建的合同数量
     * 只统计已审批通过的合同（ACTIVE状态），只有真正生效的合同才占用编号位置
     * 草稿状态（DRAFT）和已拒绝状态（REJECTED）不占用编号位置
     */
    @Select("SELECT COUNT(*) FROM finance_contract WHERE EXTRACT(YEAR FROM created_at) = #{year} AND status = 'ACTIVE' AND deleted = false")
    long countByCreatedYear(@Param("year") int year);

    /**
     * 统计指定日期和合同类型创建的合同数量（用于独立编号）
     * 只统计已审批通过的合同（ACTIVE状态），只有真正生效的合同才占用编号位置
     * 草稿状态（DRAFT）和已拒绝状态（REJECTED）不占用编号位置
     */
    @Select("SELECT COUNT(*) FROM finance_contract WHERE DATE(created_at) = #{date} AND contract_type = #{contractType} AND status = 'ACTIVE' AND deleted = false")
    long countByCreatedDateAndContractType(@Param("date") LocalDate date, @Param("contractType") String contractType);

    /**
     * 统计指定年份和合同类型创建的合同数量（用于独立编号）
     * 只统计已审批通过的合同（ACTIVE状态），只有真正生效的合同才占用编号位置
     * 草稿状态（DRAFT）和已拒绝状态（REJECTED）不占用编号位置
     */
    @Select("SELECT COUNT(*) FROM finance_contract WHERE EXTRACT(YEAR FROM created_at) = #{year} AND contract_type = #{contractType} AND status = 'ACTIVE' AND deleted = false")
    long countByCreatedYearAndContractType(@Param("year") int year, @Param("contractType") String contractType);

    /**
     * 根据ID查询合同并加行锁（用于并发控制）
     * 使用 SELECT ... FOR UPDATE 实现悲观锁
     */
    @Select("SELECT * FROM finance_contract WHERE id = #{id} AND deleted = false FOR UPDATE")
    Contract selectByIdForUpdate(@Param("id") Long id);

    /**
     * 查询即将到期的合同（问题255：合同到期提醒功能）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 在指定日期范围内到期的有效合同列表
     */
    @Select("SELECT * FROM finance_contract WHERE status = 'ACTIVE' AND deleted = false " +
            "AND expiry_date >= #{startDate} AND expiry_date <= #{endDate} " +
            "ORDER BY expiry_date ASC")
    List<Contract> selectExpiringContracts(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);

    /**
     * 查询已过期的合同
     * @param date 当前日期
     * @return 已过期但状态仍为ACTIVE的合同列表
     */
    @Select("SELECT * FROM finance_contract WHERE status = 'ACTIVE' AND deleted = false " +
            "AND expiry_date < #{date} ORDER BY expiry_date ASC")
    List<Contract> selectExpiredContracts(@Param("date") LocalDate date);

    /**
     * 统计使用指定模板的合同数量
     * ✅ 修复问题585/586: 支持检查模板使用情况
     */
    @Select("SELECT COUNT(*) FROM finance_contract WHERE template_id = #{templateId} AND deleted = false")
    long countByTemplateId(@Param("templateId") Long templateId);
}
