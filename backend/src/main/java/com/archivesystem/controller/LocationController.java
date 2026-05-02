package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.location.LocationOptionResponse;
import com.archivesystem.dto.location.LocationResponse;
import com.archivesystem.dto.location.LocationSummaryResponse;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存放位置控制器.
 * @author junyuzhan
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Validated
@Tag(name = "存放位置管理", description = "管理档案存放位置")
public class LocationController {

    private final LocationService locationService;

    /**
     * 获取位置列表（分页）.
     */
    @GetMapping
    @Operation(summary = "获取位置列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<PageResult<LocationSummaryResponse>> list(
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<ArchiveLocation> result = locationService.getList(roomName, status, keyword, pageNum, pageSize);
        return Result.success(PageResult.of(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getRecords().stream().map(LocationSummaryResponse::from).toList()
        ));
    }

    /**
     * 获取所有位置（下拉选择用）.
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有位置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<LocationResponse>> getAll() {
        List<ArchiveLocation> locations = locationService.getAll();
        return Result.success(locations.stream().map(LocationResponse::from).toList());
    }

    /**
     * 获取可用位置.
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用位置")
    @PreAuthorize("isAuthenticated()")
    public Result<List<LocationOptionResponse>> getAvailable() {
        List<ArchiveLocation> locations = locationService.getAvailable();
        return Result.success(locations.stream().map(LocationOptionResponse::from).toList());
    }

    /**
     * 获取库房列表.
     */
    @GetMapping("/rooms")
    @Operation(summary = "获取库房列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<String>> getRooms() {
        List<String> rooms = locationService.getRoomNames();
        return Result.success(rooms);
    }

    /**
     * 根据库房获取位置.
     */
    @GetMapping("/room/{roomName}")
    @Operation(summary = "根据库房获取位置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<List<LocationResponse>> getByRoom(@PathVariable String roomName) {
        List<ArchiveLocation> locations = locationService.getByRoom(roomName);
        return Result.success(locations.stream().map(LocationResponse::from).toList());
    }

    /**
     * 获取位置详情.
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取位置详情")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<LocationResponse> getById(@PathVariable Long id) {
        ArchiveLocation location = locationService.getById(id);
        return Result.success(LocationResponse.from(location));
    }

    /**
     * 创建位置.
     */
    @PostMapping
    @Operation(summary = "创建位置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<LocationSummaryResponse> create(@Valid @RequestBody ArchiveLocation location) {
        ArchiveLocation created = locationService.create(location);
        return Result.success("创建成功", LocationSummaryResponse.from(created));
    }

    /**
     * 更新位置.
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新位置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<LocationSummaryResponse> update(
            @PathVariable @Parameter(description = "位置ID") Long id, 
            @Valid @RequestBody ArchiveLocation location) {
        ArchiveLocation updated = locationService.update(id, location);
        return Result.success("更新成功", LocationSummaryResponse.from(updated));
    }

    /**
     * 删除位置.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除位置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable @Parameter(description = "位置ID") Long id) {
        locationService.delete(id);
        return Result.success("删除成功", null);
    }
}
