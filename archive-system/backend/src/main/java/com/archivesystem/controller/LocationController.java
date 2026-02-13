package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存放位置控制器.
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "存放位置管理", description = "管理档案存放位置")
public class LocationController {

    private final LocationService locationService;

    /**
     * 获取位置列表（分页）.
     */
    @GetMapping
    @Operation(summary = "获取位置列表")
    public Result<PageResult<ArchiveLocation>> list(
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<ArchiveLocation> result = locationService.getList(roomName, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取所有位置（下拉选择用）.
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有位置")
    public Result<List<ArchiveLocation>> getAll() {
        List<ArchiveLocation> locations = locationService.getAll();
        return Result.success(locations);
    }

    /**
     * 获取可用位置.
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用位置")
    public Result<List<ArchiveLocation>> getAvailable() {
        List<ArchiveLocation> locations = locationService.getAvailable();
        return Result.success(locations);
    }

    /**
     * 获取库房列表.
     */
    @GetMapping("/rooms")
    @Operation(summary = "获取库房列表")
    public Result<List<String>> getRooms() {
        List<String> rooms = locationService.getRoomNames();
        return Result.success(rooms);
    }

    /**
     * 根据库房获取位置.
     */
    @GetMapping("/room/{roomName}")
    @Operation(summary = "根据库房获取位置")
    public Result<List<ArchiveLocation>> getByRoom(@PathVariable String roomName) {
        List<ArchiveLocation> locations = locationService.getByRoom(roomName);
        return Result.success(locations);
    }

    /**
     * 获取位置详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取位置详情")
    public Result<ArchiveLocation> getById(@PathVariable Long id) {
        ArchiveLocation location = locationService.getById(id);
        return Result.success(location);
    }

    /**
     * 创建位置.
     */
    @PostMapping
    @Operation(summary = "创建位置")
    public Result<ArchiveLocation> create(@RequestBody ArchiveLocation location) {
        ArchiveLocation created = locationService.create(location);
        return Result.success("创建成功", created);
    }

    /**
     * 更新位置.
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新位置")
    public Result<ArchiveLocation> update(@PathVariable Long id, @RequestBody ArchiveLocation location) {
        ArchiveLocation updated = locationService.update(id, location);
        return Result.success("更新成功", updated);
    }

    /**
     * 删除位置.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除位置")
    public Result<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return Result.success("删除成功", null);
    }
}
