package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientRelatedCompany;
import com.lawfirm.infrastructure.persistence.mapper.ClientRelatedCompanyMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 客户关联企业仓储。 */
@Repository
public class ClientRelatedCompanyRepository
    extends AbstractRepository<ClientRelatedCompanyMapper, ClientRelatedCompany> {

  /**
   * 根据客户ID查询关联企业列表。
   *
   * @param clientId 客户ID
   * @return 关联企业列表
   */
  public List<ClientRelatedCompany> findByClientId(final Long clientId) {
    return baseMapper.selectByClientId(clientId);
  }

  /**
   * 根据客户ID和类型查询。
   *
   * @param clientId 客户ID
   * @param type 类型
   * @return 关联企业列表
   */
  public List<ClientRelatedCompany> findByClientIdAndType(final Long clientId, final String type) {
    return baseMapper.selectByClientIdAndType(clientId, type);
  }
}
