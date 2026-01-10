package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.LetterApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 出函申请Mapper
 */
@Mapper
public interface LetterApplicationMapper extends BaseMapper<LetterApplication> {

    /**
     * 查询全部出函申请（行政管理用）
     */
    @Select("<script>" +
            "SELECT * FROM letter_application WHERE deleted = false " +
            "<if test='applicationNo != null and applicationNo != \"\"'> AND application_no LIKE CONCAT('%', #{applicationNo}, '%') </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='startDate != null and startDate != \"\"'> AND created_at >= CAST(#{startDate} AS TIMESTAMP) </if>" +
            "<if test='endDate != null and endDate != \"\"'> AND created_at &lt;= CAST(#{endDate} AS TIMESTAMP) + INTERVAL '1 day' </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<LetterApplication> selectAllApplications(
            @Param("applicationNo") String applicationNo,
            @Param("matterName") String matterName,
            @Param("status") String status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询待打印的出函申请（行政用）
     */
    @Select("SELECT * FROM letter_application WHERE status IN ('APPROVED', 'PRINTED') AND deleted = false ORDER BY approved_at ASC")
    List<LetterApplication> selectPendingPrint();

    /**
     * 查询某项目的出函申请
     */
    @Select("SELECT * FROM letter_application WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
    List<LetterApplication> selectByMatterId(@Param("matterId") Long matterId);
    
    /**
     * 查询某项目的出函申请最大序号（从application_no中提取）
     * 用于生成新的申请编号，避免并发冲突
     */
    @Select("SELECT MAX(CAST(SUBSTRING(application_no FROM '[0-9]+$') AS INTEGER)) " +
            "FROM letter_application WHERE matter_id = #{matterId} AND deleted = false")
    Integer selectMaxSequenceByMatterId(@Param("matterId") Long matterId);
}
