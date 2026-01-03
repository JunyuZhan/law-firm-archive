package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.LeaveApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请Mapper
 */
@Mapper
public interface LeaveApplicationMapper extends BaseMapper<LeaveApplication> {

    /**
     * 分页查询请假申请
     */
    @Select("<script>" +
            "SELECT * FROM leave_application WHERE deleted = false " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='leaveTypeId != null'> AND leave_type_id = #{leaveTypeId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND start_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND end_time &lt;= #{endTime} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<LeaveApplication> selectApplicationPage(Page<LeaveApplication> page,
                                                   @Param("userId") Long userId,
                                                   @Param("leaveTypeId") Long leaveTypeId,
                                                   @Param("status") String status,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待审批的请假申请
     */
    @Select("SELECT * FROM leave_application WHERE status = 'PENDING' AND deleted = false ORDER BY created_at")
    List<LeaveApplication> selectPendingApplications();

    /**
     * 检查时间段是否有重叠的请假
     */
    @Select("SELECT COUNT(*) FROM leave_application " +
            "WHERE user_id = #{userId} AND status IN ('PENDING', 'APPROVED') AND deleted = false " +
            "AND ((start_time <= #{startTime} AND end_time > #{startTime}) " +
            "OR (start_time < #{endTime} AND end_time >= #{endTime}) " +
            "OR (start_time >= #{startTime} AND end_time <= #{endTime}))")
    int countOverlapping(@Param("userId") Long userId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
}
