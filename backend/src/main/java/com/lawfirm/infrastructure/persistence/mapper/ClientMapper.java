package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.client.entity.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 客户 Mapper
 */
@Mapper
public interface ClientMapper extends BaseMapper<Client> {

    /**
     * 根据客户编号查询
     */
    @Select("SELECT * FROM crm_client WHERE client_no = #{clientNo} AND deleted = false")
    Client selectByClientNo(@Param("clientNo") String clientNo);

    /**
     * 根据名称模糊查询（用于利冲检查）
     */
    @Select("""
        <script>
        SELECT * FROM crm_client WHERE deleted = false
        AND (name LIKE CONCAT('%', #{keyword}, '%')
             OR credit_code LIKE CONCAT('%', #{keyword}, '%')
             OR id_card LIKE CONCAT('%', #{keyword}, '%')
             OR contact_person LIKE CONCAT('%', #{keyword}, '%'))
        </script>
        """)
    IPage<Client> selectByKeyword(Page<Client> page, @Param("keyword") String keyword);

    /**
     * 分页查询客户
     */
    @Select("""
        <script>
        SELECT c.*
        FROM crm_client c
        WHERE c.deleted = false
        <if test="name != null and name != ''">
            AND c.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="clientType != null and clientType != ''">
            AND c.client_type = #{clientType}
        </if>
        <if test="status != null and status != ''">
            AND c.status = #{status}
        </if>
        <if test="responsibleLawyerId != null">
            AND c.responsible_lawyer_id = #{responsibleLawyerId}
        </if>
        ORDER BY c.id DESC
        </script>
        """)
    IPage<Client> selectClientPage(Page<Client> page,
                                    @Param("name") String name,
                                    @Param("clientType") String clientType,
                                    @Param("status") String status,
                                    @Param("responsibleLawyerId") Long responsibleLawyerId);
}

