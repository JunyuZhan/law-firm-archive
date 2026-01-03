package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户联系记录 Mapper
 */
@Mapper
public interface ClientContactRecordMapper extends BaseMapper<ClientContactRecord> {

    /**
     * 根据客户ID分页查询联系记录
     */
    @Select("SELECT * FROM crm_client_contact_record WHERE client_id = #{clientId} AND deleted = false ORDER BY contact_date DESC")
    IPage<ClientContactRecord> selectByClientId(Page<ClientContactRecord> page, @Param("clientId") Long clientId);

    /**
     * 根据客户ID查询所有联系记录
     */
    @Select("SELECT * FROM crm_client_contact_record WHERE client_id = #{clientId} AND deleted = false ORDER BY contact_date DESC")
    List<ClientContactRecord> selectAllByClientId(@Param("clientId") Long clientId);

    /**
     * 查询需要跟进的联系记录
     */
    @Select("SELECT * FROM crm_client_contact_record " +
            "WHERE next_follow_up_date IS NOT NULL " +
            "AND next_follow_up_date <= #{date} " +
            "AND follow_up_reminder = true " +
            "AND deleted = false " +
            "ORDER BY next_follow_up_date ASC")
    List<ClientContactRecord> selectFollowUpRecords(@Param("date") LocalDate date);
}

