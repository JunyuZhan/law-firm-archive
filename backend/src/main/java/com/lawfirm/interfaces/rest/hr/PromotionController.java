package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateCareerLevelCommand;
import com.lawfirm.application.hr.dto.CareerLevelDTO;
import com.lawfirm.application.hr.dto.CareerLevelQueryDTO;
import com.lawfirm.application.hr.dto.PromotionApplicationDTO;
import com.lawfirm.application.hr.dto.PromotionQueryDTO;
import com.lawfirm.application.hr.service.PromotionAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 晋升管理控制器 */
@Tag(name = "晋升管理", description = "职级通道和晋升申请管理")
@RestController
@RequestMapping("/hr/promotion")
@RequiredArgsConstructor
public class PromotionController {

  /** 晋升服务. */
  private final PromotionAppService promotionAppService;

  // ========== 职级管理 ==========

  /**
   * 分页查询职级列表
   *
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param keyword 关键词
   * @param category 类别
   * @param status 状态
   * @return 分页结果
   */
  @Operation(summary = "分页查询职级列表")
  @GetMapping("/levels")
  @RequirePermission("hr:promotion:list")
  public Result<PageResult<CareerLevelDTO>> listCareerLevels(
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "类别") @RequestParam(required = false) final String category,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    CareerLevelQueryDTO query = new CareerLevelQueryDTO();
    query.setPageNum(pageNum);
    query.setPageSize(pageSize);
    query.setKeyword(keyword);
    query.setCategory(category);
    query.setStatus(status);
    return Result.success(promotionAppService.listCareerLevels(query));
  }

  /**
   * 获取职级详情
   *
   * @param id 职级ID
   * @return 职级详情
   */
  @Operation(summary = "获取职级详情")
  @GetMapping("/levels/{id}")
  @RequirePermission("hr:promotion:view")
  public Result<CareerLevelDTO> getCareerLevel(@PathVariable final Long id) {
    return Result.success(promotionAppService.getCareerLevelById(id));
  }

  /**
   * 按类别获取职级列表
   *
   * @param category 类别
   * @return 职级列表
   */
  @Operation(summary = "按类别获取职级列表")
  @GetMapping("/levels/category/{category}")
  @RequirePermission("hr:promotion:list")
  public Result<List<CareerLevelDTO>> getCareerLevelsByCategory(
      @PathVariable final String category) {
    return Result.success(promotionAppService.getCareerLevelsByCategory(category));
  }

  /**
   * 创建职级
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建职级")
  @PostMapping("/levels")
  @RequirePermission("hr:promotion:create")
  @OperationLog(module = "晋升管理", action = "创建职级")
  public Result<CareerLevelDTO> createCareerLevel(
      @RequestBody @Valid final CreateCareerLevelCommand command) {
    return Result.success(promotionAppService.createCareerLevel(command));
  }

  /**
   * 更新职级
   *
   * @param id 职级ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新职级")
  @PutMapping("/levels/{id}")
  @RequirePermission("hr:promotion:update")
  @OperationLog(module = "晋升管理", action = "更新职级")
  public Result<CareerLevelDTO> updateCareerLevel(
      @PathVariable final Long id, @RequestBody @Valid final CreateCareerLevelCommand command) {
    return Result.success(promotionAppService.updateCareerLevel(id, command));
  }

  /**
   * 删除职级
   *
   * @param id 职级ID
   * @return 无返回
   */
  @Operation(summary = "删除职级")
  @DeleteMapping("/levels/{id}")
  @RequirePermission("hr:promotion:delete")
  @OperationLog(module = "晋升管理", action = "删除职级")
  public Result<Void> deleteCareerLevel(@PathVariable final Long id) {
    promotionAppService.deleteCareerLevel(id);
    return Result.success();
  }

  /**
   * 启用职级
   *
   * @param id 职级ID
   * @return 无返回
   */
  @Operation(summary = "启用职级")
  @PostMapping("/levels/{id}/enable")
  @RequirePermission("hr:promotion:update")
  @OperationLog(module = "晋升管理", action = "启用职级")
  public Result<Void> enableCareerLevel(@PathVariable final Long id) {
    promotionAppService.enableCareerLevel(id);
    return Result.success();
  }

  /**
   * 停用职级
   *
   * @param id 职级ID
   * @return 无返回
   */
  @Operation(summary = "停用职级")
  @PostMapping("/levels/{id}/disable")
  @RequirePermission("hr:promotion:update")
  @OperationLog(module = "晋升管理", action = "停用职级")
  public Result<Void> disableCareerLevel(@PathVariable final Long id) {
    promotionAppService.disableCareerLevel(id);
    return Result.success();
  }

  // ========== 晋升申请 ==========

  /**
   * 分页查询晋升申请
   *
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param keyword 关键词
   * @param status 状态
   * @param employeeId 员工ID
   * @param departmentId 部门ID
   * @return 分页结果
   */
  @Operation(summary = "分页查询晋升申请")
  @GetMapping("/applications")
  @RequirePermission("hr:promotion:list")
  public Result<PageResult<PromotionApplicationDTO>> listApplications(
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "状态") @RequestParam(required = false) final String status,
      @Parameter(description = "员工ID") @RequestParam(required = false) final Long employeeId,
      @Parameter(description = "部门ID") @RequestParam(required = false) final Long departmentId) {
    PromotionQueryDTO query = new PromotionQueryDTO();
    query.setPageNum(pageNum);
    query.setPageSize(pageSize);
    query.setKeyword(keyword);
    query.setStatus(status);
    query.setEmployeeId(employeeId);
    query.setDepartmentId(departmentId);
    return Result.success(promotionAppService.listPromotionApplications(query));
  }

  /**
   * 统计待审批数量
   *
   * @return 待审批数量
   */
  @Operation(summary = "统计待审批数量")
  @GetMapping("/applications/pending-count")
  public Result<Long> countPendingApplications() {
    return Result.success(promotionAppService.countPendingApplications());
  }
}
