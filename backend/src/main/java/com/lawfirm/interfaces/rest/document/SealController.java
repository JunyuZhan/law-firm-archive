package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.CreateSealCommand;
import com.lawfirm.application.document.dto.SealDTO;
import com.lawfirm.application.document.dto.SealQueryDTO;
import com.lawfirm.application.document.service.SealAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 印章管理接口
 */
@RestController
@RequestMapping("/api/seals")
@RequiredArgsConstructor
public class SealController {

    private final SealAppService sealAppService;

    /**
     * 分页查询印章
     */
    @GetMapping
    @RequirePermission("seal:list")
    public Result<PageResult<SealDTO>> list(SealQueryDTO query) {
        return Result.success(sealAppService.listSeals(query));
    }

    /**
     * 获取印章详情
     */
    @GetMapping("/{id}")
    @RequirePermission("seal:detail")
    public Result<SealDTO> getById(@PathVariable Long id) {
        return Result.success(sealAppService.getSealById(id));
    }

    /**
     * 创建印章
     */
    @PostMapping
    @RequirePermission("seal:manage")
    @OperationLog(module = "印章管理", action = "创建印章")
    public Result<SealDTO> create(@Valid @RequestBody CreateSealCommand command) {
        return Result.success(sealAppService.createSeal(command));
    }

    /**
     * 更新印章
     */
    @PutMapping("/{id}")
    @RequirePermission("seal:manage")
    @OperationLog(module = "印章管理", action = "更新印章")
    public Result<SealDTO> update(@PathVariable Long id,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Long keeperId,
                                  @RequestParam(required = false) String keeperName,
                                  @RequestParam(required = false) String imageUrl,
                                  @RequestParam(required = false) String description) {
        return Result.success(sealAppService.updateSeal(id, name, keeperId, keeperName, imageUrl, description));
    }

    /**
     * 变更印章状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("seal:manage")
    @OperationLog(module = "印章管理", action = "变更印章状态")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String status) {
        sealAppService.changeSealStatus(id, status);
        return Result.success();
    }

    /**
     * 删除印章
     */
    @DeleteMapping("/{id}")
    @RequirePermission("seal:manage")
    @OperationLog(module = "印章管理", action = "删除印章")
    public Result<Void> delete(@PathVariable Long id) {
        sealAppService.deleteSeal(id);
        return Result.success();
    }
}
