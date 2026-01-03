package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Backup;
import com.lawfirm.infrastructure.persistence.mapper.BackupMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 备份Repository
 */
@Repository
public class BackupRepository extends AbstractRepository<BackupMapper, Backup> {

    /**
     * 根据备份类型查询
     */
    public List<Backup> findByBackupType(String backupType) {
        LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Backup::getBackupType, backupType)
               .orderByDesc(Backup::getBackupTime);
        return list(wrapper);
    }

    /**
     * 根据状态查询
     */
    public List<Backup> findByStatus(String status) {
        LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Backup::getStatus, status)
               .orderByDesc(Backup::getBackupTime);
        return list(wrapper);
    }

    /**
     * 根据备份编号查询
     */
    public Backup findByBackupNo(String backupNo) {
        LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Backup::getBackupNo, backupNo);
        return getOne(wrapper);
    }
}

