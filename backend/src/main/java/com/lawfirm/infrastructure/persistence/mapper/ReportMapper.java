package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.workbench.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报表记录 Mapper
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    /**
     * 分页查询报表记录
     */
    @Select("""
        <script>
        SELECT * FROM workbench_report
        WHERE deleted = false
        <if test="reportType != null and reportType != ''">
            AND report_type = #{reportType}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="generatedBy != null">
            AND generated_by = #{generatedBy}
        </if>
        ORDER BY created_at DESC
        </script>
        """)
    IPage<Report> selectReportPage(Page<Report> page,
                                    @Param("reportType") String reportType,
                                    @Param("status") String status,
                                    @Param("generatedBy") Long generatedBy);

    /**
     * 根据报表编号查询
     */
    @Select("SELECT * FROM workbench_report WHERE report_no = #{reportNo} AND deleted = false LIMIT 1")
    Report selectByReportNo(@Param("reportNo") String reportNo);
}

