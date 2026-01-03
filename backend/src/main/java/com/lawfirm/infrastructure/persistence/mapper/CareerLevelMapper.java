package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.CareerLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职级通道 Mapper
 */
@Mapper
public interface CareerLevelMapper extends BaseMapper<CareerLevel> {

    /**
     * 分页查询职级
     */
    @Select("""
        <script>
        SELECT * FROM hr_career_level
        WHERE deleted = false
        <if test="keyword != null and keyword != ''">
            AND (level_name LIKE CONCAT('%', #{keyword}, '%') OR level_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="category != null and category != ''">
            AND category = #{category}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        ORDER BY category, level_order ASC
        </script>
        """)
    IPage<CareerLevel> selectLevelPage(Page<CareerLevel> page,
                                        @Param("keyword") String keyword,
                                        @Param("category") String category,
                                        @Param("status") String status);

    /**
     * 按类别查询职级列表
     */
    @Select("SELECT * FROM hr_career_level WHERE deleted = false AND category = #{category} AND status = 'ACTIVE' ORDER BY level_order ASC")
    List<CareerLevel> selectByCategory(@Param("category") String category);

    /**
     * 查询下一级职级
     */
    @Select("""
        SELECT * FROM hr_career_level 
        WHERE deleted = false AND category = #{category} AND status = 'ACTIVE' AND level_order > #{currentOrder}
        ORDER BY level_order ASC LIMIT 1
        """)
    CareerLevel selectNextLevel(@Param("category") String category, @Param("currentOrder") Integer currentOrder);

    /**
     * 根据编码查询
     */
    @Select("SELECT * FROM hr_career_level WHERE level_code = #{levelCode} AND deleted = false LIMIT 1")
    CareerLevel selectByLevelCode(@Param("levelCode") String levelCode);
}
