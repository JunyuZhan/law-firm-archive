package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 客户仓储。 */
@Repository
public class ClientRepository extends AbstractRepository<ClientMapper, Client> {

  /**
   * 根据客户编号查询。
   *
   * @param clientNo 客户编号
   * @return 客户
   */
  public Optional<Client> findByClientNo(final String clientNo) {
    return Optional.ofNullable(baseMapper.selectByClientNo(clientNo));
  }

  /**
   * 检查客户编号是否存在。
   *
   * @param clientNo 客户编号
   * @return 是否存在
   */
  public boolean existsByClientNo(final String clientNo) {
    return findByClientNo(clientNo).isPresent();
  }
}
