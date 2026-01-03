package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.AssetReceiveCommand;
import com.lawfirm.application.admin.command.CreateAssetCommand;
import com.lawfirm.application.admin.dto.AssetDTO;
import com.lawfirm.application.admin.dto.AssetRecordDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.AssetRecord;
import com.lawfirm.domain.admin.repository.AssetRecordRepository;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAppService {

    private final AssetRepository assetRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final UserRepository userRepository;

    /**
     * 分页查询资产
     */
    public PageResult<AssetDTO> listAssets(PageQuery query, String keyword, String category, 
                                            String status, Long departmentId) {
        Page<Asset> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Asset> result = assetRepository.findPage(page, keyword, category, status, departmentId, null);
        
        List<AssetDTO> items = result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取资产详情
     */
    public AssetDTO getAssetById(Long id) {
        Asset asset = assetRepository.getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        return toDTO(asset);
    }

    /**
     * 创建资产
     */
    @Transactional
    public AssetDTO createAsset(CreateAssetCommand command) {
        // 生成资产编号
        String assetNo = generateAssetNo(command.getCategory());
        
        Asset asset = Asset.builder()
                .assetNo(assetNo)
                .name(command.getName())
                .category(command.getCategory())
                .brand(command.getBrand())
                .model(command.getModel())
                .specification(command.getSpecification())
                .serialNumber(command.getSerialNumber())
                .purchaseDate(command.getPurchaseDate())
                .purchasePrice(command.getPurchasePrice())
                .supplier(command.getSupplier())
                .warrantyExpireDate(command.getWarrantyExpireDate())
                .usefulLife(command.getUsefulLife())
                .location(command.getLocation())
                .departmentId(command.getDepartmentId())
                .imageUrl(command.getImageUrl())
                .remarks(command.getRemarks())
                .status("IDLE")
                .build();
        
        assetRepository.save(asset);
        log.info("创建资产: {}", asset.getAssetNo());
        return toDTO(asset);
    }

    /**
     * 更新资产
     */
    @Transactional
    public AssetDTO updateAsset(Long id, CreateAssetCommand command) {
        Asset asset = assetRepository.getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        
        asset.setName(command.getName());
        asset.setCategory(command.getCategory());
        asset.setBrand(command.getBrand());
        asset.setModel(command.getModel());
        asset.setSpecification(command.getSpecification());
        asset.setSerialNumber(command.getSerialNumber());
        asset.setPurchaseDate(command.getPurchaseDate());
        asset.setPurchasePrice(command.getPurchasePrice());
        asset.setSupplier(command.getSupplier());
        asset.setWarrantyExpireDate(command.getWarrantyExpireDate());
        asset.setUsefulLife(command.getUsefulLife());
        asset.setLocation(command.getLocation());
        asset.setDepartmentId(command.getDepartmentId());
        asset.setImageUrl(command.getImageUrl());
        asset.setRemarks(command.getRemarks());
        
        assetRepository.updateById(asset);
        log.info("更新资产: {}", asset.getAssetNo());
        return toDTO(asset);
    }

    /**
     * 删除资产
     */
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.getById(id);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if ("IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("使用中的资产无法删除");
        }
        
        assetRepository.removeById(id);
        log.info("删除资产: {}", asset.getAssetNo());
    }

    /**
     * 资产领用
     */
    @Transactional
    public void receiveAsset(AssetReceiveCommand command) {
        Asset asset = assetRepository.getById(command.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"IDLE".equals(asset.getStatus())) {
            throw new BusinessException("该资产当前不可领用");
        }
        
        Long userId = command.getUserId() != null ? command.getUserId() : SecurityUtils.getCurrentUserId();
        
        // 创建领用记录
        AssetRecord record = AssetRecord.builder()
                .assetId(asset.getId())
                .recordType("RECEIVE")
                .operatorId(SecurityUtils.getCurrentUserId())
                .toUserId(userId)
                .operateDate(LocalDate.now())
                .expectedReturnDate(command.getExpectedReturnDate())
                .reason(command.getReason())
                .remarks(command.getRemarks())
                .approvalStatus("APPROVED") // 简化流程，直接批准
                .build();
        
        assetRecordRepository.save(record);
        
        // 更新资产状态
        asset.setStatus("IN_USE");
        asset.setCurrentUserId(userId);
        assetRepository.updateById(asset);
        
        log.info("资产领用: assetNo={}, userId={}", asset.getAssetNo(), userId);
    }

    /**
     * 资产归还
     */
    @Transactional
    public void returnAsset(Long assetId, String remarks) {
        Asset asset = assetRepository.getById(assetId);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("该资产当前不在使用中");
        }
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        
        // 创建归还记录
        AssetRecord record = AssetRecord.builder()
                .assetId(asset.getId())
                .recordType("RETURN")
                .operatorId(currentUserId)
                .fromUserId(asset.getCurrentUserId())
                .operateDate(LocalDate.now())
                .actualReturnDate(LocalDate.now())
                .remarks(remarks)
                .approvalStatus("APPROVED")
                .build();
        
        assetRecordRepository.save(record);
        
        // 更新资产状态
        asset.setStatus("IDLE");
        asset.setCurrentUserId(null);
        assetRepository.updateById(asset);
        
        log.info("资产归还: assetNo={}", asset.getAssetNo());
    }

    /**
     * 资产报废
     */
    @Transactional
    public void scrapAsset(Long assetId, String reason) {
        Asset asset = assetRepository.getById(assetId);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if ("IN_USE".equals(asset.getStatus())) {
            throw new BusinessException("使用中的资产需先归还后才能报废");
        }
        
        // 创建报废记录
        AssetRecord record = AssetRecord.builder()
                .assetId(asset.getId())
                .recordType("SCRAP")
                .operatorId(SecurityUtils.getCurrentUserId())
                .operateDate(LocalDate.now())
                .reason(reason)
                .approvalStatus("PENDING")
                .build();
        
        assetRecordRepository.save(record);
        
        // 更新资产状态
        asset.setStatus("SCRAPPED");
        assetRepository.updateById(asset);
        
        log.info("资产报废: assetNo={}", asset.getAssetNo());
    }

    /**
     * 获取资产操作记录
     */
    public List<AssetRecordDTO> getAssetRecords(Long assetId) {
        return assetRecordRepository.findByAssetId(assetId).stream()
                .map(this::toRecordDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我领用的资产
     */
    public List<AssetDTO> getMyAssets() {
        Long userId = SecurityUtils.getCurrentUserId();
        return assetRepository.findByCurrentUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取闲置资产
     */
    public List<AssetDTO> getIdleAssets() {
        return assetRepository.findIdleAssets().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资产统计
     */
    public Map<String, Object> getAssetStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("byStatus", assetRepository.countByStatus());
        stats.put("byCategory", assetRepository.countByCategory());
        return stats;
    }

    private String generateAssetNo(String category) {
        String prefix = switch (category) {
            case "OFFICE" -> "OF";
            case "IT" -> "IT";
            case "FURNITURE" -> "FN";
            case "VEHICLE" -> "VH";
            default -> "OT";
        };
        return prefix + System.currentTimeMillis();
    }

    private AssetDTO toDTO(Asset asset) {
        AssetDTO dto = AssetDTO.builder()
                .id(asset.getId())
                .assetNo(asset.getAssetNo())
                .name(asset.getName())
                .category(asset.getCategory())
                .categoryName(getCategoryName(asset.getCategory()))
                .brand(asset.getBrand())
                .model(asset.getModel())
                .specification(asset.getSpecification())
                .serialNumber(asset.getSerialNumber())
                .purchaseDate(asset.getPurchaseDate())
                .purchasePrice(asset.getPurchasePrice())
                .supplier(asset.getSupplier())
                .warrantyExpireDate(asset.getWarrantyExpireDate())
                .usefulLife(asset.getUsefulLife())
                .location(asset.getLocation())
                .currentUserId(asset.getCurrentUserId())
                .departmentId(asset.getDepartmentId())
                .status(asset.getStatus())
                .statusName(getStatusName(asset.getStatus()))
                .imageUrl(asset.getImageUrl())
                .remarks(asset.getRemarks())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
        
        // 是否在保修期内
        if (asset.getWarrantyExpireDate() != null) {
            dto.setInWarranty(!asset.getWarrantyExpireDate().isBefore(LocalDate.now()));
        }
        
        // 查询当前使用人名称
        if (asset.getCurrentUserId() != null) {
            User user = userRepository.getById(asset.getCurrentUserId());
            if (user != null) {
                dto.setCurrentUserName(user.getRealName());
            }
        }
        
        return dto;
    }

    private AssetRecordDTO toRecordDTO(AssetRecord record) {
        AssetRecordDTO dto = AssetRecordDTO.builder()
                .id(record.getId())
                .assetId(record.getAssetId())
                .recordType(record.getRecordType())
                .recordTypeName(getRecordTypeName(record.getRecordType()))
                .operatorId(record.getOperatorId())
                .fromUserId(record.getFromUserId())
                .toUserId(record.getToUserId())
                .operateDate(record.getOperateDate())
                .expectedReturnDate(record.getExpectedReturnDate())
                .actualReturnDate(record.getActualReturnDate())
                .reason(record.getReason())
                .maintenanceCost(record.getMaintenanceCost())
                .approvalStatus(record.getApprovalStatus())
                .approverId(record.getApproverId())
                .approvalComment(record.getApprovalComment())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .build();
        
        // 查询资产信息
        Asset asset = assetRepository.getById(record.getAssetId());
        if (asset != null) {
            dto.setAssetNo(asset.getAssetNo());
            dto.setAssetName(asset.getName());
        }
        
        return dto;
    }

    private String getCategoryName(String category) {
        return switch (category) {
            case "OFFICE" -> "办公设备";
            case "IT" -> "IT设备";
            case "FURNITURE" -> "家具";
            case "VEHICLE" -> "车辆";
            default -> "其他";
        };
    }

    private String getStatusName(String status) {
        return switch (status) {
            case "IDLE" -> "闲置";
            case "IN_USE" -> "使用中";
            case "MAINTENANCE" -> "维修中";
            case "SCRAPPED" -> "已报废";
            default -> status;
        };
    }

    private String getRecordTypeName(String recordType) {
        return switch (recordType) {
            case "RECEIVE" -> "领用";
            case "RETURN" -> "归还";
            case "TRANSFER" -> "转移";
            case "MAINTENANCE" -> "维修";
            case "SCRAP" -> "报废";
            default -> recordType;
        };
    }
}
