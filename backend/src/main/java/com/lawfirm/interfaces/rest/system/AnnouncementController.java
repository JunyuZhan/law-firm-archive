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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告接口
 */
@Tag(name = "系统公告", description = "系统公告管理相关接口")
@RestController
@RequestMapping("/system/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementAppService announcementAppService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    public Result<PageResult<AnnouncementDTO>> listAnnouncements(
            PageQuery query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return Result.success(announcementAppService.listAnnouncements(query, status, type));
    }

    @Operation(summary = "获取有效公告")
    @GetMapping("/valid")
    public Result<List<AnnouncementDTO>> getValidAnnouncements(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(announcementAppService.getValidAnnouncements(limit));
    }

    @Operation(summary = "获取公告详情")
    @GetMapping("/{id}")
    public Result<AnnouncementDTO> getAnnouncementById(@PathVariable Long id) {
        return Result.success(announcementAppService.getAnnouncementById(id));
    }

    @Operation(summary = "创建公告")
    @PostMapping
    @RequirePermission("sys:announcement:create")
    @OperationLog(module = "系统公告", action = "创建公告")
    public Result<AnnouncementDTO> createAnnouncement(@RequestBody CreateAnnouncementCommand command) {
        return Result.success(announcementAppService.createAnnouncement(command));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    @RequirePermission("sys:announcement:edit")
    @OperationLog(module = "系统公告", action = "更新公告")
    public Result<AnnouncementDTO> updateAnnouncement(@PathVariable Long id, @RequestBody CreateAnnouncementCommand command) {
        return Result.success(announcementAppService.updateAnnouncement(id, command));
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    @RequirePermission("sys:announcement:publish")
    @OperationLog(module = "系统公告", action = "发布公告")
    public Result<AnnouncementDTO> publishAnnouncement(@PathVariable Long id) {
        return Result.success(announcementAppService.publishAnnouncement(id));
    }

    @Operation(summary = "撤回公告")
    @PostMapping("/{id}/withdraw")
    @RequirePermission("sys:announcement:publish")
    @OperationLog(module = "系统公告", action = "撤回公告")
    public Result<Void> withdrawAnnouncement(@PathVariable Long id) {
        announcementAppService.withdrawAnnouncement(id);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @RequirePermission("sys:announcement:delete")
    @OperationLog(module = "系统公告", action = "删除公告")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementAppService.deleteAnnouncement(id);
        return Result.success();
    }
}
