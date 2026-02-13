package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.ConvertLeadCommand;
import com.lawfirm.application.client.command.CreateFollowUpCommand;
import com.lawfirm.application.client.command.CreateLeadCommand;
import com.lawfirm.application.client.command.UpdateLeadCommand;
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

/** 案源管理控制器 */
@RestController
@RequestMapping("/client/lead")
@RequiredArgsConstructor
@Tag(name = "案源管理", description = "案源线索登记、跟进和转化")
public class LeadController {

  /** 案源服务. */
  private final LeadAppService leadAppService;

  /**
   * 查询案源列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("lead:list")
  @Operation(summary = "查询案源列表")
  public Result<PageResult<LeadDTO>> listLeads(final LeadQueryDTO query) {
    PageResult<LeadDTO> result = leadAppService.listLeads(query);
    return Result.success(result);
  }

  /**
   * 获取案源详情
   *
   * @param id 案源ID
   * @return 案源详情
   */
  @GetMapping("/{id}")
  @RequirePermission("lead:view")
  @Operation(summary = "获取案源详情")
  public Result<LeadDTO> getLead(@PathVariable final Long id) {
    LeadDTO lead = leadAppService.getLead(id);
    return Result.success(lead);
  }

  /**
   * 创建案源
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("lead:create")
  @Operation(summary = "创建案源")
  @OperationLog(module = "案源管理", action = "创建案源")
  public Result<LeadDTO> createLead(@RequestBody @Valid final CreateLeadCommand command) {
    LeadDTO lead = leadAppService.createLead(command);
    return Result.success(lead);
  }

  /**
   * 更新案源
   *
   * @param id 案源ID
   * @param command 更新命令
   * @return 更新结果
   */
  @PutMapping("/{id}")
  @RequirePermission("lead:update")
  @Operation(summary = "更新案源")
  @OperationLog(module = "案源管理", action = "更新案源")
  public Result<LeadDTO> updateLead(
      @PathVariable final Long id, @RequestBody @Valid final UpdateLeadCommand command) {
    LeadDTO lead = leadAppService.updateLead(id, command);
    return Result.success(lead);
  }

  /**
   * 删除案源
   *
   * @param id 案源ID
   * @return 无返回
   */
  @DeleteMapping("/{id}")
  @RequirePermission("lead:delete")
  @Operation(summary = "删除案源")
  @OperationLog(module = "案源管理", action = "删除案源")
  public Result<Void> deleteLead(@PathVariable final Long id) {
    leadAppService.deleteLead(id);
    return Result.success();
  }

  /**
   * 创建跟进记录
   *
   * @param id 案源ID
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping("/{id}/follow-up")
  @RequirePermission("lead:follow")
  @Operation(summary = "创建跟进记录")
  @OperationLog(module = "案源管理", action = "创建跟进记录")
  public Result<LeadFollowUpDTO> createFollowUp(
      @PathVariable final Long id, @RequestBody @Valid final CreateFollowUpCommand command) {
    command.setLeadId(id);
    LeadFollowUpDTO followUp = leadAppService.createFollowUp(command);
    return Result.success(followUp);
  }

  /**
   * 查询案源跟进记录
   *
   * @param id 案源ID
   * @return 跟进记录列表
   */
  @GetMapping("/{id}/follow-ups")
  @RequirePermission("lead:view")
  @Operation(summary = "查询案源跟进记录")
  public Result<List<LeadFollowUpDTO>> listFollowUps(@PathVariable final Long id) {
    List<LeadFollowUpDTO> followUps = leadAppService.listFollowUps(id);
    return Result.success(followUps);
  }

  /**
   * 案源转化
   *
   * @param id 案源ID
   * @param command 转化命令
   * @return 转化后的案源
   */
  @PostMapping("/{id}/convert")
  @RequirePermission("lead:convert")
  @Operation(summary = "案源转化", description = "将案源转化为正式客户和项目")
  @OperationLog(module = "案源管理", action = "案源转化")
  public Result<LeadDTO> convertLead(
      @PathVariable final Long id, @RequestBody @Valid final ConvertLeadCommand command) {
    command.setLeadId(id);
    LeadDTO lead = leadAppService.convertLead(command);
    return Result.success(lead);
  }

  /**
   * 放弃案源
   *
   * @param id 案源ID
   * @param reason 放弃原因
   * @return 无返回
   */
  @PostMapping("/{id}/abandon")
  @RequirePermission("lead:update")
  @Operation(summary = "放弃案源")
  @OperationLog(module = "案源管理", action = "放弃案源")
  public Result<Void> abandonLead(
      @PathVariable final Long id, @RequestParam(required = false) final String reason) {
    leadAppService.abandonLead(id, reason);
    return Result.success();
  }

  /**
   * 获取案源统计（M2-033~M2-034）
   *
   * @return 案源统计结果
   */
  @GetMapping("/statistics")
  @RequirePermission("lead:view")
  @Operation(summary = "获取案源统计", description = "统计案源来源渠道和转化率分析")
  public Result<LeadStatisticsDTO> getLeadStatistics() {
    return Result.success(leadAppService.getLeadStatistics());
  }
}
