package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.infrastructure.persistence.mapper.NotificationMapper;
import org.springframework.stereotype.Repository;

/**
 * 通知仓储。
 *
 * <p>提供通知数据的持久化操作。
 */
@Repository
public class NotificationRepository extends AbstractRepository<NotificationMapper, Notification> {}
