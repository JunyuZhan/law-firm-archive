package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ClientRelatedCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客户关联企业 Mapper
 */
@Mapper
public interface ClientRelatedCompanyMapper extends BaseMapper<ClientRelatedCompany> {

    /**
     * 根据客户ID查询关联企业列表
     */
    @Select("SELECT * FROM crm_client_related_company WHERE client_id = #{clientId} AND deleted = false ORDER BY related_company_type, created_at DESC")
    List<ClientRelatedCompany> selectByClientId(@Param("clientId") Long clientId);

    /**
     * 根据关联类型查询
     */
    @Select("SELECT * FROM crm_client_related_company WHERE client_id = #{clientId} AND related_company_type = #{type} AND deleted = false")
    List<ClientRelatedCompany> selectByClientIdAndType(@Param("clientId") Long clientId, @Param("type") String type);
}

