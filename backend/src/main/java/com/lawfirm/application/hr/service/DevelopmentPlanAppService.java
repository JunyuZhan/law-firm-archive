package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.hr.command.CreateDevelopmentPlanCommand;
import com.lawfirm.application.hr.dto.DevelopmentMilestoneDTO;
import com.lawfirm.application.hr.dto.DevelopmentPlanDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.domain.hr.entity.DevelopmentMilestone;
import com.lawfirm.domain.hr.entity.DevelopmentPlan;
import com.lawfirm.domain.hr.repository.CareerLevelRepository;
import com.lawfirm.domain.hr.repository.DevelopmentMilestoneRepository;
import com.lawfirm.domain.hr.repository.DevelopmentPlanRepository;
import com.lawfirm.infrastructure.persistence.mapper.DevelopmentPlanMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 发展规划应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevelopmentPlanAppService {

  /** 发展规划仓储 */
  private final DevelopmentPlanRepository planRepository;

  /** 发展规划Mapper */
  private final DevelopmentPlanMapper planMapper;

  /** 发展里程碑仓储 */
  private final DevelopmentMilestoneRepository milestoneRepository;

  /** 职业级别仓储 */
  private final CareerLevelRepository levelRepository;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /**
   * 分页查询发展规划.
   *
   * @param pageNum 页码
   * @param pageSize 每页条数
   * @param keyword 关键字
   * @param status 状态
   * @param employeeId 员工ID
   * @param planYear 计划年份
   * @return 分页结果
   */
  public PageResult<DevelopmentPlanDTO> listPlans(
      final int pageNum,
      final int pageSize,
      final String keyword,
      final String status,
      final Long employeeId,
      final Integer planYear) {
    IPage<DevelopmentPlan> page =
        planMapper.selectPlanPage(
            new Page<>(pageNum, pageSize), keyword, status, employeeId, planYear);
    return PageResult.of(
        page.getRecords().stream().map(this::toPlanDTO).collect(Collectors.toList()),
        page.getTotal(),
        pageNum,
        pageSize);
  }

  /**
   * 获取规划详情.
   *
   * @param id 规划ID
   * @return 规划DTO
   */
  public DevelopmentPlanDTO getPlanById(final Long id) {
    DevelopmentPlan plan = planRepository.getByIdOrThrow(id, "发展规划不存在");
    DevelopmentPlanDTO dto = toPlanDTO(plan);
    // 加载里程碑
    dto.setMilestones(
        milestoneRepository.findByPlanId(id).stream()
            .map(this::toMilestoneDTO)
            .collect(Collectors.toList()));
    return dto;
  }

  /**
   * 获取我的当年规划.
   *
   * @return 规划DTO
   */
  public DevelopmentPlanDTO getMyCurrentPlan() {
    Long userId = SecurityUtils.getUserId();
    int currentYear = LocalDate.now().getYear();
    return planRepository
        .findByEmployeeAndYear(userId, currentYear)
        .map(
            plan -> {
              DevelopmentPlanDTO dto = toPlanDTO(plan);
              dto.setMilestones(
                  milestoneRepository.findByPlanId(plan.getId()).stream()
                      .map(this::toMilestoneDTO)
                      .collect(Collectors.toList()));
              return dto;
            })
        .orElse(null);
  }

  /**
   * 创建发展规划.
   *
   * @param command 创建命令
   * @return 规划DTO
   */
  @Transactional
  public DevelopmentPlanDTO createPlan(final CreateDevelopmentPlanCommand command) {
    Long userId = SecurityUtils.getUserId();
    String userName = SecurityUtils.getRealName();

    // 检查是否已有当年规划
    if (planRepository.findByEmployeeAndYear(userId, command.getPlanYear()).isPresent()) {
      throw new BusinessException("该年度已存在发展规划");
    }

    DevelopmentPlan plan =
        DevelopmentPlan.builder()
            .planNo(generatePlanNo())
            .employeeId(userId)
            .employeeName(userName)
            .planYear(command.getPlanYear())
            .planTitle(command.getPlanTitle())
            .targetDate(command.getTargetDate())
            .careerGoals(toJson(command.getCareerGoals()))
            .skillGoals(toJson(command.getSkillGoals()))
            .performanceGoals(toJson(command.getPerformanceGoals()))
            .actionPlans(toJson(command.getActionPlans()))
            .requiredTraining(command.getRequiredTraining())
            .requiredResources(command.getRequiredResources())
            .mentorId(command.getMentorId())
            .progressPercentage(0)
            .status("DRAFT")
            .build();

    // 设置目标职级
    if (command.getTargetLevelId() != null) {
      CareerLevel targetLevel =
          levelRepository.getByIdOrThrow(command.getTargetLevelId(), "目标职级不存在");
      plan.setTargetLevelId(targetLevel.getId());
      plan.setTargetLevelName(targetLevel.getLevelName());
    }

    planRepository.save(plan);

    // ✅ 修复：使用批量插入替代循环插入
    if (command.getMilestones() != null && !command.getMilestones().isEmpty()) {
      List<DevelopmentMilestone> milestones = new ArrayList<>();
      int order = 0;
      for (CreateDevelopmentPlanCommand.MilestoneItem item : command.getMilestones()) {
        DevelopmentMilestone milestone =
            DevelopmentMilestone.builder()
                .planId(plan.getId())
                .milestoneName(item.getMilestoneName())
                .description(item.getDescription())
                .targetDate(item.getTargetDate())
                .status("PENDING")
                .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : order++)
                .build();
        milestones.add(milestone);
      }
      milestoneRepository.saveBatch(milestones);
    }

    log.info("创建发展规划: {}", plan.getPlanNo());
    return getPlanById(plan.getId());
  }

  /**
   * 更新发展规划.
   *
   * @param id 规划ID
   * @param command 更新命令
   * @return 规划DTO
   */
  @Transactional
  public DevelopmentPlanDTO updatePlan(final Long id, final CreateDevelopmentPlanCommand command) {
    DevelopmentPlan plan = planRepository.getByIdOrThrow(id, "发展规划不存在");

    if ("COMPLETED".equals(plan.getStatus())) {
      throw new BusinessException("已完成的规划不能修改");
    }

    plan.setPlanTitle(command.getPlanTitle());
    plan.setTargetDate(command.getTargetDate());
    plan.setCareerGoals(toJson(command.getCareerGoals()));
    plan.setSkillGoals(toJson(command.getSkillGoals()));
    plan.setPerformanceGoals(toJson(command.getPerformanceGoals()));
    plan.setActionPlans(toJson(command.getActionPlans()));
    plan.setRequiredTraining(command.getRequiredTraining());
    plan.setRequiredResources(command.getRequiredResources());
    plan.setMentorId(command.getMentorId());

    if (command.getTargetLevelId() != null) {
      CareerLevel targetLevel =
          levelRepository.getByIdOrThrow(command.getTargetLevelId(), "目标职级不存在");
      plan.setTargetLevelId(targetLevel.getId());
      plan.setTargetLevelName(targetLevel.getLevelName());
    }

    planRepository.updateById(plan);

    // ✅ 修复：更新里程碑（事务内操作，失败会回滚）
    // 由于MilestoneItem没有ID字段，无法实现智能更新，采用批量替换策略
    if (command.getMilestones() != null) {
      // 先构建新里程碑列表（验证数据有效性）
      List<DevelopmentMilestone> newMilestones = new ArrayList<>();
      int order = 0;
      for (CreateDevelopmentPlanCommand.MilestoneItem item : command.getMilestones()) {
        DevelopmentMilestone milestone =
            DevelopmentMilestone.builder()
                .planId(plan.getId())
                .milestoneName(item.getMilestoneName())
                .description(item.getDescription())
                .targetDate(item.getTargetDate())
                .status("PENDING")
                .sortOrder(item.getSortOrder() != null ? item.getSortOrder() : order++)
                .build();
        newMilestones.add(milestone);
      }

      // 删除旧里程碑
      milestoneRepository.deleteByPlanId(id);

      // 批量插入新里程碑（事务内，如果失败会回滚删除操作）
      if (!newMilestones.isEmpty()) {
        milestoneRepository.saveBatch(newMilestones);
      }
    }

    log.info("更新发展规划: {}", plan.getPlanNo());
    return getPlanById(id);
  }

  /**
   * 删除发展规划.
   *
   * @param id 规划ID
   */
  @Transactional
  public void deletePlan(final Long id) {
    DevelopmentPlan plan = planRepository.getByIdOrThrow(id, "发展规划不存在");

    if ("ACTIVE".equals(plan.getStatus())) {
      throw new BusinessException("执行中的规划不能删除");
    }

    milestoneRepository.deleteByPlanId(id);
    planRepository.softDelete(id);
    log.info("删除发展规划: {}", plan.getPlanNo());
  }

  /**
   * 提交规划（草稿->待审核）.
   *
   * @param id 规划ID
   */
  @Transactional
  public void submitPlan(final Long id) {
    DevelopmentPlan plan = planRepository.getByIdOrThrow(id, "发展规划不存在");

    if (!"DRAFT".equals(plan.getStatus())) {
      throw new BusinessException("只能提交草稿状态的规划");
    }

    plan.setStatus("ACTIVE");
    planRepository.updateById(plan);
    log.info("提交发展规划: {}", plan.getPlanNo());
  }

  /**
   * 审核规划.
   *
   * @param id 规划ID
   * @param comment 审核意见
   */
  @Transactional
  public void reviewPlan(final Long id, final String comment) {
    DevelopmentPlan plan = planRepository.getByIdOrThrow(id, "发展规划不存在");

    plan.setReviewedBy(SecurityUtils.getUserId());
    plan.setReviewedByName(SecurityUtils.getRealName());
    plan.setReviewedAt(LocalDateTime.now());
    plan.setReviewComment(comment);

    planRepository.updateById(plan);
    log.info("审核发展规划: {}", plan.getPlanNo());
  }

  /**
   * 更新里程碑状态.
   *
   * @param milestoneId 里程碑ID
   * @param status 状态
   * @param completionNote 完成备注
   */
  @Transactional
  public void updateMilestoneStatus(
      final Long milestoneId, final String status, final String completionNote) {
    DevelopmentMilestone milestone = milestoneRepository.getById(milestoneId);
    if (milestone == null) {
      throw new BusinessException("里程碑不存在");
    }

    milestone.setStatus(status);
    if ("COMPLETED".equals(status)) {
      milestone.setCompletedDate(LocalDate.now());
    }
    milestone.setCompletionNote(completionNote);
    milestoneRepository.updateById(milestone);

    // 更新规划进度
    updatePlanProgress(milestone.getPlanId());

    log.info("更新里程碑状态: milestoneId={}, status={}", milestoneId, status);
  }

  /**
   * 更新规划进度.
   *
   * @param planId 规划ID
   */
  private void updatePlanProgress(final Long planId) {
    int completed = milestoneRepository.countCompleted(planId);
    int total = milestoneRepository.countTotal(planId);

    int progress = total > 0 ? (completed * 100 / total) : 0;

    DevelopmentPlan plan = planRepository.getById(planId);
    if (plan != null) {
      plan.setProgressPercentage(progress);
      if (progress >= 100) {
        plan.setStatus("COMPLETED");
      }
      planRepository.updateById(plan);
    }
  }

  // ==================== 私有方法 ====================

  private DevelopmentPlanDTO toPlanDTO(final DevelopmentPlan plan) {
    DevelopmentPlanDTO dto = new DevelopmentPlanDTO();
    dto.setId(plan.getId());
    dto.setPlanNo(plan.getPlanNo());
    dto.setEmployeeId(plan.getEmployeeId());
    dto.setEmployeeName(plan.getEmployeeName());
    dto.setPlanYear(plan.getPlanYear());
    dto.setPlanTitle(plan.getPlanTitle());
    dto.setCurrentLevelId(plan.getCurrentLevelId());
    dto.setCurrentLevelName(plan.getCurrentLevelName());
    dto.setTargetLevelId(plan.getTargetLevelId());
    dto.setTargetLevelName(plan.getTargetLevelName());
    dto.setTargetDate(plan.getTargetDate());
    dto.setCareerGoals(parseJsonList(plan.getCareerGoals()));
    dto.setSkillGoals(parseJsonList(plan.getSkillGoals()));
    dto.setPerformanceGoals(parseJsonList(plan.getPerformanceGoals()));
    dto.setActionPlans(parseJsonList(plan.getActionPlans()));
    dto.setRequiredTraining(plan.getRequiredTraining());
    dto.setRequiredResources(plan.getRequiredResources());
    dto.setMentorId(plan.getMentorId());
    dto.setMentorName(plan.getMentorName());
    dto.setProgressPercentage(plan.getProgressPercentage());
    dto.setProgressNotes(plan.getProgressNotes());
    dto.setStatus(plan.getStatus());
    dto.setStatusName(getStatusName(plan.getStatus()));
    dto.setReviewedBy(plan.getReviewedBy());
    dto.setReviewedByName(plan.getReviewedByName());
    dto.setReviewedAt(plan.getReviewedAt());
    dto.setReviewComment(plan.getReviewComment());
    dto.setCreatedAt(plan.getCreatedAt());
    dto.setUpdatedAt(plan.getUpdatedAt());
    return dto;
  }

  private DevelopmentMilestoneDTO toMilestoneDTO(final DevelopmentMilestone milestone) {
    DevelopmentMilestoneDTO dto = new DevelopmentMilestoneDTO();
    dto.setId(milestone.getId());
    dto.setPlanId(milestone.getPlanId());
    dto.setMilestoneName(milestone.getMilestoneName());
    dto.setDescription(milestone.getDescription());
    dto.setTargetDate(milestone.getTargetDate());
    dto.setStatus(milestone.getStatus());
    dto.setStatusName(getMilestoneStatusName(milestone.getStatus()));
    dto.setCompletedDate(milestone.getCompletedDate());
    dto.setCompletionNote(milestone.getCompletionNote());
    dto.setSortOrder(milestone.getSortOrder());
    dto.setCreatedAt(milestone.getCreatedAt());
    dto.setUpdatedAt(milestone.getUpdatedAt());
    return dto;
  }

  private String generatePlanNo() {
    return "PLAN" + System.currentTimeMillis();
  }

  private String toJson(final Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private List<String> parseJsonList(final String json) {
    if (json == null || json.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      return Collections.emptyList();
    }
  }

  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "DRAFT" -> "草稿";
      case "ACTIVE" -> "执行中";
      case "COMPLETED" -> "已完成";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  private String getMilestoneStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "PENDING" -> "待完成";
      case "IN_PROGRESS" -> "进行中";
      case "COMPLETED" -> "已完成";
      case "DELAYED" -> "已延期";
      default -> status;
    };
  }
}
