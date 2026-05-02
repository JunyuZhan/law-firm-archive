package com.archivesystem.dto.fonds;

import com.archivesystem.entity.Fonds;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全宗响应 DTO.
 */
@Data
@Builder
public class FondsResponse {
    private Long id;
    private String fondsNo;
    private String fondsName;
    private String fondsType;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static FondsResponse from(Fonds fonds) {
        return FondsResponse.builder()
                .id(fonds.getId())
                .fondsNo(fonds.getFondsNo())
                .fondsName(fonds.getFondsName())
                .fondsType(fonds.getFondsType())
                .description(fonds.getDescription())
                .status(fonds.getStatus())
                .createdAt(fonds.getCreatedAt())
                .build();
    }
}
