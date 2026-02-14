package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.repository.RetentionPeriodMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 */
@RestController
@RequestMapping("/retention-periods")
@RequiredArgsConstructor
@Tag(name = "保管期限", description = "保管期限管理接口")
public class RetentionPeriodController {

    private final RetentionPeriodMapper retentionPeriodMapper;

    /**
     * 获取所有保管期限
     */
    @GetMapping
    @Operation(summary = "获取保管期限列表", description = "获取所有保管期限，按排序号升序")
    @PreAuthorize("isAuthenticated()")
    public Result<List<RetentionPeriod>> list() {
        LambdaQueryWrapper<RetentionPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RetentionPeriod::getSortOrder);
        List<RetentionPeriod> list = retentionPeriodMapper.selectList(wrapper);
        return Result.success(list);
    }
}
