package com.lawfirm.domain.client.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import com.lawfirm.infrastructure.persistence.mapper.ClientContactRecordMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 客户联系记录仓储。 */
@Repository
public class ClientContactRecordRepository
    extends AbstractRepository<ClientContactRecordMapper, ClientContactRecord> {

  /**
   * 根据客户ID分页查询联系记录。
   *
   * @param page 分页对象
   * @param clientId 客户ID
   * @return 联系记录分页结果
   */
  public IPage<ClientContactRecord> findByClientId(
      final Page<ClientContactRecord> page, final Long clientId) {
    return baseMapper.selectByClientId(page, clientId);
  }

  /**
   * 根据客户ID查询所有联系记录。
   *
   * @param clientId 客户ID
   * @return 联系记录列表
   */
  public List<ClientContactRecord> findAllByClientId(final Long clientId) {
    return baseMapper.selectAllByClientId(clientId);
  }

  /**
   * 查询需要跟进的联系记录。
   *
   * @param date 日期
   * @return 需要跟进的联系记录列表
   */
  public List<ClientContactRecord> findFollowUpRecords(final LocalDate date) {
    return baseMapper.selectFollowUpRecords(date);
  }

  /**
   * 复杂查询联系记录（支持多条件筛选）。
   *
   * @param page 分页对象
   * @param clientId 客户ID
   * @param contactId 联系人ID
   * @param contactMethod 联系方式
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param followUpReminder 是否需要跟进提醒
   * @return 联系记录分页结果
   */
  public IPage<ClientContactRecord> findByConditions(
      final Page<ClientContactRecord> page,
      final Long clientId,
      final Long contactId,
      final String contactMethod,
      final LocalDate startDate,
      final LocalDate endDate,
      final Boolean followUpReminder) {
    return baseMapper.selectByConditions(
        page, clientId, contactId, contactMethod, startDate, endDate, followUpReminder);
  }
}
