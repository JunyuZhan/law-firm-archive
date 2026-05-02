package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.retention.RetentionPeriodResponse;
import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.service.RetentionPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 保管期限控制器
 * @author junyuzhan
 */
@RestController
@RequestMapping("/retention-periods")
@RequiredArgsConstructor
@Tag(name = "保管期限", description = "保管期限管理接口")
public class RetentionPeriodController {

    private final RetentionPeriodService retentionPeriodService;

    /**
     * 获取所有保管期限
     */
    @GetMapping
    @Operation(summary = "获取保管期限列表", description = "获取所有保管期限，按排序号升序")
    @PreAuthorize("isAuthenticated()")
    public Result<List<RetentionPeriodResponse>> list() {
        return Result.success(retentionPeriodService.listAll().stream()
                .map(RetentionPeriodResponse::from)
                .toList());
    }
}
