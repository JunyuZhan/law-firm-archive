package com.lawfirm.domain.client.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientTag;
import com.lawfirm.infrastructure.persistence.mapper.ClientTagMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 客户标签仓储。 */
@Repository
public class ClientTagRepository extends AbstractRepository<ClientTagMapper, ClientTag> {

  /**
   * 根据标签名称查询。
   *
   * @param tagName 标签名称
   * @return 客户标签
   */
  public Optional<ClientTag> findByTagName(final String tagName) {
    return baseMapper
        .selectList(
            Wrappers.<ClientTag>lambdaQuery()
                .eq(ClientTag::getTagName, tagName)
                .eq(ClientTag::getDeleted, false))
        .stream()
        .findFirst();
  }

  /**
   * 检查标签名称是否存在。
   *
   * @param tagName 标签名称
   * @return 是否存在
   */
  public boolean existsByTagName(final String tagName) {
    return findByTagName(tagName).isPresent();
  }
}
