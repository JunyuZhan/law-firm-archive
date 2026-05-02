package com.archivesystem.dto.borrow;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 借阅可用性检查响应 DTO.
 */
@Value
@Builder
public class BorrowAvailabilityResponse {

    boolean available;
    boolean hasCurrentApplication;
    List<String> allowedBorrowTypes;
    String unavailableReason;
    BorrowRulesResponse borrowRules;

    @Value
    @Builder
    public static class BorrowRulesResponse {
        Map<String, Integer> maxBorrowDays;
        String ruleSummary;
    }
}
