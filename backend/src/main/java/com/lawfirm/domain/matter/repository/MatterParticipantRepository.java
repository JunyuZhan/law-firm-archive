package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 案件参与人仓储。
 *
 * <p>提供案件参与人数据的持久化操作。
 */
@Repository
public class MatterParticipantRepository
    extends AbstractRepository<MatterParticipantMapper, MatterParticipant> {

  /**
   * 查询案件的所有参与人。
   *
   * @param matterId 案件ID
   * @return 参与人列表
   */
  public List<MatterParticipant> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 查询案件的主办律师。
   *
   * @param matterId 案件ID
   * @return 主办律师
   */
  public Optional<MatterParticipant> findLeadLawyer(final Long matterId) {
    MatterParticipant lead = baseMapper.selectLeadLawyer(matterId);
    return Optional.ofNullable(lead);
  }

  /**
   * 检查用户是否已在案件团队中。
   *
   * @param matterId 案件ID
   * @param userId 用户ID
   * @return 是否已在团队中
   */
  public boolean existsByMatterIdAndUserId(final Long matterId, final Long userId) {
    int count = baseMapper.countByMatterIdAndUserId(matterId, userId);
    return count > 0;
  }

  /**
   * 删除案件的所有参与人。
   *
   * @param matterId 案件ID
   */
  public void deleteByMatterId(final Long matterId) {
    baseMapper.deleteByMatterId(matterId);
  }
}
