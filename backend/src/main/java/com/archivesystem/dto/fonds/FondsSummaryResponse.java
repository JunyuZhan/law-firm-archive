package com.archivesystem.dto.fonds;

import com.archivesystem.entity.Fonds;
import lombok.Builder;
import lombok.Value;

/**
 * 全宗摘要响应 DTO.
 */
@Value
@Builder
public class FondsSummaryResponse {

    Long id;
    String fondsNo;
    String fondsName;
    String fondsType;
    String description;
    String status;

    public static FondsSummaryResponse from(Fonds fonds) {
        if (fonds == null) {
            return null;
        }

        return FondsSummaryResponse.builder()
                .id(fonds.getId())
                .fondsNo(fonds.getFondsNo())
                .fondsName(fonds.getFondsName())
                .fondsType(fonds.getFondsType())
                .description(fonds.getDescription())
                .status(fonds.getStatus())
                .build();
    }
}
