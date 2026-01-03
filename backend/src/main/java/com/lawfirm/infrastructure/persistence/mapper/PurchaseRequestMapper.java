package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.PurchaseRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 采购申请Mapper
 */
@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {

    /**
     * 分页查询采购申请
     */
    @Select("<script>" +
            "SELECT * FROM admin_purchase_request WHERE deleted = false " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR request_no LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='purchaseType != null and purchaseType != \"\"'>" +
            "AND purchase_type = #{purchaseType} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='applicantId != null'>" +
            "AND applicant_id = #{applicantId} " +
            "</if>" +
            "<if test='departmentId != null'>" +
            "AND department_id = #{departmentId} " +
            "</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<PurchaseRequest> findPage(Page<PurchaseRequest> page,
                                    @Param("keyword") String keyword,
                                    @Param("purchaseType") String purchaseType,
                                    @Param("status") String status,
                                    @Param("applicantId") Long applicantId,
                                    @Param("departmentId") Long departmentId);

    /**
     * 查询待审批的申请
     */
    @Select("SELECT * FROM admin_purchase_request WHERE deleted = false AND status = 'PENDING' ORDER BY created_at ASC")
    List<PurchaseRequest> findPendingApproval();

    /**
     * 按状态统计
     */
    @Select("SELECT status, COUNT(*) as count FROM admin_purchase_request WHERE deleted = false GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 按类型统计金额
     */
    @Select("SELECT purchase_type, SUM(actual_amount) as total FROM admin_purchase_request WHERE deleted = false AND status = 'COMPLETED' GROUP BY purchase_type")
    List<Map<String, Object>> sumAmountByType();
}
