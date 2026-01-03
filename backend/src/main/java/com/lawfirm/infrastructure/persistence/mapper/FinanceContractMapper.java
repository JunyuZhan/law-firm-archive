package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
