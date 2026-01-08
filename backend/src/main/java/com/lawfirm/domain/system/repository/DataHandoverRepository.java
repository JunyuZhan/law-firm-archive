package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.DataHandover;
import com.lawfirm.infrastructure.persistence.mapper.DataHandoverMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据交接记录仓储
 */
@Repository
public class DataHandoverRepository extends ServiceImpl<DataHandoverMapper, DataHandover> {

    /**
     * 根据ID查询，不存在则抛出异常
     */
    public DataHandover getByIdOrThrow(Long id, String errorMessage) {
        DataHandover handover = getById(id);
        if (handover == null || Boolean.TRUE.equals(handover.getDeleted())) {
            throw new BusinessException(errorMessage);
        }
        return handover;
    }

    /**
     * 根据交接单号查询
     */
    public Optional<DataHandover> findByHandoverNo(String handoverNo) {
        return Optional.ofNullable(baseMapper.selectByHandoverNo(handoverNo));
    }

    /**
     * 根据移交人ID查询
     */
    public List<DataHandover> findByFromUserId(Long fromUserId) {
        return baseMapper.selectByFromUserId(fromUserId);
    }

    /**
     * 根据接收人ID查询
     */
    public List<DataHandover> findByToUserId(Long toUserId) {
        return baseMapper.selectByToUserId(toUserId);
    }

    /**
     * 分页查询
     */
    public IPage<DataHandover> findPage(Page<DataHandover> page, Long fromUserId, Long toUserId,
                                         String handoverType, String status) {
        return baseMapper.selectHandoverPage(page, fromUserId, toUserId, handoverType, status);
    }

    /**
     * 软删除
     */
    public void softDelete(Long id) {
        DataHandover handover = getById(id);
        if (handover != null) {
            handover.setDeleted(true);
            updateById(handover);
        }
    }
}

