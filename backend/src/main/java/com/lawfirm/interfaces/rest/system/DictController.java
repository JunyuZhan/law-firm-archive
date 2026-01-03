package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateDictItemCommand;
import com.lawfirm.application.system.command.CreateDictTypeCommand;
import com.lawfirm.application.system.dto.DictItemDTO;
import com.lawfirm.application.system.dto.DictTypeDTO;
import com.lawfirm.application.system.service.DictAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典接口
 */
@Tag(name = "数据字典", description = "数据字典管理相关接口")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictAppService dictAppService;

    // ==================== 字典类型 ====================

    @Operation(summary = "获取所有字典类型")
    @GetMapping("/types")
    public Result<List<DictTypeDTO>> listDictTypes() {
        return Result.success(dictAppService.listDictTypes());
    }

    @Operation(summary = "获取字典类型详情（含字典项）")
    @GetMapping("/types/{id}")
    public Result<DictTypeDTO> getDictTypeWithItems(@PathVariable Long id) {
        return Result.success(dictAppService.getDictTypeWithItems(id));
    }

    @Operation(summary = "根据编码获取字典项")
    @GetMapping("/items/code/{code}")
    public Result<List<DictItemDTO>> getDictItemsByCode(@PathVariable String code) {
        return Result.success(dictAppService.getDictItemsByCode(code));
    }

    @Operation(summary = "创建字典类型")
    @PostMapping("/types")
    @RequirePermission("sys:dict:create")
    @OperationLog(module = "数据字典", action = "创建字典类型")
    public Result<DictTypeDTO> createDictType(@RequestBody CreateDictTypeCommand command) {
        return Result.success(dictAppService.createDictType(command));
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/types/{id}")
    @RequirePermission("sys:dict:edit")
    @OperationLog(module = "数据字典", action = "更新字典类型")
    public Result<DictTypeDTO> updateDictType(@PathVariable Long id, @RequestBody CreateDictTypeCommand command) {
        return Result.success(dictAppService.updateDictType(id, command));
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/types/{id}")
    @RequirePermission("sys:dict:delete")
    @OperationLog(module = "数据字典", action = "删除字典类型")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        dictAppService.deleteDictType(id);
        return Result.success();
    }

    // ==================== 字典项 ====================

    @Operation(summary = "获取字典项列表")
    @GetMapping("/types/{typeId}/items")
    public Result<List<DictItemDTO>> getDictItemsByTypeId(@PathVariable Long typeId) {
        return Result.success(dictAppService.getDictItemsByTypeId(typeId));
    }

    @Operation(summary = "创建字典项")
    @PostMapping("/items")
    @RequirePermission("sys:dict:create")
    @OperationLog(module = "数据字典", action = "创建字典项")
    public Result<DictItemDTO> createDictItem(@RequestBody CreateDictItemCommand command) {
        return Result.success(dictAppService.createDictItem(command));
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/items/{id}")
    @RequirePermission("sys:dict:edit")
    @OperationLog(module = "数据字典", action = "更新字典项")
    public Result<DictItemDTO> updateDictItem(@PathVariable Long id, @RequestBody CreateDictItemCommand command) {
        return Result.success(dictAppService.updateDictItem(id, command));
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/items/{id}")
    @RequirePermission("sys:dict:delete")
    @OperationLog(module = "数据字典", action = "删除字典项")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        dictAppService.deleteDictItem(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用字典项")
    @PostMapping("/items/{id}/toggle")
    @RequirePermission("sys:dict:edit")
    public Result<Void> toggleDictItemStatus(@PathVariable Long id) {
        dictAppService.toggleDictItemStatus(id);
        return Result.success();
    }
}
