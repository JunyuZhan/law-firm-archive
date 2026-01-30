package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.knowledge.entity.LawRegulation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 法规Mapper */
@Mapper
public interface LawRegulationMapper extends BaseMapper<LawRegulation> {

  /**
   * 分页查询法规.
   *
   * @param page 分页对象
   * @param categoryId 分类ID
   * @param status 状态
   * @param keyword 关键词
   * @return 法规分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM law_regulation WHERE deleted = false "
          + "<if test='categoryId != null'> AND category_id = #{categoryId} </if>"
          + "<if test='status != null'> AND status = #{status} </if>"
          + "<if test='keyword != null'> AND (title LIKE CONCAT('%',#{keyword},'%') "
          + "OR keywords LIKE CONCAT('%',#{keyword},'%')) </if>"
          + "ORDER BY issue_date DESC, id DESC"
          + "</script>")
  IPage<LawRegulation> selectRegulationPage(
      Page<LawRegulation> page,
      @Param("categoryId") Long categoryId,
      @Param("status") String status,
      @Param("keyword") String keyword);

  /**
   * 增加浏览次数.
   *
   * @param id 法规ID
   * @return 更新数量
   */
  @Update("UPDATE law_regulation SET view_count = view_count + 1 WHERE id = #{id}")
  int incrementViewCount(@Param("id") Long id);

  /**
   * 增加收藏次数.
   *
   * @param id 法规ID
   * @return 更新数量
   */
  @Update("UPDATE law_regulation SET collect_count = collect_count + 1 WHERE id = #{id}")
  int incrementCollectCount(@Param("id") Long id);

  /**
   * 减少收藏次数.
   *
   * @param id 法规ID
   * @return 更新数量
   */
  @Update(
      "UPDATE law_regulation SET collect_count = collect_count - 1 WHERE id = #{id} AND collect_count > 0")
  int decrementCollectCount(@Param("id") Long id);
}
