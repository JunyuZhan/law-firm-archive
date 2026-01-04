package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.finance.entity.Prepayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 预收款 Mapper
 */
@Mapper
public interface PrepaymentMapper extends BaseMapper<Prepayment> {

    /**
     * 查询客户的有效预收款（有剩余金额）
     */
    @Select("SELECT * FROM finance_prepayment WHERE client_id = #{clientId} " +
            "AND status = 'ACTIVE' AND remaining_amount > 0 AND deleted = false " +
            "ORDER BY receipt_date ASC")
    List<Prepayment> selectActiveByClientId(@Param("clientId") Long clientId);

    /**
     * 查询项目关联的预收款
     */
    @Select("SELECT * FROM finance_prepayment WHERE matter_id = #{matterId} AND deleted = false ORDER BY receipt_date DESC")
    List<Prepayment> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询合同关联的预收款
     */
    @Select("SELECT * FROM finance_prepayment WHERE contract_id = #{contractId} AND deleted = false ORDER BY receipt_date DESC")
    List<Prepayment> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 分页查询预收款
     */
    @Select("<script>" +
            "SELECT * FROM finance_prepayment WHERE deleted = false " +
            "<if test='clientId != null'> AND client_id = #{clientId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='contractId != null'> AND contract_id = #{contractId} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='prepaymentNo != null and prepaymentNo != \"\"'> AND prepayment_no LIKE '%' || #{prepaymentNo} || '%' </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<Prepayment> selectPrepaymentPage(Page<Prepayment> page,
                                            @Param("clientId") Long clientId,
                                            @Param("matterId") Long matterId,
                                            @Param("contractId") Long contractId,
                                            @Param("status") String status,
                                            @Param("prepaymentNo") String prepaymentNo);
}
