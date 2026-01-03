package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 联系人 Mapper
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    /**
     * 根据客户ID查询联系人列表
     */
    @Select("SELECT * FROM crm_contact WHERE client_id = #{clientId} AND deleted = false ORDER BY is_primary DESC, created_at DESC")
    List<Contact> selectByClientId(@Param("clientId") Long clientId);

    /**
     * 查询客户的主要联系人
     */
    @Select("SELECT * FROM crm_contact WHERE client_id = #{clientId} AND is_primary = true AND deleted = false LIMIT 1")
    Contact selectPrimaryByClientId(@Param("clientId") Long clientId);

    /**
     * 取消客户的所有主要联系人标记
     */
    @Update("UPDATE crm_contact SET is_primary = false, updated_at = CURRENT_TIMESTAMP WHERE client_id = #{clientId} AND deleted = false")
    void clearPrimaryByClientId(@Param("clientId") Long clientId);
}

