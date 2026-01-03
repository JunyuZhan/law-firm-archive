package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateIndicatorCommand;
import com.lawfirm.application.hr.command.CreatePerformanceTaskCommand;
import com.lawfirm.application.hr.command.SubmitEvaluationCommand;
import com.lawfirm.application.hr.dto.*;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.*;
import com.lawfirm.domain.hr.repository.*;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 绩效考核应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAppService {

    private final PerformanceTaskRepository taskRepository;
    private final PerformanceIndicatorRepository indicatorRepository;
    private final PerformanceEvaluationRepository evaluationRepository;
    private final PerformanceScoreRepository scoreRepository;
    private final UserRepository userRepository;

    // ==================== 考核任务管理 ====================

    public PageResult<PerformanceTaskDTO> listTasks(PageQuery query, Integer year, String periodType, String status) {
        Page<PerformanceTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PerformanceTask> result = taskRepository.findPage(page, year, periodType, status);
        List<PerformanceTaskDTO> items = result.getRecords().stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    public PerformanceTaskDTO getTaskById(Long id) {
        PerformanceTask task = taskRepository.getById(id);
        if (task == null) {
            throw new BusinessException("考核任务不存在");
        }
        return toTaskDTO(task);
    }

    @Transactional
    public PerformanceTaskDTO createTask(CreatePerformanceTaskCommand command) {
        PerformanceTask task = PerformanceTask.builder()
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

    @Transactional
    public void startTask(Long id) {
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

    @Transactional
    public void completeTask(Long id) {
        PerformanceTask task = taskRepository.getById(id);
        if (task == null) {
            throw new BusinessException("考核任务不存在");
        }
        task.setStatus("COMPLETED");
        taskRepository.updateById(task);
        log.info("完成考核任务: {}", task.getName());
    }


    // ==================== 考核指标管理 ====================

    public List<PerformanceIndicatorDTO> listIndicators(String category, String applicableRole) {
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

    @Transactional
    public PerformanceIndicatorDTO createIndicator(CreateIndicatorCommand command) {
        PerformanceIndicator indicator = PerformanceIndicator.builder()
                .name(command.getName())
                .code(command.getCode())
                .category(command.getCategory())
                .description(command.getDescription())
                .weight(command.getWeight())
                .maxScore(command.getMaxScore() != null ? command.getMaxScore() : 100)
                .scoringCriteria(command.getScoringCriteria())
                .applicableRole(command.getApplicableRole() != null ? command.getApplicableRole() : "ALL")
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .status("ACTIVE")
                .remarks(command.getRemarks())
                .build();
        indicatorRepository.save(indicator);
        log.info("创建考核指标: {}", indicator.getName());
        return toIndicatorDTO(indicator);
    }

    @Transactional
    public PerformanceIndicatorDTO updateIndicator(Long id, CreateIndicatorCommand command) {
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

    @Transactional
    public void deleteIndicator(Long id) {
        indicatorRepository.removeById(id);
    }

    // ==================== 绩效评价 ====================

    @Transactional
    public PerformanceEvaluationDTO submitEvaluation(SubmitEvaluationCommand command) {
        Long evaluatorId = SecurityUtils.getCurrentUserId();
        
        // 检查是否已评价
        PerformanceEvaluation existing = evaluationRepository.findByTaskEmployeeAndType(
                command.getTaskId(), command.getEmployeeId(), command.getEvaluationType());
        if (existing != null && "COMPLETED".equals(existing.getStatus())) {
            throw new BusinessException("已完成评价，不能重复提交");
        }

        // 计算总分
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        List<PerformanceIndicator> indicators = indicatorRepository.findAllActive();
        Map<Long, PerformanceIndicator> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(PerformanceIndicator::getId, i -> i));

        for (var scoreItem : command.getScores()) {
            PerformanceIndicator indicator = indicatorMap.get(scoreItem.getIndicatorId());
            if (indicator != null) {
                BigDecimal weightedScore = scoreItem.getScore()
                        .multiply(indicator.getWeight())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                totalScore = totalScore.add(weightedScore);
                totalWeight = totalWeight.add(indicator.getWeight());
            }
        }

        // 归一化总分
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            totalScore = totalScore.multiply(BigDecimal.valueOf(100)).divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        String grade = calculateGrade(totalScore);

        PerformanceEvaluation evaluation;
        if (existing != null) {
            evaluation = existing;
            scoreRepository.deleteByEvaluationId(existing.getId());
        } else {
            evaluation = PerformanceEvaluation.builder()
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

        // 保存评分明细
        for (var scoreItem : command.getScores()) {
            PerformanceScore score = PerformanceScore.builder()
                    .evaluationId(evaluation.getId())
                    .indicatorId(scoreItem.getIndicatorId())
                    .score(scoreItem.getScore())
                    .comment(scoreItem.getComment())
                    .build();
            scoreRepository.save(score);
        }

        log.info("提交绩效评价: taskId={}, employeeId={}, type={}", 
                command.getTaskId(), command.getEmployeeId(), command.getEvaluationType());
        return toEvaluationDTO(evaluation);
    }

    public List<PerformanceEvaluationDTO> getEmployeeEvaluations(Long taskId, Long employeeId) {
        return evaluationRepository.findByTaskAndEmployee(taskId, employeeId).stream()
                .map(this::toEvaluationDTO)
                .collect(Collectors.toList());
    }

    public List<PerformanceEvaluationDTO> getMyPendingEvaluations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return evaluationRepository.findPendingByEvaluator(userId).stream()
                .map(this::toEvaluationDTO)
                .collect(Collectors.toList());
    }

    public PerformanceEvaluationDTO getEvaluationDetail(Long id) {
        PerformanceEvaluation evaluation = evaluationRepository.getById(id);
        if (evaluation == null) {
            throw new BusinessException("评价记录不存在");
        }
        PerformanceEvaluationDTO dto = toEvaluationDTO(evaluation);
        dto.setScores(scoreRepository.findByEvaluationId(id).stream()
                .map(this::toScoreDTO)
                .collect(Collectors.toList()));
        return dto;
    }

    public Map<String, Object> getTaskStatistics(Long taskId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("gradeDistribution", evaluationRepository.countByGrade(taskId));
        return stats;
    }


    // ==================== 辅助方法 ====================

    private String calculateGrade(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        return "E";
    }

    private PerformanceTaskDTO toTaskDTO(PerformanceTask task) {
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

    private PerformanceIndicatorDTO toIndicatorDTO(PerformanceIndicator indicator) {
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

    private PerformanceEvaluationDTO toEvaluationDTO(PerformanceEvaluation evaluation) {
        PerformanceEvaluationDTO dto = PerformanceEvaluationDTO.builder()
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

        if (evaluation.getEmployeeId() != null) {
            User employee = userRepository.getById(evaluation.getEmployeeId());
            if (employee != null) dto.setEmployeeName(employee.getRealName());
        }
        if (evaluation.getEvaluatorId() != null) {
            User evaluator = userRepository.getById(evaluation.getEvaluatorId());
            if (evaluator != null) dto.setEvaluatorName(evaluator.getRealName());
        }
        if (evaluation.getTaskId() != null) {
            PerformanceTask task = taskRepository.getById(evaluation.getTaskId());
            if (task != null) dto.setTaskName(task.getName());
        }

        return dto;
    }

    private PerformanceScoreDTO toScoreDTO(PerformanceScore score) {
        PerformanceScoreDTO dto = PerformanceScoreDTO.builder()
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

    private String getPeriodTypeName(String type) {
        return switch (type) {
            case "MONTHLY" -> "月度";
            case "QUARTERLY" -> "季度";
            case "YEARLY" -> "年度";
            default -> type;
        };
    }

    private String getTaskStatusName(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getIndicatorCategoryName(String category) {
        return switch (category) {
            case "WORK" -> "工作业绩";
            case "ABILITY" -> "能力素质";
            case "ATTITUDE" -> "工作态度";
            default -> "其他";
        };
    }

    private String getApplicableRoleName(String role) {
        return switch (role) {
            case "ALL" -> "全部";
            case "LAWYER" -> "律师";
            case "ASSISTANT" -> "助理";
            case "ADMIN" -> "行政";
            default -> role;
        };
    }

    private String getEvaluationTypeName(String type) {
        return switch (type) {
            case "SELF" -> "自评";
            case "PEER" -> "互评";
            case "SUPERVISOR" -> "上级评价";
            default -> type;
        };
    }

    private String getGradeName(String grade) {
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
