package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Backup;
import com.lawfirm.infrastructure.persistence.mapper.BackupMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 备份仓储。
 *
 * <p>提供备份数据的持久化操作。
 */
@Repository
public class BackupRepository extends AbstractRepository<BackupMapper, Backup> {

  /**
   * 根据备份类型查询。
   *
   * @param backupType 备份类型
   * @return 备份列表
   */
  public List<Backup> findByBackupType(final String backupType) {
    LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Backup::getBackupType, backupType).orderByDesc(Backup::getBackupTime);
    return list(wrapper);
  }

  /**
   * 根据状态查询。
   *
   * @param status 状态
   * @return 备份列表
   */
  public List<Backup> findByStatus(final String status) {
    LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Backup::getStatus, status).orderByDesc(Backup::getBackupTime);
    return list(wrapper);
  }

  /**
   * 根据备份编号查询。
   *
   * @param backupNo 备份编号
   * @return 备份信息
   */
  public Backup findByBackupNo(final String backupNo) {
    LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Backup::getBackupNo, backupNo);
    return getOne(wrapper);
  }
}
