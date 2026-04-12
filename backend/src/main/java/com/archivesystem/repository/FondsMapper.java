package com.archivesystem.repository;

import com.archivesystem.entity.Fonds;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 全宗Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface FondsMapper extends BaseMapper<Fonds> {

    /**
     * 根据全宗号查询.
     */
    @Select("SELECT * FROM arc_fonds WHERE fonds_no = #{fondsNo} AND deleted = false")
    Fonds selectByFondsNo(@Param("fondsNo") String fondsNo);
}
