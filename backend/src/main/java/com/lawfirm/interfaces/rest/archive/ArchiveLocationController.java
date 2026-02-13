package com.lawfirm.interfaces.rest.archive;

import com.lawfirm.application.archive.dto.ArchiveLocationDTO;
import com.lawfirm.application.archive.service.ArchiveLocationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 档案库位管理 Controller */
@RestController
@RequestMapping("/archive/location")
@RequiredArgsConstructor
public class ArchiveLocationController {

  /** 档案库位应用服务 */
  private final ArchiveLocationAppService locationAppService;

  /**
   * 查询所有库位
   *
   * @return 库位列表
   */
  @GetMapping("/list")
  @RequirePermission("archive:location:list")
  public Result<List<ArchiveLocationDTO>> listLocations() {
    List<ArchiveLocationDTO> locations = locationAppService.listLocations();
    return Result.success(locations);
  }

  /**
   * 查询可用库位
   *
   * @return 可用库位列表
   */
  @GetMapping("/available")
  @RequirePermission("archive:location:list")
  public Result<List<ArchiveLocationDTO>> listAvailableLocations() {
    List<ArchiveLocationDTO> locations = locationAppService.listAvailableLocations();
    return Result.success(locations);
  }

  /**
   * 获取库位详情
   *
   * @param id 库位ID
   * @return 库位详情
   */
  @GetMapping("/{id}")
  @RequirePermission("archive:location:list")
  public Result<ArchiveLocationDTO> getLocation(@PathVariable final Long id) {
    ArchiveLocationDTO location = locationAppService.getLocationById(id);
    return Result.success(location);
  }

  /**
   * 创建库位
   *
   * @param dto 库位信息
   * @return 库位信息
   */
  @PostMapping
  @RequirePermission("archive:location:create")
  @OperationLog(module = "档案库位", action = "创建库位")
  public Result<ArchiveLocationDTO> createLocation(@RequestBody final ArchiveLocationDTO dto) {
    ArchiveLocationDTO location = locationAppService.createLocation(dto);
    return Result.success(location);
  }

  /**
   * 更新库位
   *
   * @param id 库位ID
   * @param dto 库位信息
   * @return 库位信息
   */
  @PutMapping("/{id}")
  @RequirePermission("archive:location:update")
  @OperationLog(module = "档案库位", action = "更新库位")
  public Result<ArchiveLocationDTO> updateLocation(
      @PathVariable final Long id, @RequestBody final ArchiveLocationDTO dto) {
    ArchiveLocationDTO location = locationAppService.updateLocation(id, dto);
    return Result.success(location);
  }

  /**
   * 库位容量监控（M7-014）
   *
   * @return 库位容量列表
   */
  @GetMapping("/capacity-monitor")
  @RequirePermission("archive:location:list")
  public Result<List<com.lawfirm.application.archive.dto.LocationCapacityDTO>> monitorCapacity() {
    return Result.success(locationAppService.monitorCapacity());
  }
}
