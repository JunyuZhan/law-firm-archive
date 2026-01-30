package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.DataHandover;
import com.lawfirm.infrastructure.persistence.mapper.DataHandoverMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 数据交接记录仓储。
 *
 * <p>提供数据交接记录的持久化操作。
 */
@Repository
public class DataHandoverRepository extends ServiceImpl<DataHandoverMapper, DataHandover> {

  /**
   * 根据ID查询，不存在则抛出异常。
   *
   * @param id 数据交接记录ID
   * @param errorMessage 错误信息
   * @return 数据交接记录
   */
  public DataHandover getByIdOrThrow(final Long id, final String errorMessage) {
    DataHandover handover = getById(id);
    if (handover == null || Boolean.TRUE.equals(handover.getDeleted())) {
      throw new BusinessException(errorMessage);
    }
    return handover;
  }

  /**
   * 根据交接单号查询。
   *
   * @param handoverNo 交接单号
   * @return 数据交接记录
   */
  public Optional<DataHandover> findByHandoverNo(final String handoverNo) {
    return Optional.ofNullable(baseMapper.selectByHandoverNo(handoverNo));
  }

  /**
   * 根据移交人ID查询。
   *
   * @param fromUserId 移交人ID
   * @return 数据交接记录列表
   */
  public List<DataHandover> findByFromUserId(final Long fromUserId) {
    return baseMapper.selectByFromUserId(fromUserId);
  }

  /**
   * 根据接收人ID查询。
   *
   * @param toUserId 接收人ID
   * @return 数据交接记录列表
   */
  public List<DataHandover> findByToUserId(final Long toUserId) {
    return baseMapper.selectByToUserId(toUserId);
  }

  /**
   * 分页查询。
   *
   * @param page 分页对象
   * @param fromUserId 移交人ID
   * @param toUserId 接收人ID
   * @param handoverType 交接类型
   * @param status 状态
   * @return 分页结果
   */
  public IPage<DataHandover> findPage(
      final Page<DataHandover> page,
      final Long fromUserId,
      final Long toUserId,
      final String handoverType,
      final String status) {
    return baseMapper.selectHandoverPage(page, fromUserId, toUserId, handoverType, status);
  }

  /**
   * 软删除。
   *
   * @param id 数据交接记录ID
   */
  public void softDelete(final Long id) {
    DataHandover handover = getById(id);
    if (handover != null) {
      handover.setDeleted(true);
      updateById(handover);
    }
  }
}
