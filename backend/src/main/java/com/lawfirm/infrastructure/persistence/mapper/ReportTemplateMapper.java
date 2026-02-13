package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.workbench.entity.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 报表模板 Mapper */
@Mapper
public interface ReportTemplateMapper extends BaseMapper<ReportTemplate> {

  /**
   * 分页查询报表模板.
   *
   * @param page 分页对象
   * @param keyword 关键词
   * @param dataSource 数据源
   * @param status 状态
   * @param createdBy 创建人ID
   * @return 报表模板分页结果
   */
  @Select(
      """
        <script>
        SELECT * FROM workbench_report_template
        WHERE deleted = false
        <if test="keyword != null and keyword != ''">
            AND (template_name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="dataSource != null and dataSource != ''">
            AND data_source = #{dataSource}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="createdBy != null">
            AND (created_by = #{createdBy} OR is_system = true)
        </if>
        ORDER BY is_system DESC, created_at DESC
        </script>
        """)
  IPage<ReportTemplate> selectTemplatePage(
      Page<ReportTemplate> page,
      @Param("keyword") String keyword,
      @Param("dataSource") String dataSource,
      @Param("status") String status,
      @Param("createdBy") Long createdBy);

  /**
   * 根据模板编号查询.
   *
   * @param templateNo 模板编号
   * @return 报表模板
   */
  @Select(
      "SELECT * FROM workbench_report_template WHERE template_no = #{templateNo} AND deleted = false LIMIT 1")
  ReportTemplate selectByTemplateNo(@Param("templateNo") String templateNo);

  /**
   * 检查模板名称是否存在.
   *
   * @param templateName 模板名称
   * @param excludeId 排除的ID
   * @return 数量
   */
  @Select(
      "SELECT COUNT(*) FROM workbench_report_template "
          + "WHERE template_name = #{templateName} AND deleted = false "
          + "AND id != #{excludeId}")
  int countByTemplateName(
      @Param("templateName") String templateName, @Param("excludeId") Long excludeId);
}
