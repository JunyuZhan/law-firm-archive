package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同模板Mapper
 */
@Mapper
public interface ContractTemplateMapper extends BaseMapper<ContractTemplate> {
}
