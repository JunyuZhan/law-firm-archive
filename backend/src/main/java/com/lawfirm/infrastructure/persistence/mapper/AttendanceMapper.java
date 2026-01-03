package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 考勤记录Mapper
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    /**
     * 分页查询考勤记录
     */
    @Select("<script>" +
            "SELECT * FROM attendance WHERE deleted = false " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='startDate != null'> AND attendance_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND attendance_date &lt;= #{endDate} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY attendance_date DESC, id DESC" +
            "</script>")
    IPage<Attendance> selectAttendancePage(Page<Attendance> page,
                                           @Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("status") String status);

    /**
     * 查询用户某日考勤
     */
    @Select("SELECT * FROM attendance WHERE user_id = #{userId} AND attendance_date = #{date} AND deleted = false")
    Attendance selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 统计用户月度考勤
     */
    @Select("SELECT status, COUNT(*) as count FROM attendance " +
            "WHERE user_id = #{userId} AND attendance_date >= #{startDate} AND attendance_date <= #{endDate} AND deleted = false " +
            "GROUP BY status")
    List<Object[]> countMonthlyAttendance(@Param("userId") Long userId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
