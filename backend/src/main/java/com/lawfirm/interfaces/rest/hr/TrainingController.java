package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateTrainingCommand;
import com.lawfirm.application.hr.dto.TrainingDTO;
import com.lawfirm.application.hr.dto.TrainingRecordDTO;
import com.lawfirm.application.hr.service.TrainingAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 培训管理 Controller
 */
@Tag(name = "培训管理", description = "培训计划和培训记录管理相关接口")
@RestController
@RequestMapping("/hr/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingAppService trainingAppService;

    /**
     * 分页查询培训列表
     */
    @Operation(summary = "分页查询培训列表")
    @GetMapping("/list")
    @RequirePermission("hr:training:list")
    public Result<PageResult<TrainingDTO>> listTrainings(PageQuery query,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status) {
        PageResult<TrainingDTO> result = trainingAppService.listTrainings(query, keyword, status);
        return Result.success(result);
    }

    /**
     * 获取可报名的培训列表
     */
    @Operation(summary = "获取可报名的培训列表")
    @GetMapping("/available")
    public Result<List<TrainingDTO>> getAvailableTrainings() {
        List<TrainingDTO> trainings = trainingAppService.getAvailableTrainings();
        return Result.success(trainings);
    }

    /**
     * 获取培训详情
     */
    @Operation(summary = "获取培训详情")
    @GetMapping("/{id}")
    public Result<TrainingDTO> getTraining(@PathVariable Long id) {
        TrainingDTO training = trainingAppService.getTrainingById(id);
        return Result.success(training);
    }

    /**
     * 创建培训计划
     */
    @Operation(summary = "创建培训计划")
    @PostMapping
    @RequirePermission("hr:training:create")
    @OperationLog(module = "培训管理", action = "创建培训计划")
    public Result<TrainingDTO> createTraining(@RequestBody @Valid CreateTrainingCommand command) {
        TrainingDTO training = trainingAppService.createTraining(command);
        return Result.success(training);
    }

    /**
     * 发布培训
     */
    @Operation(summary = "发布培训")
    @PostMapping("/{id}/publish")
    @RequirePermission("hr:training:edit")
    @OperationLog(module = "培训管理", action = "发布培训")
    public Result<Void> publishTraining(@PathVariable Long id) {
        trainingAppService.publishTraining(id);
        return Result.success();
    }

    /**
     * 取消培训
     */
    @Operation(summary = "取消培训")
    @PostMapping("/{id}/cancel")
    @RequirePermission("hr:training:edit")
    @OperationLog(module = "培训管理", action = "取消培训")
    public Result<Void> cancelTraining(@PathVariable Long id) {
        trainingAppService.cancelTraining(id);
        return Result.success();
    }

    /**
     * 报名培训
     */
    @Operation(summary = "报名培训")
    @PostMapping("/{id}/enroll")
    @OperationLog(module = "培训管理", action = "报名培训")
    public Result<Void> enrollTraining(@PathVariable Long id) {
        trainingAppService.enrollTraining(id);
        return Result.success();
    }

    /**
     * 取消报名
     */
    @Operation(summary = "取消报名")
    @PostMapping("/{id}/cancel-enrollment")
    @OperationLog(module = "培训管理", action = "取消报名")
    public Result<Void> cancelEnrollment(@PathVariable Long id) {
        trainingAppService.cancelEnrollment(id);
        return Result.success();
    }

    /**
     * 获取我的培训记录
     */
    @Operation(summary = "获取我的培训记录")
    @GetMapping("/my-records")
    public Result<List<TrainingRecordDTO>> getMyTrainingRecords() {
        List<TrainingRecordDTO> records = trainingAppService.getMyTrainingRecords();
        return Result.success(records);
    }

    /**
     * 获取我的学分统计
     */
    @Operation(summary = "获取我的学分统计")
    @GetMapping("/my-credits")
    public Result<Integer> getMyTotalCredits() {
        int credits = trainingAppService.getMyTotalCredits();
        return Result.success(credits);
    }

    /**
     * 获取培训参与者列表
     */
    @Operation(summary = "获取培训参与者列表")
    @GetMapping("/{id}/participants")
    @RequirePermission("hr:training:list")
    public Result<List<TrainingRecordDTO>> getTrainingParticipants(@PathVariable Long id) {
        List<TrainingRecordDTO> participants = trainingAppService.getTrainingParticipants(id);
        return Result.success(participants);
    }
}
