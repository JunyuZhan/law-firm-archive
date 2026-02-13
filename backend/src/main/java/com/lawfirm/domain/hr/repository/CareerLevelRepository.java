package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.infrastructure.persistence.mapper.CareerLevelMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 职级通道 Repository. */
@Repository
public class CareerLevelRepository extends AbstractRepository<CareerLevelMapper, CareerLevel> {

  /**
   * 按类别查询职级列表.
   *
   * @param category 类别
   * @return 职级列表
   */
  public List<CareerLevel> findByCategory(final String category) {
    return baseMapper.selectByCategory(category);
  }

  /**
   * 查询下一级职级.
   *
   * @param category 类别
   * @param currentOrder 当前职级顺序
   * @return 下一级职级
   */
  public Optional<CareerLevel> findNextLevel(final String category, final Integer currentOrder) {
    return Optional.ofNullable(baseMapper.selectNextLevel(category, currentOrder));
  }

  /**
   * 根据编码查询.
   *
   * @param levelCode 职级编码
   * @return 职级
   */
  public Optional<CareerLevel> findByLevelCode(final String levelCode) {
    return Optional.ofNullable(baseMapper.selectByLevelCode(levelCode));
  }
}
