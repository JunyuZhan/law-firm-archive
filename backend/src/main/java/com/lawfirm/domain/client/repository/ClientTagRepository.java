package com.lawfirm.domain.client.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientTag;
import com.lawfirm.infrastructure.persistence.mapper.ClientTagMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 客户标签仓储
 */
@Repository
public class ClientTagRepository extends AbstractRepository<ClientTagMapper, ClientTag> {

    /**
     * 根据标签名称查询
     */
    public Optional<ClientTag> findByTagName(String tagName) {
        return baseMapper.selectList(
                Wrappers.<ClientTag>lambdaQuery()
                        .eq(ClientTag::getTagName, tagName)
                        .eq(ClientTag::getDeleted, false)
        ).stream().findFirst();
    }

    /**
     * 检查标签名称是否存在
     */
    public boolean existsByTagName(String tagName) {
        return findByTagName(tagName).isPresent();
    }
}

