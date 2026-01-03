package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.CaseStudyNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 案例学习笔记Mapper（M10-013）
 */
@Mapper
public interface CaseStudyNoteMapper extends BaseMapper<CaseStudyNote> {

    /**
     * 查询用户对案例的学习笔记
     */
    @Select("SELECT * FROM case_study_note WHERE case_id = #{caseId} AND user_id = #{userId} AND deleted = false")
    CaseStudyNote selectByCaseAndUser(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * 查询案例的所有学习笔记
     */
    @Select("SELECT * FROM case_study_note WHERE case_id = #{caseId} AND deleted = false ORDER BY created_at DESC")
    List<CaseStudyNote> selectByCaseId(@Param("caseId") Long caseId);

    /**
     * 查询用户的所有学习笔记
     */
    @Select("SELECT * FROM case_study_note WHERE user_id = #{userId} AND deleted = false ORDER BY created_at DESC")
    List<CaseStudyNote> selectByUserId(@Param("userId") Long userId);
}

