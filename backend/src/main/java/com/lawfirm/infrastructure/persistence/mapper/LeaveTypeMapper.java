package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.LeaveType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 请假类型Mapper */
@Mapper
public interface LeaveTypeMapper extends BaseMapper<LeaveType> {

  /**
   * 查询所有启用的请假类型.
   *
   * @return 启用的请假类型列表
   */
  @Select("SELECT * FROM leave_type WHERE enabled = true AND deleted = false ORDER BY sort_order")
  List<LeaveType> selectEnabledTypes();

  /**
   * 根据编码查询.
   *
   * @param code 请假类型编码
   * @return 请假类型
   */
  @Select("SELECT * FROM leave_type WHERE code = #{code} AND deleted = false")
  LeaveType selectByCode(String code);
}
