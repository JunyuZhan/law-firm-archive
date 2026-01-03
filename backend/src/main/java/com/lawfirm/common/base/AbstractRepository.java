package com.lawfirm.common.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.common.exception.BusinessException;

/**
 * 仓储抽象基类 - 所有Repository实现继承此类
 * 继承 ServiceImpl 获得 save、updateById、removeById 等方法
 * 
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
public abstract class AbstractRepository<M extends BaseMapper<T>, T extends BaseEntity> 
        extends ServiceImpl<M, T> implements BaseRepository<T> {

    /**
     * 根据ID查询，不存在返回null
     */
    @Override
    public T findById(Long id) {
        return getById(id);
    }

    /**
     * 根据ID查询，不存在抛异常
     */
    @Override
    public T getByIdOrThrow(Long id, String message) {
        T entity = getById(id);
        if (entity == null) {
            throw new BusinessException(message);
        }
        return entity;
    }

    /**
     * 逻辑删除
     */
    public boolean softDelete(Long id) {
        return removeById(id);
    }
}

