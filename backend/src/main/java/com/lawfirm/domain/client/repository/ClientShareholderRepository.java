package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientShareholder;
import com.lawfirm.infrastructure.persistence.mapper.ClientShareholderMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 客户股东信息仓储。 */
@Repository
public class ClientShareholderRepository
    extends AbstractRepository<ClientShareholderMapper, ClientShareholder> {

  /**
   * 根据客户ID查询股东列表。
   *
   * @param clientId 客户ID
   * @return 股东列表
   */
  public List<ClientShareholder> findByClientId(final Long clientId) {
    return baseMapper.selectByClientId(clientId);
  }
}
