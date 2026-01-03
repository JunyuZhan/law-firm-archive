package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Matter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 案件 Mapper
 */
@Mapper
public interface MatterMapper extends BaseMapper<Matter> {

    /**
     * 根据案件编号查询
     */
    @Select("SELECT * FROM matter WHERE matter_no = #{matterNo} AND deleted = false")
    Matter selectByMatterNo(@Param("matterNo") String matterNo);

    /**
     * 分页查询案件
     */
    @Select("""
        <script>
        SELECT m.*
        FROM matter m
        WHERE m.deleted = false
        <if test="name != null and name != ''">
            AND m.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="matterNo != null and matterNo != ''">
            AND m.matter_no LIKE CONCAT('%', #{matterNo}, '%')
        </if>
        <if test="clientId != null">
            AND m.client_id = #{clientId}
        </if>
        <if test="leadLawyerId != null">
            AND m.lead_lawyer_id = #{leadLawyerId}
        </if>
        <if test="departmentId != null">
            AND m.department_id = #{departmentId}
        </if>
        <if test="matterType != null and matterType != ''">
            AND m.matter_type = #{matterType}
        </if>
        <if test="status != null and status != ''">
            AND m.status = #{status}
        </if>
        ORDER BY m.id DESC
        </script>
        """)
    IPage<Matter> selectMatterPage(Page<Matter> page,
                                    @Param("name") String name,
                                    @Param("matterNo") String matterNo,
                                    @Param("clientId") Long clientId,
                                    @Param("leadLawyerId") Long leadLawyerId,
                                    @Param("departmentId") Long departmentId,
                                    @Param("matterType") String matterType,
                                    @Param("status") String status);

    /**
     * 查询律师参与的案件
     */
    @Select("""
        SELECT m.* FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND mp.status = 'ACTIVE' AND m.deleted = false
        ORDER BY m.id DESC
        """)
    IPage<Matter> selectByParticipantUserId(Page<Matter> page, @Param("userId") Long userId);

    /**
     * 统计用户参与的项目数量（按状态）
     */
    @Select("""
        SELECT COUNT(DISTINCT m.id) FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND m.status = #{status} AND m.deleted = false
        """)
    int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 查询用户最近参与的项目
     */
    @Select("""
        SELECT m.* FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND m.deleted = false
        ORDER BY m.updated_at DESC
        LIMIT #{limit}
        """)
    java.util.List<Matter> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}

