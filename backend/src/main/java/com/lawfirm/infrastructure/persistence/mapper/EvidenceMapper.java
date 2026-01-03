package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.evidence.entity.Evidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 证据Mapper
 */
@Mapper
public interface EvidenceMapper extends BaseMapper<Evidence> {

    /**
     * 分页查询证据
     */
    @Select("<script>" +
            "SELECT * FROM evidence WHERE deleted = false " +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='name != null and name != \"\"'> AND name LIKE CONCAT('%', #{name}, '%') </if>" +
            "<if test='evidenceType != null and evidenceType != \"\"'> AND evidence_type = #{evidenceType} </if>" +
            "<if test='groupName != null and groupName != \"\"'> AND group_name = #{groupName} </if>" +
            "<if test='crossExamStatus != null and crossExamStatus != \"\"'> AND cross_exam_status = #{crossExamStatus} </if>" +
            "ORDER BY group_name, sort_order, created_at" +
            "</script>")
    IPage<Evidence> selectEvidencePage(Page<Evidence> page,
                                       @Param("matterId") Long matterId,
                                       @Param("name") String name,
                                       @Param("evidenceType") String evidenceType,
                                       @Param("groupName") String groupName,
                                       @Param("crossExamStatus") String crossExamStatus);

    /**
     * 按案件查询证据列表
     */
    @Select("SELECT * FROM evidence WHERE matter_id = #{matterId} AND deleted = false ORDER BY group_name, sort_order")
    List<Evidence> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 获取案件下的证据分组
     */
    @Select("SELECT DISTINCT group_name FROM evidence WHERE matter_id = #{matterId} AND deleted = false AND group_name IS NOT NULL ORDER BY group_name")
    List<String> selectGroupsByMatterId(@Param("matterId") Long matterId);

    /**
     * 获取分组内最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM evidence WHERE matter_id = #{matterId} AND group_name = #{groupName} AND deleted = false")
    Integer selectMaxSortOrder(@Param("matterId") Long matterId, @Param("groupName") String groupName);
}
