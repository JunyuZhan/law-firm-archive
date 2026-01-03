package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.dto.ArchiveLocationDTO;
import com.lawfirm.application.archive.service.ArchiveLocationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 档案库位管理 Controller
 */
@RestController
@RequestMapping("/archive/location")
@RequiredArgsConstructor
public class ArchiveLocationController {

    private final ArchiveLocationAppService locationAppService;

    /**
     * 查询所有库位
     */
    @GetMapping("/list")
    @RequirePermission("archive:location:list")
    public Result<List<ArchiveLocationDTO>> listLocations() {
        List<ArchiveLocationDTO> locations = locationAppService.listLocations();
        return Result.success(locations);
    }

    /**
     * 查询可用库位
     */
    @GetMapping("/available")
    @RequirePermission("archive:location:list")
    public Result<List<ArchiveLocationDTO>> listAvailableLocations() {
        List<ArchiveLocationDTO> locations = locationAppService.listAvailableLocations();
        return Result.success(locations);
    }

    /**
     * 获取库位详情
     */
    @GetMapping("/{id}")
    @RequirePermission("archive:location:list")
    public Result<ArchiveLocationDTO> getLocation(@PathVariable Long id) {
        ArchiveLocationDTO location = locationAppService.getLocationById(id);
        return Result.success(location);
    }

    /**
     * 创建库位
     */
    @PostMapping
    @RequirePermission("archive:location:create")
    @OperationLog(module = "档案库位", action = "创建库位")
    public Result<ArchiveLocationDTO> createLocation(@RequestBody ArchiveLocationDTO dto) {
        ArchiveLocationDTO location = locationAppService.createLocation(dto);
        return Result.success(location);
    }

    /**
     * 更新库位
     */
    @PutMapping("/{id}")
    @RequirePermission("archive:location:update")
    @OperationLog(module = "档案库位", action = "更新库位")
    public Result<ArchiveLocationDTO> updateLocation(@PathVariable Long id, @RequestBody ArchiveLocationDTO dto) {
        ArchiveLocationDTO location = locationAppService.updateLocation(id, dto);
        return Result.success(location);
    }

    /**
     * 库位容量监控（M7-014）
     */
    @GetMapping("/capacity-monitor")
    @RequirePermission("archive:location:list")
    public Result<List<com.lawfirm.application.archive.dto.LocationCapacityDTO>> monitorCapacity() {
        return Result.success(locationAppService.monitorCapacity());
    }
}

