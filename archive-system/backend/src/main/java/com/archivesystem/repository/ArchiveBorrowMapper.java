package com.archivesystem.repository;

import com.archivesystem.entity.ArchiveBorrow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案借阅Mapper.
 */
@Mapper
public interface ArchiveBorrowMapper extends BaseMapper<ArchiveBorrow> {

    /**
     * 分页查询借阅记录.
     */
    IPage<ArchiveBorrow> selectBorrowPage(
            Page<ArchiveBorrow> page,
            @Param("archiveId") Long archiveId,
            @Param("borrowerId") Long borrowerId,
            @Param("status") String status
    );

    /**
     * 查询档案当前的借出记录.
     */
    @Select("SELECT * FROM archive_borrow WHERE archive_id = #{archiveId} AND status = 'BORROWED' AND deleted = false LIMIT 1")
    ArchiveBorrow selectCurrentBorrow(@Param("archiveId") Long archiveId);

    /**
     * 查询逾期未还的借阅记录.
     */
    @Select("SELECT * FROM archive_borrow WHERE status = 'BORROWED' AND expected_return_date < #{today} AND deleted = false")
    List<ArchiveBorrow> selectOverdueBorrows(@Param("today") LocalDate today);

    /**
     * 更新逾期状态.
     */
    @Update("UPDATE archive_borrow SET status = 'OVERDUE' WHERE status = 'BORROWED' AND expected_return_date < #{today} AND deleted = false")
    int updateOverdueStatus(@Param("today") LocalDate today);

    /**
     * 统计借阅人的借阅数量.
     */
    @Select("SELECT COUNT(*) FROM archive_borrow WHERE borrower_id = #{borrowerId} AND status = 'BORROWED' AND deleted = false")
    int countActiveBorrowsByUser(@Param("borrowerId") Long borrowerId);
}
