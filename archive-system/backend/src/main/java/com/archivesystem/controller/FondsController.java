package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.Fonds;
import com.archivesystem.service.FondsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全宗管理控制器.
 */
@RestController
@RequestMapping("/fonds")
@RequiredArgsConstructor
@Tag(name = "全宗管理", description = "全宗CRUD接口")
public class FondsController {

    private final FondsService fondsService;

    @PostMapping
    @Operation(summary = "创建全宗")
    public Result<Fonds> create(@Valid @RequestBody Fonds fonds) {
        Fonds created = fondsService.create(fonds);
        return Result.success("创建成功", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新全宗")
    public Result<Fonds> update(@PathVariable Long id, @Valid @RequestBody Fonds fonds) {
        Fonds updated = fondsService.update(id, fonds);
        return Result.success("更新成功", updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取全宗详情")
    public Result<Fonds> getById(@PathVariable Long id) {
        Fonds fonds = fondsService.getById(id);
        return Result.success(fonds);
    }

    @GetMapping
    @Operation(summary = "获取全宗列表")
    public Result<List<Fonds>> list() {
        List<Fonds> list = fondsService.list();
        return Result.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询全宗")
    public Result<PageResult<Fonds>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<Fonds> result = fondsService.query(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除全宗")
    public Result<Void> delete(@PathVariable Long id) {
        fondsService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取全宗统计")
    public Result<Map<String, Object>> statistics(@PathVariable Long id) {
        Fonds fonds = fondsService.getById(id);
        long archiveCount = fondsService.countArchives(id);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("fonds", fonds);
        stats.put("archiveCount", archiveCount);
        
        return Result.success(stats);
    }
}
