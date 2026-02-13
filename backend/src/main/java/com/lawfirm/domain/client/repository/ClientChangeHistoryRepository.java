package com.lawfirm.domain.client.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientChangeHistory;
import com.lawfirm.infrastructure.persistence.mapper.ClientChangeHistoryMapper;
import org.springframework.stereotype.Repository;

/** 企业变更历史仓储（M2-014）。 */
@Repository
public class ClientChangeHistoryRepository
    extends AbstractRepository<ClientChangeHistoryMapper, ClientChangeHistory> {}
