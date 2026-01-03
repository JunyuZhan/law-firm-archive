package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.Contact;
import com.lawfirm.infrastructure.persistence.mapper.ContactMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 联系人 Repository
 */
@Repository
public class ContactRepository extends AbstractRepository<ContactMapper, Contact> {

    /**
     * 根据客户ID查询联系人列表
     */
    public List<Contact> findByClientId(Long clientId) {
        return baseMapper.selectByClientId(clientId);
    }

    /**
     * 查询客户的主要联系人
     */
    public Optional<Contact> findPrimaryByClientId(Long clientId) {
        Contact contact = baseMapper.selectPrimaryByClientId(clientId);
        return Optional.ofNullable(contact);
    }

    /**
     * 取消客户的所有主要联系人标记
     */
    public void clearPrimaryByClientId(Long clientId) {
        baseMapper.clearPrimaryByClientId(clientId);
    }
}

