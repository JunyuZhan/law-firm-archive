package com.lawfirm.application.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.application.archive.dto.ArchiveLocationDTO;
import com.lawfirm.application.archive.dto.LocationCapacityDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import com.lawfirm.domain.archive.repository.ArchiveLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 档案库位应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveLocationAppService {

    private final ArchiveLocationRepository locationRepository;

    /**
     * 查询所有库位
     */
    public List<ArchiveLocationDTO> listLocations() {
        List<ArchiveLocation> locations = locationRepository.list(
                new LambdaQueryWrapper<ArchiveLocation>()
                        .orderByAsc(ArchiveLocation::getRoom)
                        .orderByAsc(ArchiveLocation::getCabinet)
                        .orderByAsc(ArchiveLocation::getShelf)
                        .orderByAsc(ArchiveLocation::getPosition)
        );
        return locations.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询可用库位
     */
    public List<ArchiveLocationDTO> listAvailableLocations() {
        List<ArchiveLocation> locations = locationRepository.list(
                new LambdaQueryWrapper<ArchiveLocation>()
                        .eq(ArchiveLocation::getStatus, "AVAILABLE")
                        .orderByAsc(ArchiveLocation::getRoom)
                        .orderByAsc(ArchiveLocation::getCabinet)
        );
        return locations.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取库位详情
     */
    public ArchiveLocationDTO getLocationById(Long id) {
        ArchiveLocation location = locationRepository.getByIdOrThrow(id, "库位不存在");
        return toDTO(location);
    }

    /**
     * 创建库位
     */
    @Transactional
    public ArchiveLocationDTO createLocation(ArchiveLocationDTO dto) {
        // 检查编码唯一性
        if (locationRepository.count(
                new LambdaQueryWrapper<ArchiveLocation>()
                        .eq(ArchiveLocation::getLocationCode, dto.getLocationCode())) > 0) {
            throw new BusinessException("库位编码已存在");
        }

        ArchiveLocation location = ArchiveLocation.builder()
                .locationCode(dto.getLocationCode())
                .locationName(dto.getLocationName())
                .room(dto.getRoom())
                .cabinet(dto.getCabinet())
                .shelf(dto.getShelf())
                .position(dto.getPosition())
                .totalCapacity(dto.getTotalCapacity())
                .usedCapacity(0)
                .status(dto.getStatus() != null ? dto.getStatus() : "AVAILABLE")
                .remarks(dto.getRemarks())
                .build();

        locationRepository.save(location);
        log.info("库位创建成功: {}", location.getLocationCode());
        return toDTO(location);
    }

    /**
     * 更新库位
     */
    @Transactional
    public ArchiveLocationDTO updateLocation(Long id, ArchiveLocationDTO dto) {
        ArchiveLocation location = locationRepository.getByIdOrThrow(id, "库位不存在");
        
        if (dto.getLocationName() != null) {
            location.setLocationName(dto.getLocationName());
        }
        if (dto.getRoom() != null) {
            location.setRoom(dto.getRoom());
        }
        if (dto.getCabinet() != null) {
            location.setCabinet(dto.getCabinet());
        }
        if (dto.getShelf() != null) {
            location.setShelf(dto.getShelf());
        }
        if (dto.getPosition() != null) {
            location.setPosition(dto.getPosition());
        }
        if (dto.getTotalCapacity() != null) {
            location.setTotalCapacity(dto.getTotalCapacity());
        }
        if (dto.getStatus() != null) {
            location.setStatus(dto.getStatus());
        }
        if (dto.getRemarks() != null) {
            location.setRemarks(dto.getRemarks());
        }

        locationRepository.updateById(location);
        log.info("库位更新成功: {}", location.getLocationCode());
        return toDTO(location);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "AVAILABLE" -> "可用";
            case "FULL" -> "已满";
            case "MAINTENANCE" -> "维护中";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private ArchiveLocationDTO toDTO(ArchiveLocation location) {
        ArchiveLocationDTO dto = new ArchiveLocationDTO();
        dto.setId(location.getId());
        dto.setLocationCode(location.getLocationCode());
        dto.setLocationName(location.getLocationName());
        dto.setRoom(location.getRoom());
        dto.setCabinet(location.getCabinet());
        dto.setShelf(location.getShelf());
        dto.setPosition(location.getPosition());
        dto.setTotalCapacity(location.getTotalCapacity());
        dto.setUsedCapacity(location.getUsedCapacity());
        dto.setAvailableCapacity(location.getTotalCapacity() - location.getUsedCapacity());
        dto.setStatus(location.getStatus());
        dto.setStatusName(getStatusName(location.getStatus()));
        dto.setRemarks(location.getRemarks());
        dto.setCreatedAt(location.getCreatedAt());
        dto.setUpdatedAt(location.getUpdatedAt());
        return dto;
    }

    /**
     * 库位容量监控（M7-014）
     */
    public List<LocationCapacityDTO> monitorCapacity() {
        List<ArchiveLocation> locations = locationRepository.list(
                new LambdaQueryWrapper<ArchiveLocation>()
                        .orderByAsc(ArchiveLocation::getRoom)
                        .orderByAsc(ArchiveLocation::getCabinet)
        );

        return locations.stream().map(location -> {
            LocationCapacityDTO dto = new LocationCapacityDTO();
            dto.setLocationId(location.getId());
            dto.setLocationCode(location.getLocationCode());
            dto.setLocationName(location.getLocationName());
            dto.setTotalCapacity(location.getTotalCapacity());
            dto.setUsedCapacity(location.getUsedCapacity());
            dto.setAvailableCapacity(location.getTotalCapacity() - location.getUsedCapacity());
            
            // 计算使用率
            if (location.getTotalCapacity() != null && location.getTotalCapacity() > 0) {
                double usageRate = (location.getUsedCapacity() != null ? location.getUsedCapacity().doubleValue() : 0.0) 
                        / location.getTotalCapacity() * 100;
                dto.setUsageRate(usageRate);
            } else {
                dto.setUsageRate(0.0);
            }
            
            dto.setStatus(location.getStatus());
            dto.setStatusName(getStatusName(location.getStatus()));
            return dto;
        }).collect(Collectors.toList());
    }
}

