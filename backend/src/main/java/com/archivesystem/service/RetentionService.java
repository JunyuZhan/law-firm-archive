package com.archivesystem.service;

import com.archivesystem.entity.Archive;

import java.util.List;

/**
 * 保管期限服务接口.
 * @author junyuzhan
 */
public interface RetentionService {

    /**
     * 检查即将到期的档案（提前N天提醒）.
     */
    List<Archive> findExpiringArchives(int daysBeforeExpiry);

    /**
     * 检查已到期的档案.
     */
    List<Archive> findExpiredArchives();

    /**
     * 延长档案保管期限.
     */
    void extendRetention(Long archiveId, String newRetentionPeriod, String reason);

    /**
     * 申请档案销毁.
     */
    void applyForDestruction(Long archiveId, String reason);

    /**
     * 执行档案销毁.
     */
    void executeDestruction(Long archiveId, Long approverId, String remarks);
}
