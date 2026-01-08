package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.document.entity.SealApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用印申请Mapper
 */
@Mapper
public interface SealApplicationMapper extends BaseMapper<SealApplication> {

    /**
     * 分页查询用印申请
     */
    @Select("<script>" +
            "SELECT sa.* FROM seal_application sa " +
            "<if test='keeperId != null'> " +
            "INNER JOIN seal_info si ON sa.seal_id = si.id " +
            "</if>" +
            "WHERE sa.deleted = false " +
            "<if test='applicantId != null'> AND sa.applicant_id = #{applicantId} </if>" +
            "<if test='sealId != null'> AND sa.seal_id = #{sealId} </if>" +
            "<if test='matterId != null'> AND sa.matter_id = #{matterId} </if>" +
            "<if test='status != null and status != \"\"'> AND sa.status = #{status} </if>" +
            "<if test='keeperId != null'> AND si.keeper_id = #{keeperId} AND si.deleted = false </if>" +
            "ORDER BY sa.created_at DESC" +
            "</script>")
    IPage<SealApplication> selectApplicationPage(Page<SealApplication> page,
                                                 @Param("applicantId") Long applicantId,
                                                 @Param("sealId") Long sealId,
                                                 @Param("matterId") Long matterId,
                                                 @Param("status") String status,
                                                 @Param("keeperId") Long keeperId);

    /**
     * 查询待审批的申请
     */
    @Select("SELECT * FROM seal_application WHERE status = 'PENDING' AND deleted = false ORDER BY created_at ASC")
    java.util.List<SealApplication> selectPendingApplications();

    /**
     * 统计印章使用次数
     */
    @Select("SELECT COUNT(*) FROM seal_application WHERE seal_id = #{sealId} AND status = 'USED' AND deleted = false")
    int countUsageBySeaId(@Param("sealId") Long sealId);

    /**
     * 统计印章待处理的申请数量
     */
    @Select("SELECT COUNT(*) FROM seal_application WHERE seal_id = #{sealId} AND status IN ('PENDING', 'APPROVED') AND deleted = false")
    int countPendingBySealId(@Param("sealId") Long sealId);

    /**
     * 查询保管人待办理的申请（审批通过且印章的保管人是当前用户）
     */
    @Select("SELECT sa.* FROM seal_application sa " +
            "INNER JOIN seal_info si ON sa.seal_id = si.id " +
            "WHERE sa.status = 'APPROVED' AND sa.deleted = false " +
            "AND si.keeper_id = #{keeperId} AND si.deleted = false " +
            "ORDER BY sa.approved_at ASC, sa.created_at ASC")
    java.util.List<SealApplication> selectPendingForKeeper(@Param("keeperId") Long keeperId);

    /**
     * 查询保管人已办理的申请（已用印且印章的保管人是当前用户）
     */
    @Select("SELECT sa.* FROM seal_application sa " +
            "INNER JOIN seal_info si ON sa.seal_id = si.id " +
            "WHERE sa.status = 'USED' AND sa.deleted = false " +
            "AND si.keeper_id = #{keeperId} AND si.deleted = false " +
            "ORDER BY sa.used_at DESC")
    java.util.List<SealApplication> selectProcessedByKeeper(@Param("keeperId") Long keeperId);
}
