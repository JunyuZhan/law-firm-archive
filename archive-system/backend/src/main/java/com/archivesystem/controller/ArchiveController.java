package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.ArchiveDTO;
import com.archivesystem.dto.ArchiveQueryDTO;
import com.archivesystem.dto.ArchiveReceiveDTO;
import com.archivesystem.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 档案管理控制器.
 */
@Tag(name = "档案管理")
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 接收外部系统的档案.
     * 这是核心接口，用于接收律所系统或其他系统推送的档案。
     */
    @Operation(summary = "接收档案", description = "接收来自律所系统或其他外部系统推送的档案数据")
    @PostMapping("/receive")
    public Result<ArchiveDTO> receiveArchive(@Valid @RequestBody ArchiveReceiveDTO dto) {
        try {
            ArchiveDTO result = archiveService.receiveArchive(dto);
            return Result.success("档案接收成功", result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询档案列表.
     */
    @Operation(summary = "档案列表", description = "分页查询档案列表")
    @GetMapping
    public Result<PageResult<ArchiveDTO>> listArchives(ArchiveQueryDTO query) {
        PageResult<ArchiveDTO> result = archiveService.listArchives(query);
        return Result.success(result);
    }

    /**
     * 获取档案详情.
     */
    @Operation(summary = "档案详情", description = "获取档案详细信息，包含文件列表")
    @GetMapping("/{id}")
    public Result<ArchiveDTO> getArchive(
            @Parameter(description = "档案ID") @PathVariable Long id) {
        try {
            ArchiveDTO result = archiveService.getArchiveById(id);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 档案入库.
     */
    @Operation(summary = "档案入库", description = "将已接收的档案入库到指定位置")
    @PostMapping("/{id}/store")
    public Result<Void> storeArchive(
            @Parameter(description = "档案ID") @PathVariable Long id,
            @Parameter(description = "存放位置ID") @RequestParam Long locationId,
            @Parameter(description = "盒号") @RequestParam(required = false) String boxNo) {
        try {
            archiveService.storeArchive(id, locationId, boxNo);
            return Result.success("档案入库成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取档案统计.
     */
    @Operation(summary = "档案统计", description = "获取档案统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> result = archiveService.getStatistics();
        return Result.success(result);
    }
}
