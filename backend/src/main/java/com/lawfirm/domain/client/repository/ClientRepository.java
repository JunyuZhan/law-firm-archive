package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 客户仓储
 */
@Repository
public class ClientRepository extends AbstractRepository<ClientMapper, Client> {

    /**
     * 根据客户编号查询
     */
    public Optional<Client> findByClientNo(String clientNo) {
        return Optional.ofNullable(baseMapper.selectByClientNo(clientNo));
    }

    /**
     * 检查客户编号是否存在
     */
    public boolean existsByClientNo(String clientNo) {
        return findByClientNo(clientNo).isPresent();
    }
}
