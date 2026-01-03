package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientRelatedCompany;
import com.lawfirm.infrastructure.persistence.mapper.ClientRelatedCompanyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 客户关联企业仓储
 */
@Repository
public class ClientRelatedCompanyRepository extends AbstractRepository<ClientRelatedCompanyMapper, ClientRelatedCompany> {

    /**
     * 根据客户ID查询关联企业列表
     */
    public List<ClientRelatedCompany> findByClientId(Long clientId) {
        return baseMapper.selectByClientId(clientId);
    }

    /**
     * 根据客户ID和类型查询
     */
    public List<ClientRelatedCompany> findByClientIdAndType(Long clientId, String type) {
        return baseMapper.selectByClientIdAndType(clientId, type);
    }
}

