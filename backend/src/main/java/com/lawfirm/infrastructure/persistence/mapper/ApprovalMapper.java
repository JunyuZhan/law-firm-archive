package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.workbench.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批记录 Mapper
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {

    /**
     * 分页查询审批记录
     */
    @Select("<script>" +
            "SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='businessType != null and businessType != \"\"'> AND business_type = #{businessType} </if>" +
            "<if test='applicantId != null'> AND applicant_id = #{applicantId} </if>" +
            "<if test='approverId != null'> AND approver_id = #{approverId} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Approval> selectApprovalPage(@Param("status") String status,
                                      @Param("businessType") String businessType,
                                      @Param("applicantId") Long applicantId,
                                      @Param("approverId") Long approverId);

    /**
     * 查询待审批列表（按审批人）
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND approver_id = #{approverId} " +
            "AND status = 'PENDING' " +
            "ORDER BY " +
            "CASE urgency WHEN 'URGENT' THEN 1 ELSE 2 END, " +
            "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, " +
            "created_at ASC")
    List<Approval> selectPendingApprovals(@Param("approverId") Long approverId);

    /**
     * 查询我发起的审批
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND applicant_id = #{applicantId} " +
            "ORDER BY created_at DESC")
    List<Approval> selectMyInitiatedApprovals(@Param("applicantId") Long applicantId);

    /**
     * 根据业务类型和业务ID查询审批记录
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND business_type = #{businessType} " +
            "AND business_id = #{businessId} " +
            "ORDER BY created_at DESC")
    List<Approval> selectByBusiness(@Param("businessType") String businessType,
                                     @Param("businessId") Long businessId);

    /**
     * 统计用户待审批数量
     */
    @Select("SELECT COUNT(*) FROM workbench_approval WHERE approver_id = #{approverId} AND status = 'PENDING' AND deleted = false")
    int countPendingByApproverId(@Param("approverId") Long approverId);

    /**
     * 查询用户待审批列表（限制数量）
     */
    @Select("SELECT * FROM workbench_approval WHERE approver_id = #{approverId} AND status = 'PENDING' AND deleted = false ORDER BY created_at DESC LIMIT #{limit}")
    List<Approval> selectPendingByApproverId(@Param("approverId") Long approverId, @Param("limit") int limit);
}

