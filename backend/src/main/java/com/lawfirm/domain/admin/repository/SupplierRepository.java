package com.lawfirm.domain.admin.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.infrastructure.persistence.mapper.SupplierMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** 供应商仓储 */
@Repository
public class SupplierRepository extends AbstractRepository<SupplierMapper, Supplier> {

  /**
   * 分页查询供应商
   *
   * @param page 分页参数
   * @param keyword 关键词
   * @param supplierType 供应商类型
   * @param status 状态
   * @param rating 评级
   * @return 供应商分页结果
   */
  public IPage<Supplier> findPage(
      final Page<Supplier> page,
      final String keyword,
      final String supplierType,
      final String status,
      final String rating) {
    return baseMapper.findPage(page, keyword, supplierType, status, rating);
  }

  /**
   * 按状态统计供应商数量
   *
   * @return 统计结果列表
   */
  public List<Map<String, Object>> countByStatus() {
    return baseMapper.countByStatus();
  }

  /**
   * 按评级统计供应商数量
   *
   * @return 统计结果列表
   */
  public List<Map<String, Object>> countByRating() {
    return baseMapper.countByRating();
  }
}
