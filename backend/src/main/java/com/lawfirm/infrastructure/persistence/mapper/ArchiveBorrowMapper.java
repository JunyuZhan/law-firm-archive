package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.archive.entity.ArchiveBorrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案借阅 Mapper
 */
@Mapper
public interface ArchiveBorrowMapper extends BaseMapper<ArchiveBorrow> {

    /**
     * 查询逾期的借阅记录
     */
    @Select("""
        SELECT * FROM archive_borrow 
        WHERE status IN ('BORROWED', 'OVERDUE') 
        AND expected_return_date < #{today}
        AND deleted = false
        ORDER BY expected_return_date
        """)
    List<ArchiveBorrow> selectOverdueBorrows(@Param("today") LocalDate today);
}

