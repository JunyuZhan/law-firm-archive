package com.archivesystem.dto.location;

import com.archivesystem.entity.ArchiveLocation;
import lombok.Builder;
import lombok.Data;

/**
 * 存放位置下拉响应 DTO.
 */
@Data
@Builder
public class LocationOptionResponse {
    private Long id;
    private String locationName;
    private String roomName;
    private String shelfNo;

    public static LocationOptionResponse from(ArchiveLocation location) {
        return LocationOptionResponse.builder()
                .id(location.getId())
                .locationName(location.getLocationName())
                .roomName(location.getRoomName())
                .shelfNo(location.getShelfNo())
                .build();
    }
}
