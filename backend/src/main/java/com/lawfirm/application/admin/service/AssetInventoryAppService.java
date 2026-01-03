package com.lawfirm.application.admin.service;

import com.lawfirm.application.admin.command.CreateAssetInventoryCommand;
import com.lawfirm.application.admin.dto.AssetInventoryDTO;
import com.lawfirm.application.admin.dto.AssetInventoryDetailDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.AssetInventory;
import com.lawfirm.domain.admin.entity.AssetInventoryDetail;
import com.lawfirm.domain.admin.repository.AssetInventoryRepository;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryDetailMapper;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 资产盘点服务（M8-033）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInventoryAppService {

    private final AssetInventoryRepository inventoryRepository;
    private final AssetInventoryMapper inventoryMapper;
    private final AssetInventoryDetailMapper detailMapper;
    private final AssetRepository assetRepository;

    /**
     * 创建资产盘点
     */
    @Transactional
    public AssetInventoryDTO createInventory(CreateAssetInventoryCommand command) {
        Long userId = SecurityUtils.getUserId();

        // 生成盘点编号
        String inventoryNo = generateInventoryNo();

        AssetInventory inventory = AssetInventory.builder()
                .inventoryNo(inventoryNo)
                .inventoryDate(command.getInventoryDate())
                .inventoryType(command.getInventoryType())
                .departmentId(command.getDepartmentId())
                .location(command.getLocation())
                .status(AssetInventory.STATUS_IN_PROGRESS)
                .totalCount(0)
                .actualCount(0)
                .surplusCount(0)
                .shortageCount(0)
                .remark(command.getRemark())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        inventoryRepository.save(inventory);

        // 如果是全盘，获取所有资产；如果是抽盘，使用指定的资产ID列表
        List<Asset> assets;
        if (AssetInventory.TYPE_FULL.equals(command.getInventoryType())) {
            assets = assetRepository.list();
        } else {
            if (command.getAssetIds() == null || command.getAssetIds().isEmpty()) {
                throw new BusinessException("抽盘时必须指定资产ID列表");
            }
            assets = command.getAssetIds().stream()
                    .map(assetRepository::findById)
                    .filter(asset -> asset != null)
                    .collect(Collectors.toList());
        }

        // 创建盘点明细
        for (Asset asset : assets) {
            AssetInventoryDetail detail = AssetInventoryDetail.builder()
                    .inventoryId(inventory.getId())
                    .assetId(asset.getId())
                    .expectedStatus(asset.getStatus())
                    .expectedLocation(asset.getLocation())
                    .expectedUserId(asset.getCurrentUserId())
                    .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                    .build();
            detailMapper.insert(detail);
        }

        inventory.setTotalCount(assets.size());
        inventoryRepository.updateById(inventory);

        log.info("资产盘点创建成功: inventoryNo={}, type={}, count={}", inventoryNo, command.getInventoryType(), assets.size());
        return getInventoryById(inventory.getId());
    }

    /**
     * 更新盘点明细
     */
    @Transactional
    public void updateInventoryDetail(Long detailId, String actualStatus, String actualLocation, Long actualUserId, String discrepancyDesc) {
        AssetInventoryDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException("盘点明细不存在");
        }

        AssetInventory inventory = inventoryRepository.getByIdOrThrow(detail.getInventoryId(), "盘点不存在");
        if (AssetInventory.STATUS_COMPLETED.equals(inventory.getStatus())) {
            throw new BusinessException("盘点已完成，不能修改");
        }

        detail.setActualStatus(actualStatus);
        detail.setActualLocation(actualLocation);
        detail.setActualUserId(actualUserId);
        detail.setDiscrepancyDesc(discrepancyDesc);

        // 判断差异类型
        String discrepancyType = AssetInventoryDetail.DISCREPANCY_NORMAL;
        if (!detail.getExpectedStatus().equals(actualStatus)) {
            discrepancyType = AssetInventoryDetail.DISCREPANCY_STATUS;
        } else if (detail.getExpectedLocation() != null && !detail.getExpectedLocation().equals(actualLocation)) {
            discrepancyType = AssetInventoryDetail.DISCREPANCY_LOCATION;
        } else if (detail.getExpectedUserId() != null && !detail.getExpectedUserId().equals(actualUserId)) {
            discrepancyType = AssetInventoryDetail.DISCREPANCY_LOCATION;
        }
        detail.setDiscrepancyType(discrepancyType);

        detailMapper.updateById(detail);
    }

    /**
     * 完成盘点
     */
    @Transactional
    public AssetInventoryDTO completeInventory(Long inventoryId) {
        AssetInventory inventory = inventoryRepository.getByIdOrThrow(inventoryId, "盘点不存在");
        
        if (AssetInventory.STATUS_COMPLETED.equals(inventory.getStatus())) {
            throw new BusinessException("盘点已完成");
        }

        // 统计盘点结果
        List<AssetInventoryDetail> details = detailMapper.selectByInventoryId(inventoryId);
        int actualCount = 0;
        int surplusCount = 0;
        int shortageCount = 0;

        for (AssetInventoryDetail detail : details) {
            if (!AssetInventoryDetail.DISCREPANCY_NORMAL.equals(detail.getDiscrepancyType())) {
                actualCount++;
                if (AssetInventoryDetail.DISCREPANCY_SURPLUS.equals(detail.getDiscrepancyType())) {
                    surplusCount++;
                } else if (AssetInventoryDetail.DISCREPANCY_SHORTAGE.equals(detail.getDiscrepancyType())) {
                    shortageCount++;
                }
            } else {
                actualCount++;
            }
        }

        inventory.setActualCount(actualCount);
        inventory.setSurplusCount(surplusCount);
        inventory.setShortageCount(shortageCount);
        inventory.setStatus(AssetInventory.STATUS_COMPLETED);
        inventory.setUpdatedBy(SecurityUtils.getUserId());
        inventory.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.updateById(inventory);

        log.info("资产盘点完成: inventoryNo={}, total={}, actual={}, surplus={}, shortage={}",
                inventory.getInventoryNo(), inventory.getTotalCount(), actualCount, surplusCount, shortageCount);
        return getInventoryById(inventoryId);
    }

    /**
     * 获取盘点详情
     */
    public AssetInventoryDTO getInventoryById(Long id) {
        AssetInventory inventory = inventoryRepository.getByIdOrThrow(id, "盘点不存在");
        AssetInventoryDTO dto = toDTO(inventory);
        
        // 加载明细
        List<AssetInventoryDetail> details = detailMapper.selectByInventoryId(id);
        dto.setDetails(details.stream().map(this::toDetailDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 查询进行中的盘点
     */
    public List<AssetInventoryDTO> getInProgressInventories() {
        List<AssetInventory> inventories = inventoryMapper.selectInProgress();
        return inventories.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private String generateInventoryNo() {
        String prefix = "INV" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    private String getInventoryTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case AssetInventory.TYPE_FULL -> "全盘";
            case AssetInventory.TYPE_PARTIAL -> "抽盘";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case AssetInventory.STATUS_IN_PROGRESS -> "进行中";
            case AssetInventory.STATUS_COMPLETED -> "已完成";
            default -> status;
        };
    }

    private String getDiscrepancyTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case AssetInventoryDetail.DISCREPANCY_NORMAL -> "正常";
            case AssetInventoryDetail.DISCREPANCY_SURPLUS -> "盘盈";
            case AssetInventoryDetail.DISCREPANCY_SHORTAGE -> "盘亏";
            case AssetInventoryDetail.DISCREPANCY_LOCATION -> "位置不符";
            case AssetInventoryDetail.DISCREPANCY_STATUS -> "状态不符";
            default -> type;
        };
    }

    private AssetInventoryDTO toDTO(AssetInventory inventory) {
        AssetInventoryDTO dto = new AssetInventoryDTO();
        dto.setId(inventory.getId());
        dto.setInventoryNo(inventory.getInventoryNo());
        dto.setInventoryDate(inventory.getInventoryDate());
        dto.setInventoryType(inventory.getInventoryType());
        dto.setInventoryTypeName(getInventoryTypeName(inventory.getInventoryType()));
        dto.setDepartmentId(inventory.getDepartmentId());
        dto.setLocation(inventory.getLocation());
        dto.setStatus(inventory.getStatus());
        dto.setStatusName(getStatusName(inventory.getStatus()));
        dto.setTotalCount(inventory.getTotalCount());
        dto.setActualCount(inventory.getActualCount());
        dto.setSurplusCount(inventory.getSurplusCount());
        dto.setShortageCount(inventory.getShortageCount());
        dto.setRemark(inventory.getRemark());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }

    private AssetInventoryDetailDTO toDetailDTO(AssetInventoryDetail detail) {
        AssetInventoryDetailDTO dto = new AssetInventoryDetailDTO();
        dto.setId(detail.getId());
        dto.setInventoryId(detail.getInventoryId());
        dto.setAssetId(detail.getAssetId());
        dto.setExpectedStatus(detail.getExpectedStatus());
        dto.setActualStatus(detail.getActualStatus());
        dto.setExpectedLocation(detail.getExpectedLocation());
        dto.setActualLocation(detail.getActualLocation());
        dto.setExpectedUserId(detail.getExpectedUserId());
        dto.setActualUserId(detail.getActualUserId());
        dto.setDiscrepancyType(detail.getDiscrepancyType());
        dto.setDiscrepancyTypeName(getDiscrepancyTypeName(detail.getDiscrepancyType()));
        dto.setDiscrepancyDesc(detail.getDiscrepancyDesc());
        dto.setRemark(detail.getRemark());

        // 查询资产信息
        if (detail.getAssetId() != null) {
            Asset asset = assetRepository.findById(detail.getAssetId());
            if (asset != null) {
                dto.setAssetNo(asset.getAssetNo());
                dto.setAssetName(asset.getName());
            }
        }

        return dto;
    }
}

