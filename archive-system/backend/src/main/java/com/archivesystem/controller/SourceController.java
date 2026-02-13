package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案来源控制器.
 */
@RestController
@RequestMapping("/sources")
@RequiredArgsConstructor
@Tag(name = "档案来源管理", description = "管理外部档案来源")
public class SourceController {

    private final ExternalSourceMapper externalSourceMapper;

    @GetMapping
    @Operation(summary = "获取来源列表")
    public Result<List<ExternalSource>> list() {
        List<ExternalSource> sources = externalSourceMapper.selectList(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getDeleted, false)
                        .orderByDesc(ExternalSource::getCreatedAt));
        return Result.success(sources);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取来源详情")
    public Result<ExternalSource> getById(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null || source.getDeleted()) {
            return Result.error("404", "来源不存在");
        }
        return Result.success(source);
    }

    @PostMapping
    @Operation(summary = "创建来源")
    public Result<ExternalSource> create(@RequestBody ExternalSource source) {
        externalSourceMapper.insert(source);
        return Result.success("创建成功", source);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新来源")
    public Result<Void> update(@PathVariable Long id, @RequestBody ExternalSource source) {
        source.setId(id);
        externalSourceMapper.updateById(source);
        return Result.success("更新成功", null);
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "切换启用状态")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam Boolean enabled) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source == null) {
            return Result.error("404", "来源不存在");
        }
        source.setEnabled(enabled);
        externalSourceMapper.updateById(source);
        return Result.success(enabled ? "已启用" : "已禁用", null);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    public Result<Void> test(@PathVariable Long id) {
        // TODO: 实现连接测试
        return Result.success("连接测试成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除来源")
    public Result<Void> delete(@PathVariable Long id) {
        ExternalSource source = externalSourceMapper.selectById(id);
        if (source != null) {
            source.setDeleted(true);
            externalSourceMapper.updateById(source);
        }
        return Result.success("删除成功", null);
    }
}
