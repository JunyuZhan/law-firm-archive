package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.dto.fonds.FondsOptionResponse;
import com.archivesystem.dto.fonds.FondsResponse;
import com.archivesystem.dto.fonds.FondsStatisticsResponse;
import com.archivesystem.dto.fonds.FondsSummaryResponse;
import com.archivesystem.entity.Fonds;
import com.archivesystem.service.FondsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 全宗管理控制器.
 * @author junyuzhan
 */
@RestController
@RequestMapping("/fonds")
@RequiredArgsConstructor
@Tag(name = "全宗管理", description = "全宗CRUD接口")
public class FondsController {

    private final FondsService fondsService;

    @PostMapping
    @Operation(summary = "创建全宗")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<FondsSummaryResponse> create(@Valid @RequestBody Fonds fonds) {
        Fonds created = fondsService.create(fonds);
        return Result.success("创建成功", FondsSummaryResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新全宗")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<FondsSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody Fonds fonds) {
        Fonds updated = fondsService.update(id, fonds);
        return Result.success("更新成功", FondsSummaryResponse.from(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取全宗详情")
    @PreAuthorize("isAuthenticated()")
    public Result<FondsResponse> getById(@PathVariable Long id) {
        Fonds fonds = fondsService.getById(id);
        return Result.success(FondsResponse.from(fonds));
    }

    @GetMapping
    @Operation(summary = "获取全宗列表")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FondsOptionResponse>> list() {
        List<Fonds> list = fondsService.list();
        return Result.success(list.stream().map(FondsOptionResponse::from).toList());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询全宗")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<FondsSummaryResponse>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<Fonds> result = fondsService.query(keyword, pageNum, pageSize);
        return Result.success(PageResult.of(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getRecords().stream().map(FondsSummaryResponse::from).toList()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除全宗")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        fondsService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取全宗统计")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ARCHIVE_MANAGER')")
    public Result<FondsStatisticsResponse> statistics(@PathVariable Long id) {
        fondsService.getById(id);
        long archiveCount = fondsService.countArchives(id);

        return Result.success(FondsStatisticsResponse.builder()
                .archiveCount(archiveCount)
                .build());
    }
}
