package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateSealApplicationCommand;
import com.lawfirm.application.document.dto.SealApplicationDTO;
import com.lawfirm.application.document.dto.SealApplicationQueryDTO;
import com.lawfirm.application.document.service.SealApplicationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用印申请接口
 */
@RestController
@RequestMapping("/document/seal-application")
@RequiredArgsConstructor
public class SealApplicationController {

    private final SealApplicationAppService applicationAppService;

    /**
     * 分页查询用印申请
     */
    @GetMapping
    @RequirePermission("seal:apply:list")
    public Result<PageResult<SealApplicationDTO>> list(SealApplicationQueryDTO query) {
        return Result.success(applicationAppService.listApplications(query));
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/{id}")
    @RequirePermission("seal:apply:detail")
    public Result<SealApplicationDTO> getById(@PathVariable Long id) {
        return Result.success(applicationAppService.getApplicationById(id));
    }

    /**
     * 创建用印申请
     */
    @PostMapping
    @RequirePermission("seal:apply")
    @OperationLog(module = "用印申请", action = "提交申请")
    public Result<SealApplicationDTO> create(@Valid @RequestBody CreateSealApplicationCommand command) {
        return Result.success(applicationAppService.createApplication(command));
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("seal:approve")
    @OperationLog(module = "用印申请", action = "审批通过")
    public Result<SealApplicationDTO> approve(@PathVariable Long id,
                                              @RequestParam(required = false) String comment) {
        return Result.success(applicationAppService.approve(id, comment));
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("seal:approve")
    @OperationLog(module = "用印申请", action = "审批拒绝")
    public Result<SealApplicationDTO> reject(@PathVariable Long id,
                                             @RequestParam(required = false) String comment) {
        return Result.success(applicationAppService.reject(id, comment));
    }

    /**
     * 登记用印
     */
    @PostMapping("/{id}/use")
    @RequirePermission("seal:use")
    @OperationLog(module = "用印申请", action = "登记用印")
    public Result<SealApplicationDTO> registerUsage(@PathVariable Long id,
                                                    @RequestParam(required = false) String remark) {
        return Result.success(applicationAppService.registerUsage(id, remark));
    }

    /**
     * 取消申请
     */
    @PostMapping("/{id}/cancel")
    @RequirePermission("seal:apply")
    @OperationLog(module = "用印申请", action = "取消申请")
    public Result<Void> cancel(@PathVariable Long id) {
        applicationAppService.cancelApplication(id);
        return Result.success();
    }

    /**
     * 获取待审批列表
     */
    @GetMapping("/pending")
    @RequirePermission("seal:approve")
    public Result<List<SealApplicationDTO>> getPending() {
        return Result.success(applicationAppService.getPendingApplications());
    }
}
