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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
    
    // 问题292修复：资产编号生成序列号，避免并发冲突
    private final AtomicLong assetSequence = new AtomicLong(0);

    /**
     * 分页查询资产
     * 问题290修复：使用批量加载避免N+1查询
     */
    public PageResult<AssetDTO> listAssets(PageQuery query, String keyword, String category, 
                                            String status, Long departmentId) {
        Page<Asset> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<Asset> result = assetRepository.findPage(page, keyword, category, status, departmentId, null);
        List<Asset> assets = result.getRecords();
        
        if (assets.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
        }
        
        // 批量加载当前使用人信息，避免N+1查询
        Set<Long> userIds = assets.stream()
                .map(Asset::getCurrentUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userRepository.listByIds(new ArrayList<>(userIds)).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        
        List<AssetDTO> items = assets.stream()
                .map(a -> toDTO(a, userMap))
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
     * 问题294修复：检查历史记录，防止孤儿数据
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
        
        // 检查是否有历史记录
        long recordCount = assetRecordRepository.countByAssetId(id);
        if (recordCount > 0) {
            throw new BusinessException("该资产有" + recordCount + "条历史记录，无法删除。建议使用报废功能。");
        }
        
        assetRepository.removeById(id);
        log.info("删除资产: {}", asset.getAssetNo());
    }

    /**
     * 资产领用
     * 权限控制：
     * - 普通用户只能为自己领用
     * - 管理员/资产管理员可以代他人领用
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
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Long targetUserId = command.getUserId();
        
        // 权限检查：只能为自己领用，除非是管理员/资产管理员
        if (targetUserId != null && !targetUserId.equals(currentUserId)) {
            // 检查是否有管理员权限
            if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("ASSET_MANAGER")) {
                throw new BusinessException("权限不足：只能为自己领用资产，无权代他人领用");
            }
            log.warn("管理员代领用资产: operator={}, targetUser={}, assetNo={}", 
                     currentUserId, targetUserId, asset.getAssetNo());
        } else {
            targetUserId = currentUserId;
        }
        
        // 创建领用记录
        AssetRecord record = AssetRecord.builder()
                .assetId(asset.getId())
                .recordType("RECEIVE")
                .operatorId(currentUserId)
                .toUserId(targetUserId)
                .operateDate(LocalDate.now())
                .expectedReturnDate(command.getExpectedReturnDate())
                .reason(command.getReason())
                .remarks(command.getRemarks())
                .approvalStatus("APPROVED") // 简化流程，直接批准
                .build();
        
        assetRecordRepository.save(record);
        
        // 更新资产状态
        asset.setStatus("IN_USE");
        asset.setCurrentUserId(targetUserId);
        assetRepository.updateById(asset);
        
        log.info("资产领用: assetNo={}, userId={}, operator={}", asset.getAssetNo(), targetUserId, currentUserId);
    }

    /**
     * 资产归还
     * 问题291修复：验证归还人权限，只能归还自己的资产或管理员代归还
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
        
        // 验证权限：只能归还自己的资产，除非是管理员/资产管理员
        if (asset.getCurrentUserId() != null && !asset.getCurrentUserId().equals(currentUserId)) {
            if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("ASSET_MANAGER")) {
                throw new BusinessException("权限不足：只能归还自己领用的资产");
            }
            log.warn("管理员代归还资产: operator={}, currentUser={}, assetNo={}",
                     currentUserId, asset.getCurrentUserId(), asset.getAssetNo());
        }
        
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
        
        log.info("资产归还: assetNo={}, operator={}, fromUser={}", 
                 asset.getAssetNo(), currentUserId, asset.getCurrentUserId());
    }

    /**
     * 资产报废申请
     * 问题300修复：使用中间状态PENDING_SCRAP，审批通过后才变为SCRAPPED
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
        if ("SCRAPPED".equals(asset.getStatus())) {
            throw new BusinessException("该资产已报废");
        }
        if ("PENDING_SCRAP".equals(asset.getStatus())) {
            throw new BusinessException("该资产已提交报废申请，请等待审批");
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
        
        // 更新资产状态为待报废，等审批通过后再变为已报废
        asset.setStatus("PENDING_SCRAP");
        assetRepository.updateById(asset);
        
        log.info("资产报废申请: assetNo={}, 等待审批", asset.getAssetNo());
    }
    
    /**
     * 审批报废申请
     * 问题300修复：新增审批回调方法
     */
    @Transactional
    public void approveScrap(Long assetId, boolean approved, String comment) {
        Asset asset = assetRepository.getById(assetId);
        if (asset == null) {
            throw new BusinessException("资产不存在");
        }
        if (!"PENDING_SCRAP".equals(asset.getStatus())) {
            throw new BusinessException("该资产不在待报废状态");
        }
        
        if (approved) {
            asset.setStatus("SCRAPPED");
            log.info("资产报废审批通过: {}", asset.getAssetNo());
        } else {
            asset.setStatus("IDLE");  // 恢复为闲置状态
            log.info("资产报废审批拒绝: {}, 原因: {}", asset.getAssetNo(), comment);
        }
        
        assetRepository.updateById(asset);
    }

    /**
     * 获取资产操作记录
     * 问题293修复：优化N+1查询，只查询一次资产信息
     */
    public List<AssetRecordDTO> getAssetRecords(Long assetId) {
        List<AssetRecord> records = assetRecordRepository.findByAssetId(assetId);
        
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 只查询一次资产信息（所有记录都是同一个资产）
        Asset asset = assetRepository.getById(assetId);
        
        return records.stream()
                .map(r -> toRecordDTO(r, asset))
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

    /**
     * 生成资产编号
     * 问题292修复：使用日期+序列号+随机数避免并发冲突
     */
    private String generateAssetNo(String category) {
        String prefix = switch (category) {
            case "OFFICE" -> "OF";
            case "IT" -> "IT";
            case "FURNITURE" -> "FN";
            case "VEHICLE" -> "VH";
            default -> "OT";
        };
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = assetSequence.incrementAndGet() % 1000;
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("%s%s%03d%s", prefix, date, seq, random);
    }

    /**
     * 转换为DTO（单个资产，会查询用户）
     */
    private AssetDTO toDTO(Asset asset) {
        return toDTO(asset, null);
    }
    
    /**
     * 转换为DTO（批量优化版本，从Map获取用户信息）
     * 问题290修复：支持批量加载
     */
    private AssetDTO toDTO(Asset asset, Map<Long, User> userMap) {
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
        
        // 查询当前使用人名称（从Map获取或单独查询）
        if (asset.getCurrentUserId() != null) {
            User user = (userMap != null) ? userMap.get(asset.getCurrentUserId()) 
                                          : userRepository.getById(asset.getCurrentUserId());
            if (user != null) {
                dto.setCurrentUserName(user.getRealName());
            }
        }
        
        return dto;
    }

    /**
     * 转换为记录DTO（优化版本，直接使用传入的Asset对象）
     * 问题293修复：避免N+1查询
     */
    private AssetRecordDTO toRecordDTO(AssetRecord record, Asset asset) {
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
        
        // 从参数获取资产信息，避免查询
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
