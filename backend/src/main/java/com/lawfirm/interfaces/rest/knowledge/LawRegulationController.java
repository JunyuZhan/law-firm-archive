package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateLawRegulationCommand;
import com.lawfirm.application.knowledge.dto.LawCategoryDTO;
import com.lawfirm.application.knowledge.dto.LawRegulationDTO;
import com.lawfirm.application.knowledge.dto.LawRegulationQueryDTO;
import com.lawfirm.application.knowledge.service.LawRegulationAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
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

/** 法规库接口 */
@Tag(name = "法规库", description = "法规查询、收藏相关接口")
@RestController
@RequestMapping("/knowledge/law")
@RequiredArgsConstructor
public class LawRegulationController {

  /** 法规库应用服务. */
  private final LawRegulationAppService lawRegulationAppService;

  /**
   * 获取法规分类树
   *
   * @return 分类树
   */
  @Operation(summary = "获取法规分类树")
  @GetMapping("/categories")
  public Result<List<LawCategoryDTO>> getCategoryTree() {
    return Result.success(lawRegulationAppService.getCategoryTree());
  }

  /**
   * 分页查询法规
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询法规")
  @GetMapping
  public Result<PageResult<LawRegulationDTO>> listRegulations(final LawRegulationQueryDTO query) {
    return Result.success(lawRegulationAppService.listRegulations(query));
  }

  /**
   * 获取法规详情
   *
   * @param id 法规ID
   * @return 法规详情
   */
  @Operation(summary = "获取法规详情")
  @GetMapping("/{id}")
  public Result<LawRegulationDTO> getRegulationById(@PathVariable final Long id) {
    return Result.success(lawRegulationAppService.getRegulationById(id));
  }

  /**
   * 创建法规
   *
   * @param command 创建命令
   * @return 创建的法规
   */
  @Operation(summary = "创建法规")
  @PostMapping
  @RequirePermission("knowledge:law:create")
  @OperationLog(module = "法规库", action = "创建法规")
  public Result<LawRegulationDTO> createRegulation(
      @RequestBody final CreateLawRegulationCommand command) {
    return Result.success(lawRegulationAppService.createRegulation(command));
  }

  /**
   * 更新法规
   *
   * @param id 法规ID
   * @param command 更新命令
   * @return 更新后的法规
   */
  @Operation(summary = "更新法规")
  @PutMapping("/{id}")
  @RequirePermission("knowledge:law:update")
  @OperationLog(module = "法规库", action = "更新法规")
  public Result<LawRegulationDTO> updateRegulation(
      @PathVariable final Long id, @RequestBody final CreateLawRegulationCommand command) {
    return Result.success(lawRegulationAppService.updateRegulation(id, command));
  }

  /**
   * 删除法规
   *
   * @param id 法规ID
   * @return 操作结果
   */
  @Operation(summary = "删除法规")
  @DeleteMapping("/{id}")
  @RequirePermission("knowledge:law:delete")
  @OperationLog(module = "法规库", action = "删除法规")
  public Result<Void> deleteRegulation(@PathVariable final Long id) {
    lawRegulationAppService.deleteRegulation(id);
    return Result.success();
  }

  /**
   * 收藏法规
   *
   * @param id 法规ID
   * @return 操作结果
   */
  @Operation(summary = "收藏法规")
  @PostMapping("/{id}/collect")
  public Result<Void> collectRegulation(@PathVariable final Long id) {
    lawRegulationAppService.collectRegulation(id);
    return Result.success();
  }

  /**
   * 取消收藏法规
   *
   * @param id 法规ID
   * @return 操作结果
   */
  @Operation(summary = "取消收藏法规")
  @DeleteMapping("/{id}/collect")
  public Result<Void> uncollectRegulation(@PathVariable final Long id) {
    lawRegulationAppService.uncollectRegulation(id);
    return Result.success();
  }

  /**
   * 获取我的收藏法规
   *
   * @return 收藏列表
   */
  @Operation(summary = "获取我的收藏法规")
  @GetMapping("/collected")
  public Result<List<LawRegulationDTO>> getMyCollectedRegulations() {
    return Result.success(lawRegulationAppService.getMyCollectedRegulations());
  }

  /**
   * 标注法规失效
   *
   * @param id 法规ID
   * @param reason 失效原因
   * @return 操作结果
   */
  @Operation(summary = "标注法规失效")
  @PostMapping("/{id}/mark-repealed")
  @RequirePermission("knowledge:law:update")
  @OperationLog(module = "法规库", action = "标注法规失效")
  public Result<LawRegulationDTO> markAsRepealed(
      @PathVariable final Long id, @RequestParam(required = false) final String reason) {
    return Result.success(lawRegulationAppService.markAsRepealed(id, reason));
  }
}
