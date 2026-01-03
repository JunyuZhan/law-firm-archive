package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.infrastructure.persistence.mapper.SupplierMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 供应商仓储
 */
@Repository
public class SupplierRepository extends AbstractRepository<SupplierMapper, Supplier> {

    public IPage<Supplier> findPage(Page<Supplier> page, String keyword, String supplierType, 
                                    String status, String rating) {
        return baseMapper.findPage(page, keyword, supplierType, status, rating);
    }

    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus();
    }

    public List<Map<String, Object>> countByRating() {
        return baseMapper.countByRating();
    }
}
