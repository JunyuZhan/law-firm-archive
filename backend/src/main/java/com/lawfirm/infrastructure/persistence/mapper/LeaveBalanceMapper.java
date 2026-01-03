package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.LeaveBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 假期余额Mapper
 */
@Mapper
public interface LeaveBalanceMapper extends BaseMapper<LeaveBalance> {

    /**
     * 查询用户某年度的假期余额
     */
    @Select("SELECT * FROM leave_balance WHERE user_id = #{userId} AND year = #{year} AND deleted = false")
    List<LeaveBalance> selectByUserAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 查询用户某类型某年度的假期余额
     */
    @Select("SELECT * FROM leave_balance WHERE user_id = #{userId} AND leave_type_id = #{leaveTypeId} AND year = #{year} AND deleted = false")
    LeaveBalance selectByUserTypeYear(@Param("userId") Long userId,
                                      @Param("leaveTypeId") Long leaveTypeId,
                                      @Param("year") Integer year);

    /**
     * 扣减假期余额
     */
    @Update("UPDATE leave_balance SET used_days = used_days + #{days}, remaining_days = remaining_days - #{days}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND leave_type_id = #{leaveTypeId} AND year = #{year} AND remaining_days >= #{days}")
    int deductBalance(@Param("userId") Long userId,
                      @Param("leaveTypeId") Long leaveTypeId,
                      @Param("year") Integer year,
                      @Param("days") BigDecimal days);

    /**
     * 恢复假期余额
     */
    @Update("UPDATE leave_balance SET used_days = used_days - #{days}, remaining_days = remaining_days + #{days}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND leave_type_id = #{leaveTypeId} AND year = #{year}")
    int restoreBalance(@Param("userId") Long userId,
                       @Param("leaveTypeId") Long leaveTypeId,
                       @Param("year") Integer year,
                       @Param("days") BigDecimal days);
}
