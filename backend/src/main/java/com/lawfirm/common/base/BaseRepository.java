package com.lawfirm.common.base;

import com.lawfirm.common.exception.BusinessException;

/**
 * 仓储基础接口 - 定义仓储的基本操作
 * 注意：save、updateById、removeById 等方法由 ServiceImpl 提供
 * 
 * @param <T> 实体类型
 */
public interface BaseRepository<T extends BaseEntity> {

    /**
     * 根据ID查询
     */
    T findById(Long id);

    /**
     * 根据ID查询，不存在抛异常
     */
    default T getByIdOrThrow(Long id, String message) {
        T entity = findById(id);
        if (entity == null) {
            throw new BusinessException(message);
        }
        return entity;
    }
}
