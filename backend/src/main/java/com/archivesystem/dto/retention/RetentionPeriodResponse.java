package com.archivesystem.dto.retention;

import com.archivesystem.entity.RetentionPeriod;
import lombok.Builder;
import lombok.Value;

/**
 * 保管期限响应DTO.
 */
@Value
@Builder
public class RetentionPeriodResponse {

    String periodCode;
    String periodName;
    Integer periodYears;
    Integer sortOrder;

    public static RetentionPeriodResponse from(RetentionPeriod period) {
        if (period == null) {
            return null;
        }

        return RetentionPeriodResponse.builder()
                .periodCode(period.getPeriodCode())
                .periodName(period.getPeriodName())
                .periodYears(period.getPeriodYears())
                .sortOrder(period.getSortOrder())
                .build();
    }
}
