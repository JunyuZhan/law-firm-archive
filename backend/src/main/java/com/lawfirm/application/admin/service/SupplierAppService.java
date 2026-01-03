package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateSupplierCommand;
import com.lawfirm.application.admin.dto.SupplierDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.domain.admin.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierAppService {

    private final SupplierRepository supplierRepository;

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
     */
    @Transactional
    public SupplierDTO createSupplier(CreateSupplierCommand command) {
        String supplierNo = "SUP" + System.currentTimeMillis();
        
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
     * 更新供应商
     */
    @Transactional
    public SupplierDTO updateSupplier(Long id, CreateSupplierCommand command) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        
        supplier.setName(command.getName());
        supplier.setSupplierType(command.getSupplierType());
        supplier.setContactPerson(command.getContactPerson());
        supplier.setContactPhone(command.getContactPhone());
        supplier.setContactEmail(command.getContactEmail());
        supplier.setAddress(command.getAddress());
        supplier.setCreditCode(command.getCreditCode());
        supplier.setBankName(command.getBankName());
        supplier.setBankAccount(command.getBankAccount());
        supplier.setSupplyScope(command.getSupplyScope());
        supplier.setRating(command.getRating());
        supplier.setRemarks(command.getRemarks());
        
        supplierRepository.updateById(supplier);
        log.info("更新供应商: {}", supplier.getName());
        return toDTO(supplier);
    }

    /**
     * 删除供应商
     */
    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.getById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        supplierRepository.removeById(id);
        log.info("删除供应商: {}", supplier.getName());
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
        return switch (type) {
            case "GOODS" -> "物品供应商";
            case "SERVICE" -> "服务供应商";
            case "BOTH" -> "综合供应商";
            default -> type;
        };
    }

    private String getRatingName(String rating) {
        return switch (rating) {
            case "A" -> "优秀";
            case "B" -> "良好";
            case "C" -> "一般";
            case "D" -> "较差";
            default -> rating;
        };
    }
}
