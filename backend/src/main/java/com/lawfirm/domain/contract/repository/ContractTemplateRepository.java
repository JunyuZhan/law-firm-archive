package com.lawfirm.domain.contract.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.infrastructure.persistence.mapper.ContractTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 合同模板仓储
 */
@Repository
public class ContractTemplateRepository extends AbstractRepository<ContractTemplateMapper, ContractTemplate> {

    /**
     * 获取所有启用的模板
     */
    public List<ContractTemplate> findActiveTemplates() {
        return list(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, "ACTIVE")
                .eq(ContractTemplate::getDeleted, false)
                .orderByAsc(ContractTemplate::getSortOrder));
    }

    /**
     * 获取所有模板
     */
    public List<ContractTemplate> findAllTemplates() {
        return list(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getDeleted, false)
                .orderByAsc(ContractTemplate::getSortOrder));
    }

    /**
     * 按合同类型获取模板
     */
    public List<ContractTemplate> findByContractType(String contractType) {
        return list(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getContractType, contractType)
                .eq(ContractTemplate::getStatus, "ACTIVE")
                .eq(ContractTemplate::getDeleted, false)
                .orderByAsc(ContractTemplate::getSortOrder));
    }

    /**
     * 生成模板编号
     */
    public String generateTemplateNo() {
        Long count = count(new LambdaQueryWrapper<ContractTemplate>());
        return String.format("CT-%03d", count + 1);
    }
}
