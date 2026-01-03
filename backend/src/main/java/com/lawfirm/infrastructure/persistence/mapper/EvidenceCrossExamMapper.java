package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.evidence.entity.EvidenceCrossExam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 质证记录Mapper
 */
@Mapper
public interface EvidenceCrossExamMapper extends BaseMapper<EvidenceCrossExam> {

    /**
     * 查询证据的质证记录
     */
    @Select("SELECT * FROM evidence_cross_exam WHERE evidence_id = #{evidenceId} ORDER BY created_at DESC")
    List<EvidenceCrossExam> selectByEvidenceId(@Param("evidenceId") Long evidenceId);

    /**
     * 查询指定方的质证记录
     */
    @Select("SELECT * FROM evidence_cross_exam WHERE evidence_id = #{evidenceId} AND exam_party = #{examParty}")
    EvidenceCrossExam selectByEvidenceIdAndParty(@Param("evidenceId") Long evidenceId, @Param("examParty") String examParty);
}
