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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 数据字典接口 */
@Tag(name = "数据字典", description = "数据字典管理相关接口")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class DictController {

  /** 字典应用服务 */
  private final DictAppService dictAppService;

  // ==================== 字典类型 ====================

  /**
   * 获取所有字典类型
   *
   * @return 字典类型列表
   */
  @Operation(summary = "获取所有字典类型")
  @GetMapping("/types")
  public Result<List<DictTypeDTO>> listDictTypes() {
    return Result.success(dictAppService.listDictTypes());
  }

  /**
   * 获取字典类型详情（含字典项）
   *
   * @param id 字典类型ID
   * @return 字典类型详情
   */
  @Operation(summary = "获取字典类型详情（含字典项）")
  @GetMapping("/types/{id}")
  public Result<DictTypeDTO> getDictTypeWithItems(@PathVariable final Long id) {
    return Result.success(dictAppService.getDictTypeWithItems(id));
  }

  /**
   * 根据编码获取字典项
   *
   * @param code 字典类型编码
   * @return 字典项列表
   */
  @Operation(summary = "根据编码获取字典项")
  @GetMapping("/items/code/{code}")
  public Result<List<DictItemDTO>> getDictItemsByCode(@PathVariable final String code) {
    return Result.success(dictAppService.getDictItemsByCode(code));
  }

  /**
   * 创建字典类型
   *
   * @param command 创建字典类型命令
   * @return 字典类型信息
   */
  @Operation(summary = "创建字典类型")
  @PostMapping("/types")
  @RequirePermission("sys:dict:create")
  @OperationLog(module = "数据字典", action = "创建字典类型")
  public Result<DictTypeDTO> createDictType(@RequestBody final CreateDictTypeCommand command) {
    return Result.success(dictAppService.createDictType(command));
  }

  /**
   * 更新字典类型
   *
   * @param id 字典类型ID
   * @param command 更新字典类型命令
   * @return 字典类型信息
   */
  @Operation(summary = "更新字典类型")
  @PutMapping("/types/{id}")
  @RequirePermission("sys:dict:update")
  @OperationLog(module = "数据字典", action = "更新字典类型")
  public Result<DictTypeDTO> updateDictType(
      @PathVariable final Long id, @RequestBody final CreateDictTypeCommand command) {
    return Result.success(dictAppService.updateDictType(id, command));
  }

  /**
   * 删除字典类型
   *
   * @param id 字典类型ID
   * @return 空结果
   */
  @Operation(summary = "删除字典类型")
  @DeleteMapping("/types/{id}")
  @RequirePermission("sys:dict:delete")
  @OperationLog(module = "数据字典", action = "删除字典类型")
  public Result<Void> deleteDictType(@PathVariable final Long id) {
    dictAppService.deleteDictType(id);
    return Result.success();
  }

  // ==================== 字典项 ====================

  /**
   * 获取字典项列表
   *
   * @param typeId 字典类型ID
   * @return 字典项列表
   */
  @Operation(summary = "获取字典项列表")
  @GetMapping("/types/{typeId}/items")
  public Result<List<DictItemDTO>> getDictItemsByTypeId(@PathVariable final Long typeId) {
    return Result.success(dictAppService.getDictItemsByTypeId(typeId));
  }

  /**
   * 创建字典项
   *
   * @param command 创建字典项命令
   * @return 字典项信息
   */
  @Operation(summary = "创建字典项")
  @PostMapping("/items")
  @RequirePermission("sys:dict:create")
  @OperationLog(module = "数据字典", action = "创建字典项")
  public Result<DictItemDTO> createDictItem(@RequestBody final CreateDictItemCommand command) {
    return Result.success(dictAppService.createDictItem(command));
  }

  /**
   * 更新字典项
   *
   * @param id 字典项ID
   * @param command 更新字典项命令
   * @return 字典项信息
   */
  @Operation(summary = "更新字典项")
  @PutMapping("/items/{id}")
  @RequirePermission("sys:dict:update")
  @OperationLog(module = "数据字典", action = "更新字典项")
  public Result<DictItemDTO> updateDictItem(
      @PathVariable final Long id, @RequestBody final CreateDictItemCommand command) {
    return Result.success(dictAppService.updateDictItem(id, command));
  }

  /**
   * 删除字典项
   *
   * @param id 字典项ID
   * @return 空结果
   */
  @Operation(summary = "删除字典项")
  @DeleteMapping("/items/{id}")
  @RequirePermission("sys:dict:delete")
  @OperationLog(module = "数据字典", action = "删除字典项")
  public Result<Void> deleteDictItem(@PathVariable final Long id) {
    dictAppService.deleteDictItem(id);
    return Result.success();
  }

  /**
   * 启用/禁用字典项
   *
   * @param id 字典项ID
   * @return 空结果
   */
  @Operation(summary = "启用/禁用字典项")
  @PostMapping("/items/{id}/toggle")
  @RequirePermission("sys:dict:update")
  public Result<Void> toggleDictItemStatus(@PathVariable final Long id) {
    dictAppService.toggleDictItemStatus(id);
    return Result.success();
  }
}
