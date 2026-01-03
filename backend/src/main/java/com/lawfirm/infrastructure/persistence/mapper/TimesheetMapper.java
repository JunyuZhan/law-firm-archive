package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Timesheet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时记录Mapper
 */
@Mapper
public interface TimesheetMapper extends BaseMapper<Timesheet> {

    /**
     * 分页查询工时
     */
    @Select("<script>" +
            "SELECT * FROM timesheet WHERE deleted = false " +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='workType != null and workType != \"\"'> AND work_type = #{workType} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='startDate != null'> AND work_date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND work_date &lt;= #{endDate} </if>" +
            "<if test='billable != null'> AND billable = #{billable} </if>" +
            "ORDER BY work_date DESC, created_at DESC" +
            "</script>")
    IPage<Timesheet> selectTimesheetPage(Page<Timesheet> page,
                                         @Param("matterId") Long matterId,
                                         @Param("userId") Long userId,
                                         @Param("workType") String workType,
                                         @Param("status") String status,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("billable") Boolean billable);

    /**
     * 按用户和日期范围查询
     */
    @Select("SELECT * FROM timesheet WHERE user_id = #{userId} AND work_date BETWEEN #{startDate} AND #{endDate} AND deleted = false ORDER BY work_date")
    List<Timesheet> selectByUserAndDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 统计用户某月总工时
     */
    @Select("SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} AND EXTRACT(YEAR FROM work_date) = #{year} AND EXTRACT(MONTH FROM work_date) = #{month} AND status = 'APPROVED' AND deleted = false")
    BigDecimal sumHoursByUserAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    /**
     * 统计案件总工时
     */
    @Select("SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE matter_id = #{matterId} AND status = 'APPROVED' AND deleted = false")
    BigDecimal sumHoursByMatter(@Param("matterId") Long matterId);

    /**
     * 查询待审批工时
     */
    @Select("SELECT * FROM timesheet WHERE status = 'SUBMITTED' AND deleted = false ORDER BY submitted_at ASC")
    List<Timesheet> selectPendingApproval();

    /**
     * 统计用户日期范围内总工时
     */
    @Select("SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} AND work_date BETWEEN #{startDate} AND #{endDate} AND deleted = false")
    BigDecimal sumHoursByUserIdAndDateRange(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 统计用户日期范围内可计费工时
     */
    @Select("SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} AND work_date BETWEEN #{startDate} AND #{endDate} AND billable = true AND deleted = false")
    BigDecimal sumBillableHoursByUserIdAndDateRange(@Param("userId") Long userId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 统计用户日期范围内记录数
     */
    @Select("SELECT COUNT(*) FROM timesheet WHERE user_id = #{userId} AND work_date BETWEEN #{startDate} AND #{endDate} AND deleted = false")
    int countByUserIdAndDateRange(@Param("userId") Long userId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
