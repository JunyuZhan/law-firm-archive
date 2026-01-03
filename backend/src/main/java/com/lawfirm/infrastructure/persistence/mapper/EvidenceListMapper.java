package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 证据清单Mapper
 */
@Mapper
public interface EvidenceListMapper extends BaseMapper<EvidenceList> {

    @Select("<script>" +
            "SELECT * FROM evidence_list WHERE deleted = false " +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='listType != null'> AND list_type = #{listType} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<EvidenceList> selectListPage(Page<EvidenceList> page,
                                       @Param("matterId") Long matterId,
                                       @Param("listType") String listType);

    @Select("SELECT * FROM evidence_list WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
    List<EvidenceList> selectByMatterId(@Param("matterId") Long matterId);
}
