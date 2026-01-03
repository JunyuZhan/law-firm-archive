package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.knowledge.entity.CaseLibrary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 案例库Mapper
 */
@Mapper
public interface CaseLibraryMapper extends BaseMapper<CaseLibrary> {

    /**
     * 分页查询案例
     */
    @Select("<script>" +
            "SELECT * FROM case_library WHERE deleted = false " +
            "<if test='categoryId != null'> AND category_id = #{categoryId} </if>" +
            "<if test='source != null'> AND source = #{source} </if>" +
            "<if test='caseType != null'> AND case_type = #{caseType} </if>" +
            "<if test='keyword != null'> AND (title LIKE CONCAT('%',#{keyword},'%') OR keywords LIKE CONCAT('%',#{keyword},'%') OR cause_of_action LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "ORDER BY judge_date DESC, id DESC" +
            "</script>")
    IPage<CaseLibrary> selectCasePage(Page<CaseLibrary> page,
                                       @Param("categoryId") Long categoryId,
                                       @Param("source") String source,
                                       @Param("caseType") String caseType,
                                       @Param("keyword") String keyword);

    /**
     * 增加浏览次数
     */
    @Update("UPDATE case_library SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加收藏次数
     */
    @Update("UPDATE case_library SET collect_count = collect_count + 1 WHERE id = #{id}")
    int incrementCollectCount(@Param("id") Long id);

    /**
     * 减少收藏次数
     */
    @Update("UPDATE case_library SET collect_count = collect_count - 1 WHERE id = #{id} AND collect_count > 0")
    int decrementCollectCount(@Param("id") Long id);
}
