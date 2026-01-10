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

    /**
     * 查询我审批过的记录（已通过或已拒绝）
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND approver_id = #{approverId} " +
            "AND status IN ('APPROVED', 'REJECTED') " +
            "ORDER BY approved_at DESC")
    List<Approval> selectMyApprovedHistory(@Param("approverId") Long approverId);

    /**
     * 根据数据范围查询审批历史（已完成的审批）
     * ALL: 查看所有已完成审批
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND status IN ('APPROVED', 'REJECTED') " +
            "ORDER BY approved_at DESC")
    List<Approval> selectAllApprovalHistory();

    /**
     * 根据部门ID列表查询审批历史
     * 通过申请人的部门进行过滤
     */
    @Select("<script>" +
            "SELECT a.* FROM workbench_approval a " +
            "INNER JOIN sys_user u ON a.applicant_id = u.id " +
            "WHERE a.deleted = false " +
            "AND a.status IN ('APPROVED', 'REJECTED') " +
            "AND u.department_id IN " +
            "<foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>" +
            "#{deptId}" +
            "</foreach> " +
            "ORDER BY a.approved_at DESC" +
            "</script>")
    List<Approval> selectApprovalHistoryByDeptIds(@Param("deptIds") List<Long> deptIds);

    /**
     * 查询自己相关的审批历史（自己发起或自己审批的）
     */
    @Select("SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "AND status IN ('APPROVED', 'REJECTED') " +
            "AND (applicant_id = #{userId} OR approver_id = #{userId}) " +
            "ORDER BY approved_at DESC")
    List<Approval> selectSelfApprovalHistory(@Param("userId") Long userId);

    /**
     * 查询直接子部门ID
     */
    @Select("SELECT id FROM sys_department WHERE parent_id = #{parentId} AND deleted = false")
    List<Long> selectChildDeptIds(@Param("parentId") Long parentId);

    /**
     * ✅ 修复问题556: 使用递归CTE一次性查询所有后代部门ID
     * 避免递归Java调用导致的多次数据库查询
     */
    @Select("WITH RECURSIVE dept_tree AS (" +
            "  SELECT id FROM sys_department WHERE parent_id = #{parentId} AND deleted = false" +
            "  UNION ALL" +
            "  SELECT d.id FROM sys_department d" +
            "  INNER JOIN dept_tree dt ON d.parent_id = dt.id" +
            "  WHERE d.deleted = false" +
            ") SELECT id FROM dept_tree")
    List<Long> selectAllDescendantDeptIds(@Param("parentId") Long parentId);

    /**
     * 分页查询审批记录（带权限过滤，在数据库层面分页）
     * @param status 状态
     * @param businessType 业务类型
     * @param applicantId 申请人ID
     * @param approverId 审批人ID
     * @param currentUserId 当前用户ID（用于非管理员权限过滤）
     * @param isAdmin 是否为管理员/主任（管理员可查看全部）
     * @param offset 分页偏移量
     * @param pageSize 每页大小
     */
    @Select("<script>" +
            "SELECT * FROM workbench_approval " +
            "WHERE deleted = false " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='businessType != null and businessType != \"\"'> AND business_type = #{businessType} </if>" +
            "<if test='applicantId != null'> AND applicant_id = #{applicantId} </if>" +
            "<if test='approverId != null'> AND approver_id = #{approverId} </if>" +
            "<if test='!isAdmin'> AND (applicant_id = #{currentUserId} OR approver_id = #{currentUserId}) </if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<Approval> selectApprovalPageWithPermission(
            @Param("status") String status,
            @Param("businessType") String businessType,
            @Param("applicantId") Long applicantId,
            @Param("approverId") Long approverId,
            @Param("currentUserId") Long currentUserId,
            @Param("isAdmin") boolean isAdmin,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    /**
     * 统计审批记录总数（带权限过滤）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM workbench_approval " +
            "WHERE deleted = false " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='businessType != null and businessType != \"\"'> AND business_type = #{businessType} </if>" +
            "<if test='applicantId != null'> AND applicant_id = #{applicantId} </if>" +
            "<if test='approverId != null'> AND approver_id = #{approverId} </if>" +
            "<if test='!isAdmin'> AND (applicant_id = #{currentUserId} OR approver_id = #{currentUserId}) </if>" +
            "</script>")
    long countApprovalWithPermission(
            @Param("status") String status,
            @Param("businessType") String businessType,
            @Param("applicantId") Long applicantId,
            @Param("approverId") Long approverId,
            @Param("currentUserId") Long currentUserId,
            @Param("isAdmin") boolean isAdmin);
}

