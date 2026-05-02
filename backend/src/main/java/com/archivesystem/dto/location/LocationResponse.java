package com.archivesystem.dto.location;

import com.archivesystem.entity.ArchiveLocation;
import lombok.Builder;
import lombok.Value;

/**
 * 位置响应DTO.
 */
@Value
@Builder
public class LocationResponse {

    Long id;
    String locationCode;
    String locationName;
    String roomName;
    String area;
    String shelfNo;
    String layerNo;
    Integer totalCapacity;
    Integer usedCapacity;
    String status;
    String remarks;

    public static LocationResponse from(ArchiveLocation location) {
        if (location == null) {
            return null;
        }

        return LocationResponse.builder()
                .id(location.getId())
                .locationCode(location.getLocationCode())
                .locationName(location.getLocationName())
                .roomName(location.getRoomName())
                .area(location.getArea())
                .shelfNo(location.getShelfNo())
                .layerNo(location.getLayerNo())
                .totalCapacity(location.getTotalCapacity())
                .usedCapacity(location.getUsedCapacity())
                .status(location.getStatus())
                .remarks(location.getRemarks())
                .build();
    }
}
