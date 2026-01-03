package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.infrastructure.persistence.mapper.NotificationMapper;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository extends AbstractRepository<NotificationMapper, Notification> {
}
