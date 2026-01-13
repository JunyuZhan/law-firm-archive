package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.*;
import com.lawfirm.application.client.dto.LeadDTO;
import com.lawfirm.application.client.dto.LeadFollowUpDTO;
import com.lawfirm.application.client.dto.LeadQueryDTO;
import com.lawfirm.application.client.dto.LeadStatisticsDTO;
import com.lawfirm.application.client.service.LeadAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 案源管理控制器
 */
@RestController
@RequestMapping("/client/lead")
@RequiredArgsConstructor
@Tag(name = "案源管理", description = "案源线索登记、跟进和转化")
public class LeadController {

    private final LeadAppService leadAppService;

    @GetMapping
    @RequirePermission("lead:list")
    @Operation(summary = "查询案源列表")
    public Result<PageResult<LeadDTO>> listLeads(LeadQueryDTO query) {
        PageResult<LeadDTO> result = leadAppService.listLeads(query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequirePermission("lead:view")
    @Operation(summary = "获取案源详情")
    public Result<LeadDTO> getLead(@PathVariable Long id) {
        LeadDTO lead = leadAppService.getLead(id);
        return Result.success(lead);
    }

    @PostMapping
    @RequirePermission("lead:create")
    @Operation(summary = "创建案源")
    @OperationLog(module = "案源管理", action = "创建案源")
    public Result<LeadDTO> createLead(@RequestBody @Valid CreateLeadCommand command) {
        LeadDTO lead = leadAppService.createLead(command);
        return Result.success(lead);
    }

    @PutMapping("/{id}")
    @RequirePermission("lead:update")
    @Operation(summary = "更新案源")
    @OperationLog(module = "案源管理", action = "更新案源")
    public Result<LeadDTO> updateLead(@PathVariable Long id, @RequestBody @Valid UpdateLeadCommand command) {
        LeadDTO lead = leadAppService.updateLead(id, command);
        return Result.success(lead);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("lead:delete")
    @Operation(summary = "删除案源")
    @OperationLog(module = "案源管理", action = "删除案源")
    public Result<Void> deleteLead(@PathVariable Long id) {
        leadAppService.deleteLead(id);
        return Result.success();
    }

    @PostMapping("/{id}/follow-up")
    @RequirePermission("lead:follow")
    @Operation(summary = "创建跟进记录")
    @OperationLog(module = "案源管理", action = "创建跟进记录")
    public Result<LeadFollowUpDTO> createFollowUp(@PathVariable Long id, 
                                                    @RequestBody @Valid CreateFollowUpCommand command) {
        command.setLeadId(id);
        LeadFollowUpDTO followUp = leadAppService.createFollowUp(command);
        return Result.success(followUp);
    }

    @GetMapping("/{id}/follow-ups")
    @RequirePermission("lead:view")
    @Operation(summary = "查询案源跟进记录")
    public Result<List<LeadFollowUpDTO>> listFollowUps(@PathVariable Long id) {
        List<LeadFollowUpDTO> followUps = leadAppService.listFollowUps(id);
        return Result.success(followUps);
    }

    @PostMapping("/{id}/convert")
    @RequirePermission("lead:convert")
    @Operation(summary = "案源转化", description = "将案源转化为正式客户和项目")
    @OperationLog(module = "案源管理", action = "案源转化")
    public Result<LeadDTO> convertLead(@PathVariable Long id, @RequestBody @Valid ConvertLeadCommand command) {
        command.setLeadId(id);
        LeadDTO lead = leadAppService.convertLead(command);
        return Result.success(lead);
    }

    @PostMapping("/{id}/abandon")
    @RequirePermission("lead:update")
    @Operation(summary = "放弃案源")
    @OperationLog(module = "案源管理", action = "放弃案源")
    public Result<Void> abandonLead(@PathVariable Long id, @RequestParam(required = false) String reason) {
        leadAppService.abandonLead(id, reason);
        return Result.success();
    }

    /**
     * 获取案源统计（M2-033~M2-034）
     */
    @GetMapping("/statistics")
    @RequirePermission("lead:view")
    @Operation(summary = "获取案源统计", description = "统计案源来源渠道和转化率分析")
    public Result<LeadStatisticsDTO> getLeadStatistics() {
        return Result.success(leadAppService.getLeadStatistics());
    }
}

