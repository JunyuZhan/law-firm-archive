package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Announcement;
import com.lawfirm.infrastructure.persistence.mapper.AnnouncementMapper;
import org.springframework.stereotype.Repository;

/**
 * 公告仓储。
 *
 * <p>提供公告数据的持久化操作。
 */
@Repository
public class AnnouncementRepository extends AbstractRepository<AnnouncementMapper, Announcement> {}
