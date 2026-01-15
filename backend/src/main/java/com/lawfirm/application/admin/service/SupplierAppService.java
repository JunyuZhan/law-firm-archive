package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateSupplierCommand;
import com.lawfirm.application.admin.dto.SupplierDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.domain.admin.repository.PurchaseRequestRepository;
import com.lawfirm.domain.admin.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 供应商应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierAppService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    // 问题430修复：序号生成器防止并发重复
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 分页查询供应商
     */
    public PageResult<SupplierDTO> listSuppliers(PageQuery query, String keyword, String supplierType,
                                                  String status, String rating) {
        Page<Supplier> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Supplier> result = supplierRepository.findPage(page, keyword, supplierType, status, rating);
        
        List<SupplierDTO> items = result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取供应商详情
     */
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        return toDTO(supplier);
    }

    /**
     * 创建供应商
     * 问题430修复：使用安全的编号生成
     */
    @Transactional
    public SupplierDTO createSupplier(CreateSupplierCommand command) {
        String supplierNo = generateSupplierNo();
        
        Supplier supplier = Supplier.builder()
                .supplierNo(supplierNo)
                .name(command.getName())
                .supplierType(command.getSupplierType())
                .contactPerson(command.getContactPerson())
                .contactPhone(command.getContactPhone())
                .contactEmail(command.getContactEmail())
                .address(command.getAddress())
                .creditCode(command.getCreditCode())
                .bankName(command.getBankName())
                .bankAccount(command.getBankAccount())
                .supplyScope(command.getSupplyScope())
                .rating(command.getRating() != null ? command.getRating() : "B")
                .status("ACTIVE")
                .remarks(command.getRemarks())
                .build();
        
        supplierRepository.save(supplier);
        log.info("创建供应商: {}", supplier.getName());
        return toDTO(supplier);
    }

    /**
     * 问题430修复：生成供应商编号（防止并发重复）
     */
    private String generateSupplierNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = sequence.incrementAndGet() % 10000;
        return String.format("SUP%s%04d", date, seq);
    }


    /**
     * 更新供应商
     * 问题修复：只更新非null字段，避免覆盖原有数据
     */
    @Transactional
    public SupplierDTO updateSupplier(Long id, CreateSupplierCommand command) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        
        // 只更新非null字段，避免覆盖原有数据导致NPE
        if (command.getName() != null) {
            supplier.setName(command.getName());
        }
        if (command.getSupplierType() != null) {
            supplier.setSupplierType(command.getSupplierType());
        }
        if (command.getContactPerson() != null) {
            supplier.setContactPerson(command.getContactPerson());
        }
        if (command.getContactPhone() != null) {
            supplier.setContactPhone(command.getContactPhone());
        }
        if (command.getContactEmail() != null) {
            supplier.setContactEmail(command.getContactEmail());
        }
        if (command.getAddress() != null) {
            supplier.setAddress(command.getAddress());
        }
        if (command.getCreditCode() != null) {
            supplier.setCreditCode(command.getCreditCode());
        }
        if (command.getBankName() != null) {
            supplier.setBankName(command.getBankName());
        }
        if (command.getBankAccount() != null) {
            supplier.setBankAccount(command.getBankAccount());
        }
        if (command.getSupplyScope() != null) {
            supplier.setSupplyScope(command.getSupplyScope());
        }
        if (command.getRating() != null) {
            supplier.setRating(command.getRating());
        }
        if (command.getRemarks() != null) {
            supplier.setRemarks(command.getRemarks());
        }
        
        supplierRepository.updateById(supplier);
        log.info("更新供应商: {}", supplier.getName());
        return toDTO(supplier);
    }

    /**
     * 删除供应商
     * 问题432修复：检查是否有采购记录，使用软删除
     */
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        
        // 问题432修复：检查是否有采购记录
        long purchaseCount = purchaseRequestRepository.countBySupplierId(id);
        if (purchaseCount > 0) {
            throw new BusinessException("该供应商有" + purchaseCount + "条采购记录，无法删除。建议使用停用功能。");
        }
        
        // 使用软删除（改为停用状态）
        supplier.setStatus("INACTIVE");
        supplierRepository.updateById(supplier);
        log.info("供应商已停用: {}", supplier.getName());
    }

    /**
     * 启用/停用供应商
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        supplier.setStatus(status);
        supplierRepository.updateById(supplier);
        log.info("供应商状态变更: {} -> {}", supplier.getName(), status);
    }

    /**
     * 获取供应商统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("byStatus", supplierRepository.countByStatus());
        stats.put("byRating", supplierRepository.countByRating());
        return stats;
    }

    private SupplierDTO toDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .supplierNo(supplier.getSupplierNo())
                .name(supplier.getName())
                .supplierType(supplier.getSupplierType())
                .supplierTypeName(getSupplierTypeName(supplier.getSupplierType()))
                .contactPerson(supplier.getContactPerson())
                .contactPhone(supplier.getContactPhone())
                .contactEmail(supplier.getContactEmail())
                .address(supplier.getAddress())
                .creditCode(supplier.getCreditCode())
                .bankName(supplier.getBankName())
                .bankAccount(supplier.getBankAccount())
                .supplyScope(supplier.getSupplyScope())
                .rating(supplier.getRating())
                .ratingName(getRatingName(supplier.getRating()))
                .status(supplier.getStatus())
                .statusName("ACTIVE".equals(supplier.getStatus()) ? "正常" : "停用")
                .remarks(supplier.getRemarks())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    private String getSupplierTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "GOODS" -> "物品供应商";
            case "SERVICE" -> "服务供应商";
            case "BOTH" -> "综合供应商";
            case "OFFICE_SUPPLIES" -> "办公用品供应商";
            default -> type;
        };
    }

    private String getRatingName(String rating) {
        if (rating == null) return null;
        return switch (rating) {
            case "A" -> "优秀";
            case "B" -> "良好";
            case "C" -> "一般";
            case "D" -> "较差";
            default -> rating;
        };
    }
}
