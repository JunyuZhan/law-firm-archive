package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreateLetterApplicationCommand;
import com.lawfirm.application.admin.dto.LetterApplicationDTO;
import com.lawfirm.application.admin.dto.LetterTemplateDTO;
import com.lawfirm.application.admin.service.LetterAppService;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 出函管理接口
 */
@Tag(name = "出函管理")
@RestController
@RequestMapping("/admin/letter")
@RequiredArgsConstructor
public class LetterController {

    private final LetterAppService letterAppService;

    // ==================== 模板管理 ====================

    @Operation(summary = "获取启用的模板列表")
    @GetMapping("/template/list")
    public Result<List<LetterTemplateDTO>> listTemplates() {
        return Result.success(letterAppService.listActiveTemplates());
    }

    @Operation(summary = "创建模板")
    @PostMapping("/template")
    public Result<LetterTemplateDTO> createTemplate(
            @RequestParam String name,
            @RequestParam String letterType,
            @RequestParam String content,
            @RequestParam(required = false) String description) {
        return Result.success(letterAppService.createTemplate(name, letterType, content, description));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/template/{id}")
    public Result<LetterTemplateDTO> updateTemplate(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String letterType,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String description) {
        return Result.success(letterAppService.updateTemplate(id, name, letterType, content, description));
    }

    @Operation(summary = "启用/停用模板")
    @PostMapping("/template/{id}/toggle")
    public Result<Void> toggleTemplateStatus(@PathVariable Long id) {
        letterAppService.toggleTemplateStatus(id);
        return Result.success();
    }

    @Operation(summary = "获取所有模板（管理员）")
    @GetMapping("/template/all")
    public Result<List<LetterTemplateDTO>> listAllTemplates() {
        return Result.success(letterAppService.listAllTemplates());
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/template/{id}")
    public Result<LetterTemplateDTO> getTemplate(@PathVariable Long id) {
        return Result.success(letterAppService.getTemplateById(id));
    }

    // ==================== 出函申请 ====================

    @Operation(summary = "创建出函申请")
    @PostMapping("/application")
    public Result<LetterApplicationDTO> createApplication(@RequestBody CreateLetterApplicationCommand command) {
        return Result.success(letterAppService.createApplication(command));
    }

    @Operation(summary = "获取申请详情")
    @GetMapping("/application/{id}")
    public Result<LetterApplicationDTO> getApplication(@PathVariable Long id) {
        return Result.success(letterAppService.getById(id));
    }

    @Operation(summary = "查询项目的出函记录")
    @GetMapping("/application/matter/{matterId}")
    public Result<List<LetterApplicationDTO>> listByMatter(@PathVariable Long matterId) {
        return Result.success(letterAppService.listByMatter(matterId));
    }

    @Operation(summary = "我的申请列表")
    @GetMapping("/application/my")
    public Result<List<LetterApplicationDTO>> listMyApplications() {
        return Result.success(letterAppService.listMyApplications());
    }

    @Operation(summary = "取消申请")
    @PostMapping("/application/{id}/cancel")
    public Result<Void> cancelApplication(@PathVariable Long id) {
        letterAppService.cancelApplication(id);
        return Result.success();
    }

    @Operation(summary = "待审批列表")
    @GetMapping("/application/pending-approval")
    public Result<List<LetterApplicationDTO>> listPendingApproval() {
        return Result.success(letterAppService.listPendingApproval());
    }

    @Operation(summary = "审批通过")
    @PostMapping("/application/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam(required = false) String comment) {
        letterAppService.approve(id, comment);
        return Result.success();
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/application/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam String comment) {
        letterAppService.reject(id, comment);
        return Result.success();
    }

    @Operation(summary = "退回修改")
    @PostMapping("/application/{id}/return")
    public Result<Void> returnForRevision(@PathVariable Long id, @RequestParam String comment) {
        letterAppService.returnForRevision(id, comment);
        return Result.success();
    }

    @Operation(summary = "更新申请（被退回后修改）")
    @PutMapping("/application/{id}")
    public Result<LetterApplicationDTO> updateApplication(
            @PathVariable Long id, 
            @RequestBody CreateLetterApplicationCommand command) {
        return Result.success(letterAppService.updateApplication(id, command));
    }

    @Operation(summary = "重新提交申请")
    @PostMapping("/application/{id}/resubmit")
    public Result<LetterApplicationDTO> resubmit(
            @PathVariable Long id, 
            @RequestBody CreateLetterApplicationCommand command) {
        return Result.success(letterAppService.resubmit(id, command));
    }

    @Operation(summary = "重新提交审批（仅改变状态）")
    @PostMapping("/application/{id}/submit")
    public Result<Void> submitForApproval(@PathVariable Long id) {
        letterAppService.submitForApproval(id);
        return Result.success();
    }

    // ==================== 行政操作 ====================

    @Operation(summary = "获取全部申请列表（行政管理）")
    @GetMapping("/application/all")
    public Result<List<LetterApplicationDTO>> listAllApplications(
            @RequestParam(required = false) String applicationNo,
            @RequestParam(required = false) String matterName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(letterAppService.listAllApplications(applicationNo, matterName, status, startDate, endDate));
    }

    @Operation(summary = "获取待打印列表")
    @GetMapping("/application/pending-print")
    public Result<List<LetterApplicationDTO>> listPendingPrint() {
        return Result.success(letterAppService.listPendingPrint());
    }

    @Operation(summary = "确认打印")
    @PostMapping("/application/{id}/print")
    public Result<Void> confirmPrint(@PathVariable Long id) {
        letterAppService.confirmPrint(id);
        return Result.success();
    }

    @Operation(summary = "确认领取")
    @PostMapping("/application/{id}/receive")
    public Result<Void> confirmReceive(@PathVariable Long id) {
        letterAppService.confirmReceive(id);
        return Result.success();
    }
}
