package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ConflictCheckItem;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 利冲检查项 Mapper */
@Mapper
public interface ConflictCheckItemMapper extends BaseMapper<ConflictCheckItem> {

  /**
   * 根据检查ID查询所有检查项.
   *
   * @param checkId 检查ID
   * @return 利冲检查项列表
   */
  @Select(
      "SELECT * FROM crm_conflict_check_item WHERE check_id = #{checkId} AND deleted = false ORDER BY id")
  List<ConflictCheckItem> selectByCheckId(@Param("checkId") Long checkId);

  /**
   * 删除检查的所有检查项.
   *
   * @param checkId 检查ID
   */
  @Delete("DELETE FROM crm_conflict_check_item WHERE check_id = #{checkId}")
  void deleteByCheckId(@Param("checkId") Long checkId);
}
