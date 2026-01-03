package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统公告Mapper
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 分页查询公告
     */
    @Select("<script>" +
            "SELECT * FROM sys_announcement WHERE deleted = false " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='type != null'> AND type = #{type} </if>" +
            "ORDER BY is_top DESC, priority DESC, publish_time DESC" +
            "</script>")
    IPage<Announcement> selectAnnouncementPage(Page<Announcement> page,
                                                @Param("status") String status,
                                                @Param("type") String type);

    /**
     * 查询有效公告
     */
    @Select("SELECT * FROM sys_announcement WHERE status = 'PUBLISHED' AND deleted = false " +
            "AND (expire_time IS NULL OR expire_time > NOW()) " +
            "ORDER BY is_top DESC, priority DESC, publish_time DESC LIMIT #{limit}")
    List<Announcement> selectValidAnnouncements(@Param("limit") int limit);
}
