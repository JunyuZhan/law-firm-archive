package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Announcement;
import com.lawfirm.infrastructure.persistence.mapper.AnnouncementMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepository extends AbstractRepository<AnnouncementMapper, Announcement> {
}
