package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateStateCompensationCommand;
import com.lawfirm.application.matter.command.UpdateStateCompensationCommand;
import com.lawfirm.application.matter.dto.StateCompensationDTO;
import com.lawfirm.application.matter.service.StateCompensationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 国家赔偿案件信息 Controller
 */
@RestController
@RequestMapping("/matter/{matterId}/state-compensation")
@RequiredArgsConstructor
@Tag(name = "国家赔偿管理", description = "国家赔偿案件业务信息管理接口")
public class StateCompensationController {

    private final StateCompensationAppService stateCompensationAppService;

    /**
     * 获取案件的国家赔偿信息
     */
    @GetMapping
    @RequirePermission("matter:view")
    @Operation(summary = "获取国家赔偿信息", description = "根据案件ID获取国家赔偿业务信息")
    public Result<StateCompensationDTO> getByMatterId(@PathVariable Long matterId) {
        StateCompensationDTO dto = stateCompensationAppService.getByMatterId(matterId);
        return Result.success(dto);
    }

    /**
     * 创建国家赔偿信息
     */
    @PostMapping
    @RequirePermission("matter:update")
    @OperationLog(module = "国家赔偿管理", action = "创建国家赔偿信息")
    @Operation(summary = "创建国家赔偿信息", description = "为案件创建国家赔偿业务信息")
    public Result<StateCompensationDTO> create(
            @PathVariable Long matterId,
            @RequestBody @Valid CreateStateCompensationCommand command) {
        command.setMatterId(matterId);
        StateCompensationDTO dto = stateCompensationAppService.create(command);
        return Result.success(dto);
    }

    /**
     * 更新国家赔偿信息
     */
    @PutMapping("/{id}")
    @RequirePermission("matter:update")
    @OperationLog(module = "国家赔偿管理", action = "更新国家赔偿信息")
    @Operation(summary = "更新国家赔偿信息", description = "更新国家赔偿业务信息")
    public Result<StateCompensationDTO> update(
            @PathVariable Long matterId,
            @PathVariable Long id,
            @RequestBody @Valid UpdateStateCompensationCommand command) {
        command.setId(id);
        StateCompensationDTO dto = stateCompensationAppService.update(command);
        return Result.success(dto);
    }

    /**
     * 删除国家赔偿信息
     */
    @DeleteMapping("/{id}")
    @RequirePermission("matter:update")
    @OperationLog(module = "国家赔偿管理", action = "删除国家赔偿信息")
    @Operation(summary = "删除国家赔偿信息", description = "删除国家赔偿业务信息")
    public Result<Void> delete(
            @PathVariable Long matterId,
            @PathVariable Long id) {
        stateCompensationAppService.delete(id);
        return Result.success();
    }

    /**
     * 根据案件ID删除国家赔偿信息
     */
    @DeleteMapping
    @RequirePermission("matter:update")
    @OperationLog(module = "国家赔偿管理", action = "删除国家赔偿信息")
    @Operation(summary = "根据案件ID删除国家赔偿信息", description = "删除指定案件的国家赔偿业务信息")
    public Result<Void> deleteByMatterId(@PathVariable Long matterId) {
        stateCompensationAppService.deleteByMatterId(matterId);
        return Result.success();
    }
}
