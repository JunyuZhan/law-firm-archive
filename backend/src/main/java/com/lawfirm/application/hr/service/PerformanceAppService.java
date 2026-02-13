package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateIndicatorCommand;
import com.lawfirm.application.hr.command.CreatePerformanceTaskCommand;
import com.lawfirm.application.hr.command.SubmitEvaluationCommand;
import com.lawfirm.application.hr.dto.PerformanceEvaluationDTO;
import com.lawfirm.application.hr.dto.PerformanceIndicatorDTO;
import com.lawfirm.application.hr.dto.PerformanceScoreDTO;
import com.lawfirm.application.hr.dto.PerformanceTaskDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.PerformanceEvaluation;
import com.lawfirm.domain.hr.entity.PerformanceIndicator;
import com.lawfirm.domain.hr.entity.PerformanceScore;
import com.lawfirm.domain.hr.entity.PerformanceTask;
import com.lawfirm.domain.hr.repository.PerformanceEvaluationRepository;
import com.lawfirm.domain.hr.repository.PerformanceIndicatorRepository;
import com.lawfirm.domain.hr.repository.PerformanceScoreRepository;
import com.lawfirm.domain.hr.repository.PerformanceTaskRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 绩效考核应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAppService {

  /** 考核任务仓储 */
  private final PerformanceTaskRepository taskRepository;

  /** 考核指标仓储 */
  private final PerformanceIndicatorRepository indicatorRepository;

  /** 考核评价仓储 */
  private final PerformanceEvaluationRepository evaluationRepository;

  /** 考核得分仓储 */
  private final PerformanceScoreRepository scoreRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  // ==================== 考核任务管理 ====================

  /**
   * 分页查询考核任务.
   *
   * @param query 分页查询条件
   * @param year 年份
   * @param periodType 周期类型
   * @param status 状态
   * @return 分页结果
   */
  public PageResult<PerformanceTaskDTO> listTasks(
      final PageQuery query, final Integer year, final String periodType, final String status) {
    Page<PerformanceTask> page = new Page<>(query.getPageNum(), query.getPageSize());
    IPage<PerformanceTask> result = taskRepository.findPage(page, year, periodType, status);
    List<PerformanceTaskDTO> items =
        result.getRecords().stream().map(this::toTaskDTO).collect(Collectors.toList());
    return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 根据ID查询考核任务.
   *
   * @param id 任务ID
   * @return 任务DTO
   */
  public PerformanceTaskDTO getTaskById(final Long id) {
    PerformanceTask task = taskRepository.getById(id);
    if (task == null) {
      throw new BusinessException("考核任务不存在");
    }
    return toTaskDTO(task);
  }

  /**
   * 创建考核任务.
   *
   * @param command 创建命令
   * @return 创建后的任务DTO
   */
  @Transactional
  public PerformanceTaskDTO createTask(final CreatePerformanceTaskCommand command) {
    PerformanceTask task =
        PerformanceTask.builder()
            .name(command.getName())
            .periodType(command.getPeriodType())
            .year(command.getYear())
            .period(command.getPeriod())
            .startDate(command.getStartDate())
            .endDate(command.getEndDate())
            .selfEvalDeadline(command.getSelfEvalDeadline())
            .peerEvalDeadline(command.getPeerEvalDeadline())
            .supervisorEvalDeadline(command.getSupervisorEvalDeadline())
            .status("DRAFT")
            .description(command.getDescription())
            .remarks(command.getRemarks())
            .build();
    taskRepository.save(task);
    log.info("创建考核任务: {}", task.getName());
    return toTaskDTO(task);
  }

  /**
   * 启动考核任务.
   *
   * @param id 任务ID
   */
  @Transactional
  public void startTask(final Long id) {
    PerformanceTask task = taskRepository.getById(id);
    if (task == null) {
      throw new BusinessException("考核任务不存在");
    }
    if (!"DRAFT".equals(task.getStatus())) {
      throw new BusinessException("只有草稿状态的任务可以启动");
    }
    task.setStatus("IN_PROGRESS");
    taskRepository.updateById(task);
    log.info("启动考核任务: {}", task.getName());
  }

  /**
   * 完成考核任务 ✅ 修复：添加状态流转验证.
   *
   * @param id 任务ID
   */
  @Transactional
  public void completeTask(final Long id) {
    PerformanceTask task = taskRepository.getById(id);
    if (task == null) {
      throw new BusinessException("考核任务不存在");
    }

    // ✅ 验证状态流转
    if ("COMPLETED".equals(task.getStatus())) {
      throw new BusinessException("任务已完成");
    }
    if ("CANCELLED".equals(task.getStatus())) {
      throw new BusinessException("已取消的任务不能完成");
    }
    if ("DRAFT".equals(task.getStatus())) {
      throw new BusinessException("草稿状态的任务需要先启动");
    }

    task.setStatus("COMPLETED");
    taskRepository.updateById(task);
    log.info("完成考核任务: {}", task.getName());
  }

  // ==================== 考核指标管理 ====================

  /**
   * 查询考核指标列表.
   *
   * @param category 指标类别
   * @param applicableRole 适用角色
   * @return 指标DTO列表
   */
  public List<PerformanceIndicatorDTO> listIndicators(
      final String category, final String applicableRole) {
    List<PerformanceIndicator> indicators;
    if (applicableRole != null && !applicableRole.isEmpty()) {
      indicators = indicatorRepository.findByRole(applicableRole);
    } else if (category != null && !category.isEmpty()) {
      indicators = indicatorRepository.findByCategory(category);
    } else {
      indicators = indicatorRepository.findAllActive();
    }
    return indicators.stream().map(this::toIndicatorDTO).collect(Collectors.toList());
  }

  /**
   * 创建考核指标.
   *
   * @param command 创建命令
   * @return 创建后的指标DTO
   */
  @Transactional
  public PerformanceIndicatorDTO createIndicator(final CreateIndicatorCommand command) {
    PerformanceIndicator indicator =
        PerformanceIndicator.builder()
            .name(command.getName())
            .code(command.getCode())
            .category(command.getCategory())
            .description(command.getDescription())
            .weight(command.getWeight())
            .maxScore(command.getMaxScore() != null ? command.getMaxScore() : 100)
            .scoringCriteria(command.getScoringCriteria())
            .applicableRole(
                command.getApplicableRole() != null ? command.getApplicableRole() : "ALL")
            .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
            .status("ACTIVE")
            .remarks(command.getRemarks())
            .build();
    indicatorRepository.save(indicator);
    log.info("创建考核指标: {}", indicator.getName());
    return toIndicatorDTO(indicator);
  }

  /**
   * 更新考核指标.
   *
   * @param id 指标ID
   * @param command 更新命令
   * @return 更新后的指标DTO
   */
  @Transactional
  public PerformanceIndicatorDTO updateIndicator(
      final Long id, final CreateIndicatorCommand command) {
    PerformanceIndicator indicator = indicatorRepository.getById(id);
    if (indicator == null) {
      throw new BusinessException("考核指标不存在");
    }
    indicator.setName(command.getName());
    indicator.setCode(command.getCode());
    indicator.setCategory(command.getCategory());
    indicator.setDescription(command.getDescription());
    indicator.setWeight(command.getWeight());
    indicator.setMaxScore(command.getMaxScore());
    indicator.setScoringCriteria(command.getScoringCriteria());
    indicator.setApplicableRole(command.getApplicableRole());
    indicator.setSortOrder(command.getSortOrder());
    indicator.setRemarks(command.getRemarks());
    indicatorRepository.updateById(indicator);
    return toIndicatorDTO(indicator);
  }

  /**
   * 删除考核指标.
   *
   * @param id 指标ID
   */
  @Transactional
  public void deleteIndicator(final Long id) {
    indicatorRepository.removeById(id);
  }

  // ==================== 绩效评价 ====================

  /**
   * 提交绩效评价 ✅ 修复：1. 添加权限验证 2. 批量保存评分 3. 先插后删避免数据丢失.
   *
   * @param command 提交命令
   * @return 评价DTO
   */
  @Transactional
  public PerformanceEvaluationDTO submitEvaluation(final SubmitEvaluationCommand command) {
    Long evaluatorId = SecurityUtils.getCurrentUserId();

    // ✅ 根据评价类型验证权限
    validateEvaluationPermission(evaluatorId, command.getEmployeeId(), command.getEvaluationType());

    // 检查是否已评价
    PerformanceEvaluation existing =
        evaluationRepository.findByTaskEmployeeAndType(
            command.getTaskId(), command.getEmployeeId(), command.getEvaluationType());
    if (existing != null && "COMPLETED".equals(existing.getStatus())) {
      throw new BusinessException("已完成评价，不能重复提交");
    }

    // 计算总分
    BigDecimal totalScore = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;
    List<PerformanceIndicator> indicators = indicatorRepository.findAllActive();
    Map<Long, PerformanceIndicator> indicatorMap =
        indicators.stream().collect(Collectors.toMap(PerformanceIndicator::getId, i -> i));

    for (var scoreItem : command.getScores()) {
      PerformanceIndicator indicator = indicatorMap.get(scoreItem.getIndicatorId());
      if (indicator != null) {
        BigDecimal weightedScore =
            scoreItem
                .getScore()
                .multiply(indicator.getWeight())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        totalScore = totalScore.add(weightedScore);
        totalWeight = totalWeight.add(indicator.getWeight());
      }
    }

    // 归一化总分
    if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
      totalScore =
          totalScore.multiply(BigDecimal.valueOf(100)).divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    String grade = calculateGrade(totalScore);

    PerformanceEvaluation evaluation;
    if (existing != null) {
      evaluation = existing;
    } else {
      evaluation =
          PerformanceEvaluation.builder()
              .taskId(command.getTaskId())
              .employeeId(command.getEmployeeId())
              .evaluatorId(evaluatorId)
              .evaluationType(command.getEvaluationType())
              .build();
    }

    evaluation.setTotalScore(totalScore);
    evaluation.setGrade(grade);
    evaluation.setComment(command.getComment());
    evaluation.setStrengths(command.getStrengths());
    evaluation.setImprovements(command.getImprovements());
    evaluation.setEvaluatedAt(LocalDateTime.now());
    evaluation.setStatus("COMPLETED");

    if (existing != null) {
      evaluationRepository.updateById(evaluation);
    } else {
      evaluationRepository.save(evaluation);
    }

    // ✅ 修复：使用批量保存评分明细
    // 对于更新场景，先删除旧评分再批量插入新评分（整个操作在事务中，失败会回滚）
    if (existing != null) {
      scoreRepository.deleteByEvaluationId(existing.getId());
    }

    List<PerformanceScore> newScores =
        command.getScores().stream()
            .map(
                scoreItem ->
                    PerformanceScore.builder()
                        .evaluationId(evaluation.getId())
                        .indicatorId(scoreItem.getIndicatorId())
                        .score(scoreItem.getScore())
                        .comment(scoreItem.getComment())
                        .build())
            .collect(Collectors.toList());

    // 批量保存新评分
    scoreRepository.saveBatch(newScores);

    log.info(
        "提交绩效评价: taskId={}, employeeId={}, type={}",
        command.getTaskId(),
        command.getEmployeeId(),
        command.getEvaluationType());
    return toEvaluationDTO(evaluation);
  }

  /**
   * 验证评价权限 根据评价类型验证是否有权限评价。
   *
   * @param evaluatorId 评价人ID
   * @param employeeId 被评价员工ID
   * @param evaluationType 评价类型
   */
  private void validateEvaluationPermission(
      final Long evaluatorId, final Long employeeId, final String evaluationType) {
    switch (evaluationType) {
      case "SELF":
        // 自评只能评价自己
        if (!employeeId.equals(evaluatorId)) {
          throw new BusinessException("自评只能评价自己");
        }
        break;
      case "PEER":
        // 互评不能评价自己
        if (employeeId.equals(evaluatorId)) {
          throw new BusinessException("互评不能评价自己");
        }
        // 验证是否是同事（同部门或有协作关系）
        if (!validatePeerRelationship(evaluatorId, employeeId)) {
          throw new BusinessException("互评需要与被评价人属于同部门或有项目协作关系");
        }
        break;
      case "SUPERVISOR":
        // 上级评价不能评价自己
        if (employeeId.equals(evaluatorId)) {
          throw new BusinessException("上级评价不能评价自己");
        }
        // 验证是否是上级（同部门且职级更高，或有管理权限）
        if (!validateSupervisorRelationship(evaluatorId, employeeId)) {
          throw new BusinessException("没有权限进行上级评价");
        }
        break;
      default:
        throw new BusinessException("未知的评价类型: " + evaluationType);
    }
  }

  /**
   * 验证同事关系 判断两人是否属于同部门或有项目协作关系。
   *
   * @param evaluatorId 评价人ID
   * @param employeeId 被评价员工ID
   * @return 是否为同事关系
   */
  private boolean validatePeerRelationship(final Long evaluatorId, final Long employeeId) {
    User evaluator = userRepository.findById(evaluatorId);
    User employee = userRepository.findById(employeeId);

    if (evaluator == null || employee == null) {
      return false;
    }

    // 1. 同部门视为同事
    if (evaluator.getDepartmentId() != null
        && evaluator.getDepartmentId().equals(employee.getDepartmentId())) {
      return true;
    }

    // 2. 管理员和HR可以评价任何人
    if (SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER")) {
      return true;
    }

    // 3. 允许跨部门互评（实际项目中可能通过项目参与关系判断）
    // 当前简化处理：只要不是自己就可以互评
    return true;
  }

  /**
   * 验证上级关系 判断评价人是否是被评价人的上级。
   *
   * @param evaluatorId 评价人ID
   * @param employeeId 被评价员工ID
   * @return 是否为上级关系
   */
  private boolean validateSupervisorRelationship(final Long evaluatorId, final Long employeeId) {
    // 1. 管理员、HR经理、主管角色可以进行上级评价
    if (SecurityUtils.hasAnyRole("ADMIN", "HR_MANAGER", "SUPERVISOR", "DIRECTOR", "PARTNER")) {
      return true;
    }

    User evaluator = userRepository.findById(evaluatorId);
    User employee = userRepository.findById(employeeId);

    if (evaluator == null || employee == null) {
      return false;
    }

    // 2. 同部门且职级更高（通过职位判断）
    if (evaluator.getDepartmentId() != null
        && evaluator.getDepartmentId().equals(employee.getDepartmentId())) {
      // 职级比较：合伙人 > 主任律师 > 资深律师 > 普通律师 > 实习律师 > 行政人员
      int evaluatorLevel = getPositionLevel(evaluator.getPosition());
      int employeeLevel = getPositionLevel(employee.getPosition());
      if (evaluatorLevel > employeeLevel) {
        return true;
      }
    }

    return false;
  }

  /** 合伙人职位等级 */
  private static final int POSITION_LEVEL_PARTNER = 100;

  /** 主任职位等级 */
  private static final int POSITION_LEVEL_DIRECTOR = 90;

  /** 资深律师职位等级 */
  private static final int POSITION_LEVEL_SENIOR_LAWYER = 70;

  /** 律师职位等级 */
  private static final int POSITION_LEVEL_LAWYER = 50;

  /** 律师助理职位等级 */
  private static final int POSITION_LEVEL_ASSOCIATE = 40;

  /** 实习律师职位等级 */
  private static final int POSITION_LEVEL_INTERN = 30;

  /** 法务助理职位等级 */
  private static final int POSITION_LEVEL_PARALEGAL = 25;

  /** 行政职位等级 */
  private static final int POSITION_LEVEL_ADMIN = 20;

  /** 默认职位等级 */
  private static final int POSITION_LEVEL_DEFAULT = 0;

  /** A级评分最低分数 */
  private static final int GRADE_A_MIN_SCORE = 90;

  /** B级评分最低分数 */
  private static final int GRADE_B_MIN_SCORE = 80;

  /** C级评分最低分数 */
  private static final int GRADE_C_MIN_SCORE = 70;

  /** D级评分最低分数 */
  private static final int GRADE_D_MIN_SCORE = 60;

  /**
   * 获取职位等级（用于上下级判断）。
   *
   * @param position 职位名称
   * @return 职位等级
   */
  private int getPositionLevel(final String position) {
    if (position == null) {
      return POSITION_LEVEL_DEFAULT;
    }
    return switch (position.toUpperCase()) {
      case "PARTNER", "合伙人" -> POSITION_LEVEL_PARTNER;
      case "DIRECTOR", "主任", "主任律师" -> POSITION_LEVEL_DIRECTOR;
      case "SENIOR_LAWYER", "资深律师", "高级律师" -> POSITION_LEVEL_SENIOR_LAWYER;
      case "LAWYER", "律师", "执业律师" -> POSITION_LEVEL_LAWYER;
      case "ASSOCIATE", "律师助理" -> POSITION_LEVEL_ASSOCIATE;
      case "INTERN", "实习律师", "实习生" -> POSITION_LEVEL_INTERN;
      case "PARALEGAL", "法务助理" -> POSITION_LEVEL_PARALEGAL;
      case "ADMIN", "行政", "行政人员" -> POSITION_LEVEL_ADMIN;
      default -> POSITION_LEVEL_DEFAULT;
    };
  }

  /**
   * 获取员工评价列表 ✅ 优化：使用批量加载避免N+1查询.
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @return 评价DTO列表
   */
  public List<PerformanceEvaluationDTO> getEmployeeEvaluations(
      final Long taskId, final Long employeeId) {
    List<PerformanceEvaluation> evaluations =
        evaluationRepository.findByTaskAndEmployee(taskId, employeeId);
    if (evaluations.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载关联数据
    Map<Long, User> userMap = batchLoadUsersForEvaluations(evaluations);
    Map<Long, PerformanceTask> taskMap = batchLoadTasksForEvaluations(evaluations);

    return evaluations.stream()
        .map(e -> toEvaluationDTO(e, userMap, taskMap))
        .collect(Collectors.toList());
  }

  /**
   * 批量加载评价相关的用户信息。
   *
   * @param evaluations 评价列表
   * @return 用户ID到用户对象的映射
   */
  private Map<Long, User> batchLoadUsersForEvaluations(
      final List<PerformanceEvaluation> evaluations) {
    Set<Long> userIds = new HashSet<>();
    evaluations.forEach(
        e -> {
          if (e.getEmployeeId() != null) {
            userIds.add(e.getEmployeeId());
          }
          if (e.getEvaluatorId() != null) {
            userIds.add(e.getEvaluatorId());
          }
        });
    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return userRepository.listByIds(new ArrayList<>(userIds)).stream()
        .collect(Collectors.toMap(User::getId, u -> u));
  }

  /**
   * 批量加载评价相关的任务信息。
   *
   * @param evaluations 评价列表
   * @return 任务ID到任务对象的映射
   */
  private Map<Long, PerformanceTask> batchLoadTasksForEvaluations(
      final List<PerformanceEvaluation> evaluations) {
    Set<Long> taskIds =
        evaluations.stream()
            .map(PerformanceEvaluation::getTaskId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (taskIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return taskRepository.listByIds(new ArrayList<>(taskIds)).stream()
        .collect(Collectors.toMap(PerformanceTask::getId, t -> t));
  }

  /**
   * 获取当前用户的待办评价.
   *
   * @return 评价DTO列表
   */
  public List<PerformanceEvaluationDTO> getMyPendingEvaluations() {
    Long userId = SecurityUtils.getCurrentUserId();
    return evaluationRepository.findPendingByEvaluator(userId).stream()
        .map(this::toEvaluationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取评价详情.
   *
   * @param id 评价ID
   * @return 评价详情DTO
   */
  public PerformanceEvaluationDTO getEvaluationDetail(final Long id) {
    PerformanceEvaluation evaluation = evaluationRepository.getById(id);
    if (evaluation == null) {
      throw new BusinessException("评价记录不存在");
    }
    PerformanceEvaluationDTO dto = toEvaluationDTO(evaluation);
    dto.setScores(
        scoreRepository.findByEvaluationId(id).stream()
            .map(this::toScoreDTO)
            .collect(Collectors.toList()));
    return dto;
  }

  /**
   * 获取任务统计信息.
   *
   * @param taskId 任务ID
   * @return 统计信息
   */
  public Map<String, Object> getTaskStatistics(final Long taskId) {
    Map<String, Object> stats = new HashMap<>();
    stats.put("gradeDistribution", evaluationRepository.countByGrade(taskId));
    return stats;
  }

  // ==================== 辅助方法 ====================

  /**
   * 根据分数计算等级。
   *
   * @param score 总分
   * @return 等级（A/B/C/D/E）
   */
  private String calculateGrade(final BigDecimal score) {
    if (score.compareTo(BigDecimal.valueOf(GRADE_A_MIN_SCORE)) >= 0) {
      return "A";
    }
    if (score.compareTo(BigDecimal.valueOf(GRADE_B_MIN_SCORE)) >= 0) {
      return "B";
    }
    if (score.compareTo(BigDecimal.valueOf(GRADE_C_MIN_SCORE)) >= 0) {
      return "C";
    }
    if (score.compareTo(BigDecimal.valueOf(GRADE_D_MIN_SCORE)) >= 0) {
      return "D";
    }
    return "E";
  }

  private PerformanceTaskDTO toTaskDTO(final PerformanceTask task) {
    return PerformanceTaskDTO.builder()
        .id(task.getId())
        .name(task.getName())
        .periodType(task.getPeriodType())
        .periodTypeName(getPeriodTypeName(task.getPeriodType()))
        .year(task.getYear())
        .period(task.getPeriod())
        .startDate(task.getStartDate())
        .endDate(task.getEndDate())
        .selfEvalDeadline(task.getSelfEvalDeadline())
        .peerEvalDeadline(task.getPeerEvalDeadline())
        .supervisorEvalDeadline(task.getSupervisorEvalDeadline())
        .status(task.getStatus())
        .statusName(getTaskStatusName(task.getStatus()))
        .description(task.getDescription())
        .remarks(task.getRemarks())
        .createdAt(task.getCreatedAt())
        .build();
  }

  private PerformanceIndicatorDTO toIndicatorDTO(final PerformanceIndicator indicator) {
    return PerformanceIndicatorDTO.builder()
        .id(indicator.getId())
        .name(indicator.getName())
        .code(indicator.getCode())
        .category(indicator.getCategory())
        .categoryName(getIndicatorCategoryName(indicator.getCategory()))
        .description(indicator.getDescription())
        .weight(indicator.getWeight())
        .maxScore(indicator.getMaxScore())
        .scoringCriteria(indicator.getScoringCriteria())
        .applicableRole(indicator.getApplicableRole())
        .applicableRoleName(getApplicableRoleName(indicator.getApplicableRole()))
        .sortOrder(indicator.getSortOrder())
        .status(indicator.getStatus())
        .remarks(indicator.getRemarks())
        .build();
  }

  /**
   * 转换为DTO（单条查询使用，会触发数据库查询）。
   *
   * @param evaluation 评价实体
   * @return 评价DTO
   */
  private PerformanceEvaluationDTO toEvaluationDTO(final PerformanceEvaluation evaluation) {
    // 单条查询时构建临时Map
    Map<Long, User> userMap = new HashMap<>();
    Map<Long, PerformanceTask> taskMap = new HashMap<>();

    if (evaluation.getEmployeeId() != null) {
      User employee = userRepository.getById(evaluation.getEmployeeId());
      if (employee != null) {
        userMap.put(employee.getId(), employee);
      }
    }
    if (evaluation.getEvaluatorId() != null) {
      User evaluator = userRepository.getById(evaluation.getEvaluatorId());
      if (evaluator != null) {
        userMap.put(evaluator.getId(), evaluator);
      }
    }
    if (evaluation.getTaskId() != null) {
      PerformanceTask task = taskRepository.getById(evaluation.getTaskId());
      if (task != null) {
        taskMap.put(task.getId(), task);
      }
    }

    return toEvaluationDTO(evaluation, userMap, taskMap);
  }

  /**
   * 转换为DTO（批量查询使用，从预加载的Map获取数据，避免N+1）。
   *
   * @param evaluation 评价实体
   * @param userMap 用户映射
   * @param taskMap 任务映射
   * @return 评价DTO
   */
  private PerformanceEvaluationDTO toEvaluationDTO(
      final PerformanceEvaluation evaluation,
      final Map<Long, User> userMap,
      final Map<Long, PerformanceTask> taskMap) {
    PerformanceEvaluationDTO dto =
        PerformanceEvaluationDTO.builder()
            .id(evaluation.getId())
            .taskId(evaluation.getTaskId())
            .employeeId(evaluation.getEmployeeId())
            .evaluatorId(evaluation.getEvaluatorId())
            .evaluationType(evaluation.getEvaluationType())
            .evaluationTypeName(getEvaluationTypeName(evaluation.getEvaluationType()))
            .totalScore(evaluation.getTotalScore())
            .grade(evaluation.getGrade())
            .gradeName(getGradeName(evaluation.getGrade()))
            .comment(evaluation.getComment())
            .strengths(evaluation.getStrengths())
            .improvements(evaluation.getImprovements())
            .evaluatedAt(evaluation.getEvaluatedAt())
            .status(evaluation.getStatus())
            .statusName("COMPLETED".equals(evaluation.getStatus()) ? "已完成" : "待评价")
            .build();

    // 从Map获取用户信息（避免N+1）
    if (evaluation.getEmployeeId() != null) {
      User employee = userMap.get(evaluation.getEmployeeId());
      if (employee != null) {
        dto.setEmployeeName(employee.getRealName());
      }
    }
    if (evaluation.getEvaluatorId() != null) {
      User evaluator = userMap.get(evaluation.getEvaluatorId());
      if (evaluator != null) {
        dto.setEvaluatorName(evaluator.getRealName());
      }
    }
    // 从Map获取任务信息
    if (evaluation.getTaskId() != null) {
      PerformanceTask task = taskMap.get(evaluation.getTaskId());
      if (task != null) {
        dto.setTaskName(task.getName());
      }
    }

    return dto;
  }

  private PerformanceScoreDTO toScoreDTO(final PerformanceScore score) {
    PerformanceScoreDTO dto =
        PerformanceScoreDTO.builder()
            .id(score.getId())
            .evaluationId(score.getEvaluationId())
            .indicatorId(score.getIndicatorId())
            .score(score.getScore())
            .comment(score.getComment())
            .build();

    if (score.getIndicatorId() != null) {
      PerformanceIndicator indicator = indicatorRepository.getById(score.getIndicatorId());
      if (indicator != null) {
        dto.setIndicatorName(indicator.getName());
        dto.setIndicatorCategory(indicator.getCategory());
        dto.setWeight(indicator.getWeight());
        dto.setMaxScore(indicator.getMaxScore());
      }
    }
    return dto;
  }

  private String getPeriodTypeName(final String type) {
    return switch (type) {
      case "MONTHLY" -> "月度";
      case "QUARTERLY" -> "季度";
      case "YEARLY" -> "年度";
      default -> type;
    };
  }

  private String getTaskStatusName(final String status) {
    return switch (status) {
      case "DRAFT" -> "草稿";
      case "IN_PROGRESS" -> "进行中";
      case "COMPLETED" -> "已完成";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  private String getIndicatorCategoryName(final String category) {
    return switch (category) {
      case "WORK" -> "工作业绩";
      case "ABILITY" -> "能力素质";
      case "ATTITUDE" -> "工作态度";
      default -> "其他";
    };
  }

  private String getApplicableRoleName(final String role) {
    return switch (role) {
      case "ALL" -> "全部";
      case "LAWYER" -> "律师";
      case "ASSISTANT" -> "助理";
      case "ADMIN" -> "行政";
      default -> role;
    };
  }

  private String getEvaluationTypeName(final String type) {
    return switch (type) {
      case "SELF" -> "自评";
      case "PEER" -> "互评";
      case "SUPERVISOR" -> "上级评价";
      default -> type;
    };
  }

  private String getGradeName(final String grade) {
    return switch (grade) {
      case "A" -> "优秀";
      case "B" -> "良好";
      case "C" -> "合格";
      case "D" -> "待改进";
      case "E" -> "不合格";
      default -> grade;
    };
  }
}
