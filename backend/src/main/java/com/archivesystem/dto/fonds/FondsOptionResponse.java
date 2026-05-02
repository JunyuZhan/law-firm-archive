package com.archivesystem.dto.fonds;

import com.archivesystem.entity.Fonds;
import lombok.Builder;
import lombok.Value;

/**
 * 全宗下拉选项响应 DTO.
 */
@Value
@Builder
public class FondsOptionResponse {

    Long id;
    String fondsNo;
    String fondsName;

    public static FondsOptionResponse from(Fonds fonds) {
        if (fonds == null) {
            return null;
        }

        return FondsOptionResponse.builder()
                .id(fonds.getId())
                .fondsNo(fonds.getFondsNo())
                .fondsName(fonds.getFondsName())
                .build();
    }
}
