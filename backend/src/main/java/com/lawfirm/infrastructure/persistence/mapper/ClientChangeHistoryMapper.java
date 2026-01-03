package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.client.entity.ClientChangeHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 企业变更历史Mapper（M2-014）
 */
@Mapper
public interface ClientChangeHistoryMapper extends BaseMapper<ClientChangeHistory> {

    /**
     * 查询客户的所有变更记录（按变更日期倒序）
     */
    @Select("SELECT * FROM crm_client_change_history WHERE client_id = #{clientId} AND deleted = false ORDER BY change_date DESC, created_at DESC")
    List<ClientChangeHistory> selectByClientId(@Param("clientId") Long clientId);

    /**
     * 查询指定类型的变更记录
     */
    @Select("SELECT * FROM crm_client_change_history WHERE client_id = #{clientId} AND change_type = #{changeType} AND deleted = false ORDER BY change_date DESC")
    List<ClientChangeHistory> selectByClientIdAndType(@Param("clientId") Long clientId, @Param("changeType") String changeType);
}

