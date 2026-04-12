package com.archivesystem.repository;

import com.archivesystem.entity.BorrowApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 借阅申请Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface BorrowApplicationMapper extends BaseMapper<BorrowApplication> {

    /**
     * 根据申请编号查询.
     */
    @Select("SELECT * FROM arc_borrow_application WHERE application_no = #{applicationNo} AND deleted = false")
    BorrowApplication selectByApplicationNo(@Param("applicationNo") String applicationNo);

    /**
     * 根据申请人ID查询申请列表.
     */
    @Select("SELECT * FROM arc_borrow_application WHERE applicant_id = #{applicantId} AND deleted = false ORDER BY created_at DESC")
    List<BorrowApplication> selectByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 根据档案ID查询借阅记录.
     */
    @Select("SELECT * FROM arc_borrow_application WHERE archive_id = #{archiveId} AND deleted = false ORDER BY created_at DESC")
    List<BorrowApplication> selectByArchiveId(@Param("archiveId") Long archiveId);

    /**
     * 查询待审批列表.
     */
    @Select("SELECT * FROM arc_borrow_application WHERE status = 'PENDING' AND deleted = false ORDER BY created_at")
    List<BorrowApplication> selectPendingList();
}
