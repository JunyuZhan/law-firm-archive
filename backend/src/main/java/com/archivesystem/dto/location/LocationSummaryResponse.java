package com.archivesystem.dto.location;

import com.archivesystem.entity.ArchiveLocation;
import lombok.Builder;
import lombok.Value;

/**
 * 位置摘要响应 DTO.
 */
@Value
@Builder
public class LocationSummaryResponse {

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

    public static LocationSummaryResponse from(ArchiveLocation location) {
        if (location == null) {
            return null;
        }

        return LocationSummaryResponse.builder()
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
                .build();
    }
}
