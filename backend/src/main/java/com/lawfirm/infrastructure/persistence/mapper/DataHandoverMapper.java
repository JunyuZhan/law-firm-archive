package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.DataHandover;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据交接记录Mapper
 */
@Mapper
public interface DataHandoverMapper extends BaseMapper<DataHandover> {

    /**
     * 分页查询交接记录
     */
    @Select("<script>" +
            "SELECT * FROM sys_data_handover WHERE deleted = false " +
            "<if test='fromUserId != null'> AND from_user_id = #{fromUserId} </if>" +
            "<if test='toUserId != null'> AND to_user_id = #{toUserId} </if>" +
            "<if test='handoverType != null and handoverType != \"\"'> AND handover_type = #{handoverType} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<DataHandover> selectHandoverPage(Page<DataHandover> page,
                                           @Param("fromUserId") Long fromUserId,
                                           @Param("toUserId") Long toUserId,
                                           @Param("handoverType") String handoverType,
                                           @Param("status") String status);

    /**
     * 根据移交人ID查询
     */
    @Select("SELECT * FROM sys_data_handover WHERE from_user_id = #{fromUserId} AND deleted = false ORDER BY created_at DESC")
    List<DataHandover> selectByFromUserId(@Param("fromUserId") Long fromUserId);

    /**
     * 根据接收人ID查询
     */
    @Select("SELECT * FROM sys_data_handover WHERE to_user_id = #{toUserId} AND deleted = false ORDER BY created_at DESC")
    List<DataHandover> selectByToUserId(@Param("toUserId") Long toUserId);

    /**
     * 根据交接单号查询
     */
    @Select("SELECT * FROM sys_data_handover WHERE handover_no = #{handoverNo} AND deleted = false")
    DataHandover selectByHandoverNo(@Param("handoverNo") String handoverNo);
}

