package com.lawfirm.domain.client.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import com.lawfirm.infrastructure.persistence.mapper.ClientContactRecordMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户联系记录仓储
 */
@Repository
public class ClientContactRecordRepository extends AbstractRepository<ClientContactRecordMapper, ClientContactRecord> {

    /**
     * 根据客户ID分页查询联系记录
     */
    public IPage<ClientContactRecord> findByClientId(Page<ClientContactRecord> page, Long clientId) {
        return baseMapper.selectByClientId(page, clientId);
    }

    /**
     * 根据客户ID查询所有联系记录
     */
    public List<ClientContactRecord> findAllByClientId(Long clientId) {
        return baseMapper.selectAllByClientId(clientId);
    }

    /**
     * 查询需要跟进的联系记录
     */
    public List<ClientContactRecord> findFollowUpRecords(LocalDate date) {
        return baseMapper.selectFollowUpRecords(date);
    }
}

