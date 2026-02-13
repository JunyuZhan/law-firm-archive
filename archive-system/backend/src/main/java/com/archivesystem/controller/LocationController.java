package com.archivesystem.controller;

import com.archivesystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 存放位置控制器.
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "存放位置管理", description = "管理档案存放位置")
public class LocationController {

    @GetMapping
    @Operation(summary = "获取位置列表")
    public Result<List<Map<String, Object>>> list() {
        // TODO: 实现位置管理功能
        return Result.success(Collections.emptyList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取位置详情")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        // TODO: 实现位置详情
        return Result.error("404", "位置不存在");
    }

    @PostMapping
    @Operation(summary = "创建位置")
    public Result<Void> create(@RequestBody Map<String, Object> data) {
        // TODO: 实现创建位置
        return Result.error("501", "功能尚未实现");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新位置")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        // TODO: 实现更新位置
        return Result.error("501", "功能尚未实现");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除位置")
    public Result<Void> delete(@PathVariable Long id) {
        // TODO: 实现删除位置
        return Result.error("501", "功能尚未实现");
    }
}
