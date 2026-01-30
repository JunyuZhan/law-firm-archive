package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateAnnouncementCommand;
import com.lawfirm.application.system.dto.AnnouncementDTO;
import com.lawfirm.application.system.service.AnnouncementAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 系统公告接口 */
@Tag(name = "系统公告", description = "系统公告管理相关接口")
@RestController
@RequestMapping("/system/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

  /** 公告应用服务 */
  private final AnnouncementAppService announcementAppService;

  /**
   * 分页查询公告
   *
   * @param query 分页查询条件
   * @param status 状态（可选）
   * @param type 类型（可选）
   * @return 分页结果
   */
  @Operation(summary = "分页查询公告")
  @GetMapping
  public Result<PageResult<AnnouncementDTO>> listAnnouncements(
      final PageQuery query,
      @RequestParam(required = false) final String status,
      @RequestParam(required = false) final String type) {
    return Result.success(announcementAppService.listAnnouncements(query, status, type));
  }

  /**
   * 获取有效公告
   *
   * @param limit 数量限制
   * @return 有效公告列表
   */
  @Operation(summary = "获取有效公告")
  @GetMapping("/valid")
  public Result<List<AnnouncementDTO>> getValidAnnouncements(
      @RequestParam(defaultValue = "10") final int limit) {
    return Result.success(announcementAppService.getValidAnnouncements(limit));
  }

  /**
   * 获取公告详情
   *
   * @param id 公告ID
   * @return 公告详情
   */
  @Operation(summary = "获取公告详情")
  @GetMapping("/{id}")
  public Result<AnnouncementDTO> getAnnouncementById(@PathVariable final Long id) {
    return Result.success(announcementAppService.getAnnouncementById(id));
  }

  /**
   * 创建公告
   *
   * @param command 创建公告命令
   * @return 公告信息
   */
  @Operation(summary = "创建公告")
  @PostMapping
  @RequirePermission("sys:announcement:create")
  @OperationLog(module = "系统公告", action = "创建公告")
  public Result<AnnouncementDTO> createAnnouncement(
      @RequestBody final CreateAnnouncementCommand command) {
    return Result.success(announcementAppService.createAnnouncement(command));
  }

  /**
   * 更新公告
   *
   * @param id 公告ID
   * @param command 更新公告命令
   * @return 公告信息
   */
  @Operation(summary = "更新公告")
  @PutMapping("/{id}")
  @RequirePermission("sys:announcement:edit")
  @OperationLog(module = "系统公告", action = "更新公告")
  public Result<AnnouncementDTO> updateAnnouncement(
      @PathVariable final Long id, @RequestBody final CreateAnnouncementCommand command) {
    return Result.success(announcementAppService.updateAnnouncement(id, command));
  }

  /**
   * 发布公告
   *
   * @param id 公告ID
   * @return 公告信息
   */
  @Operation(summary = "发布公告")
  @PostMapping("/{id}/publish")
  @RequirePermission("sys:announcement:publish")
  @OperationLog(module = "系统公告", action = "发布公告")
  public Result<AnnouncementDTO> publishAnnouncement(@PathVariable final Long id) {
    return Result.success(announcementAppService.publishAnnouncement(id));
  }

  /**
   * 撤回公告
   *
   * @param id 公告ID
   * @return 空结果
   */
  @Operation(summary = "撤回公告")
  @PostMapping("/{id}/withdraw")
  @RequirePermission("sys:announcement:publish")
  @OperationLog(module = "系统公告", action = "撤回公告")
  public Result<Void> withdrawAnnouncement(@PathVariable final Long id) {
    announcementAppService.withdrawAnnouncement(id);
    return Result.success();
  }

  /**
   * 删除公告
   *
   * @param id 公告ID
   * @return 空结果
   */
  @Operation(summary = "删除公告")
  @DeleteMapping("/{id}")
  @RequirePermission("sys:announcement:delete")
  @OperationLog(module = "系统公告", action = "删除公告")
  public Result<Void> deleteAnnouncement(@PathVariable final Long id) {
    announcementAppService.deleteAnnouncement(id);
    return Result.success();
  }
}
