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
            "SELECT * FROM seal_application WHERE deleted = false " +
            "<if test='applicantId != null'> AND applicant_id = #{applicantId} </if>" +
            "<if test='sealId != null'> AND seal_id = #{sealId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<SealApplication> selectApplicationPage(Page<SealApplication> page,
                                                 @Param("applicantId") Long applicantId,
                                                 @Param("sealId") Long sealId,
                                                 @Param("matterId") Long matterId,
                                                 @Param("status") String status);

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
}
