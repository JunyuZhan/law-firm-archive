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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 外出登记接口（M8-005）
 */
@Tag(name = "外出管理", description = "外出登记和返回")
@RestController
@RequestMapping("/admin/go-out")
@RequiredArgsConstructor
public class GoOutController {

    private final GoOutAppService goOutAppService;

    @Operation(summary = "外出登记")
    @PostMapping("/register")
    @RequirePermission("admin:goout:register")
    @OperationLog(module = "外出管理", action = "外出登记")
    public Result<GoOutRecordDTO> registerGoOut(@RequestBody @Valid GoOutCommand command) {
        return Result.success(goOutAppService.registerGoOut(command));
    }

    @Operation(summary = "登记返回")
    @PostMapping("/{id}/return")
    @RequirePermission("admin:goout:register")
    @OperationLog(module = "外出管理", action = "登记返回")
    public Result<GoOutRecordDTO> registerReturn(@PathVariable Long id) {
        return Result.success(goOutAppService.registerReturn(id));
    }

    @Operation(summary = "查询我的外出记录")
    @GetMapping("/my")
    @RequirePermission("admin:goout:list")
    public Result<List<GoOutRecordDTO>> getMyRecords() {
        return Result.success(goOutAppService.getMyRecords());
    }

    @Operation(summary = "查询指定日期范围的外出记录")
    @GetMapping("/range")
    @RequirePermission("admin:goout:list")
    public Result<List<GoOutRecordDTO>> getRecordsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Result.success(goOutAppService.getRecordsByDateRange(startDate, endDate));
    }

    @Operation(summary = "查询当前外出的记录")
    @GetMapping("/current")
    @RequirePermission("admin:goout:list")
    public Result<List<GoOutRecordDTO>> getCurrentOut() {
        return Result.success(goOutAppService.getCurrentOut());
    }
}

