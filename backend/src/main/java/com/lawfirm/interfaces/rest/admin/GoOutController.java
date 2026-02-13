package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.GoOutCommand;
import com.lawfirm.application.admin.dto.GoOutRecordDTO;
import com.lawfirm.application.admin.service.GoOutAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 外出登记接口（M8-005） */
@Tag(name = "外出管理", description = "外出登记和返回")
@RestController
@RequestMapping("/admin/go-out")
@RequiredArgsConstructor
public class GoOutController {

  /** 外出应用服务 */
  private final GoOutAppService goOutAppService;

  /**
   * 外出登记
   *
   * @param command 外出命令
   * @return 外出记录
   */
  @Operation(summary = "外出登记")
  @PostMapping("/register")
  @RequirePermission("admin:goout:register")
  @OperationLog(module = "外出管理", action = "外出登记")
  public Result<GoOutRecordDTO> registerGoOut(@RequestBody @Valid final GoOutCommand command) {
    return Result.success(goOutAppService.registerGoOut(command));
  }

  /**
   * 登记返回
   *
   * @param id 外出记录ID
   * @return 外出记录
   */
  @Operation(summary = "登记返回")
  @PostMapping("/{id}/return")
  @RequirePermission("admin:goout:register")
  @OperationLog(module = "外出管理", action = "登记返回")
  public Result<GoOutRecordDTO> registerReturn(@PathVariable final Long id) {
    return Result.success(goOutAppService.registerReturn(id));
  }

  /**
   * 查询我的外出记录
   *
   * @return 外出记录列表
   */
  @Operation(summary = "查询我的外出记录")
  @GetMapping("/my")
  @RequirePermission("admin:goout:list")
  public Result<List<GoOutRecordDTO>> getMyRecords() {
    return Result.success(goOutAppService.getMyRecords());
  }

  /**
   * 查询指定日期范围的外出记录
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 外出记录列表
   */
  @Operation(summary = "查询指定日期范围的外出记录")
  @GetMapping("/range")
  @RequirePermission("admin:goout:list")
  public Result<List<GoOutRecordDTO>> getRecordsByDateRange(
      @RequestParam final LocalDate startDate, @RequestParam final LocalDate endDate) {
    return Result.success(goOutAppService.getRecordsByDateRange(startDate, endDate));
  }

  /**
   * 查询当前外出的记录
   *
   * @return 外出记录列表
   */
  @Operation(summary = "查询当前外出的记录")
  @GetMapping("/current")
  @RequirePermission("admin:goout:list")
  public Result<List<GoOutRecordDTO>> getCurrentOut() {
    return Result.success(goOutAppService.getCurrentOut());
  }
}
