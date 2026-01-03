package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.hr.command.CreateCareerLevelCommand;
import com.lawfirm.application.hr.command.CreatePromotionCommand;
import com.lawfirm.application.hr.command.SubmitReviewCommand;
import com.lawfirm.application.hr.dto.CareerLevelDTO;
import com.lawfirm.application.hr.dto.PromotionApplicationDTO;
import com.lawfirm.application.hr.dto.PromotionReviewDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.domain.hr.entity.PromotionApplication;
import com.lawfirm.domain.hr.entity.PromotionReview;
import com.lawfirm.domain.hr.repository.CareerLevelRepository;
import com.lawfirm.domain.hr.repository.PromotionApplicationRepository;
import com.lawfirm.domain.hr.repository.PromotionReviewRepository;
import com.lawfirm.infrastructure.persistence.mapper.CareerLevelMapper;
import com.lawfirm.infrastructure.persistence.mapper.PromotionApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 晋升管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionAppService {

    private final CareerLevelRepository levelRepository;
    private final CareerLevelMapper levelMapper;
    private final PromotionApplicationRepository applicationRepository;
    private final PromotionApplicationMapper applicationMapper;
    private final PromotionReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    // ==================== 职级管理 ====================

    /**
     * 分页查询职级
     */
    public PageResult<CareerLevelDTO> listLevels(int pageNum, int pageSize, 
                                                  String keyword, String category, String status) {
        IPage<CareerLevel> page = levelMapper.selectLevelPage(
                new Page<>(pageNum, pageSize), keyword, category, status);
        return PageResult.of(
                page.getRecords().stream().map(this::toLevelDTO).collect(Collectors.toList()),
                page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取职级详情
     */
    public CareerLevelDTO getLevelById(Long id) {
        CareerLevel level = levelRepository.getByIdOrThrow(id, "职级不存在");
        return toLevelDTO(level);
    }

    /**
     * 按类别获取职级列表
     */
    public List<CareerLevelDTO> getLevelsByCategory(String category) {
        return levelRepository.findByCategory(category).stream()
                .map(this::toLevelDTO).collect(Collectors.toList());
    }

    /**
     * 创建职级
     */
    @Transactional
    public CareerLevelDTO createLevel(CreateCareerLevelCommand command) {
        if (levelRepository.findByLevelCode(command.getLevelCode()).isPresent()) {
            throw new BusinessException("职级编码已存在");
        }

        CareerLevel level = CareerLevel.builder()
                .levelCode(command.getLevelCode())
                .levelName(command.getLevelName())
                .levelOrder(command.getLevelOrder())
                .category(command.getCategory())
                .description(command.getDescription())
                .minWorkYears(command.getMinWorkYears())
                .minMatterCount(command.getMinMatterCount())
                .minRevenue(command.getMinRevenue())
                .requiredCertificates(toJson(command.getRequiredCertificates()))
                .otherRequirements(command.getOtherRequirements())
                .salaryMin(command.getSalaryMin())
                .salaryMax(command.getSalaryMax())
                .status("ACTIVE")
                .build();

        levelRepository.save(level);
        log.info("创建职级: {}", level.getLevelCode());
        return toLevelDTO(level);
    }

    /**
     * 更新职级
     */
    @Transactional
    public CareerLevelDTO updateLevel(Long id, CreateCareerLevelCommand command) {
        CareerLevel level = levelRepository.getByIdOrThrow(id, "职级不存在");

        level.setLevelName(command.getLevelName());
        level.setLevelOrder(command.getLevelOrder());
        level.setDescription(command.getDescription());
        level.setMinWorkYears(command.getMinWorkYears());
        level.setMinMatterCount(command.getMinMatterCount());
        level.setMinRevenue(command.getMinRevenue());
        level.setRequiredCertificates(toJson(command.getRequiredCertificates()));
        level.setOtherRequirements(command.getOtherRequirements());
        level.setSalaryMin(command.getSalaryMin());
        level.setSalaryMax(command.getSalaryMax());

        levelRepository.updateById(level);
        log.info("更新职级: {}", level.getLevelCode());
        return toLevelDTO(level);
    }

    /**
     * 删除职级
     */
    @Transactional
    public void deleteLevel(Long id) {
        CareerLevel level = levelRepository.getByIdOrThrow(id, "职级不存在");
        levelRepository.softDelete(id);
        log.info("删除职级: {}", level.getLevelCode());
    }

    /**
     * 启用/停用职级
     */
    @Transactional
    public void changeLevelStatus(Long id, String status) {
        CareerLevel level = levelRepository.getByIdOrThrow(id, "职级不存在");
        level.setStatus(status);
        levelRepository.updateById(level);
        log.info("修改职级状态: {} -> {}", level.getLevelCode(), status);
    }

    // ==================== 晋升申请 ====================

    /**
     * 分页查询晋升申请
     */
    public PageResult<PromotionApplicationDTO> listApplications(int pageNum, int pageSize,
                                                                 String keyword, String status,
                                                                 Long employeeId, Long departmentId) {
        IPage<PromotionApplication> page = applicationMapper.selectApplicationPage(
                new Page<>(pageNum, pageSize), keyword, status, employeeId, departmentId);
        return PageResult.of(
                page.getRecords().stream().map(this::toApplicationDTO).collect(Collectors.toList()),
                page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取晋升申请详情
     */
    public PromotionApplicationDTO getApplicationById(Long id) {
        PromotionApplication app = applicationRepository.getByIdOrThrow(id, "晋升申请不存在");
        PromotionApplicationDTO dto = toApplicationDTO(app);
        // 加载评审记录
        dto.setReviews(reviewRepository.findByApplicationId(id).stream()
                .map(this::toReviewDTO).collect(Collectors.toList()));
        return dto;
    }

    /**
     * 提交晋升申请
     */
    @Transactional
    public PromotionApplicationDTO submitApplication(CreatePromotionCommand command) {
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getRealName();

        CareerLevel targetLevel = levelRepository.getByIdOrThrow(command.getTargetLevelId(), "目标职级不存在");

        PromotionApplication app = PromotionApplication.builder()
                .applicationNo(generateApplicationNo())
                .employeeId(userId)
                .employeeName(userName)
                .targetLevelId(command.getTargetLevelId())
                .targetLevelName(targetLevel.getLevelName())
                .applyReason(command.getApplyReason())
                .achievements(command.getAchievements())
                .selfEvaluation(command.getSelfEvaluation())
                .attachments(toJson(command.getAttachments()))
                .status("PENDING")
                .applyDate(LocalDate.now())
                .build();

        applicationRepository.save(app);
        log.info("提交晋升申请: {}", app.getApplicationNo());
        return toApplicationDTO(app);
    }

    /**
     * 取消晋升申请
     */
    @Transactional
    public void cancelApplication(Long id) {
        PromotionApplication app = applicationRepository.getByIdOrThrow(id, "晋升申请不存在");
        
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException("只能取消待审批的申请");
        }

        app.setStatus("CANCELLED");
        applicationRepository.updateById(app);
        log.info("取消晋升申请: {}", app.getApplicationNo());
    }

    /**
     * 提交评审
     */
    @Transactional
    public void submitReview(SubmitReviewCommand command) {
        PromotionApplication app = applicationRepository.getByIdOrThrow(command.getApplicationId(), "晋升申请不存在");
        
        if ("APPROVED".equals(app.getStatus()) || "REJECTED".equals(app.getStatus())) {
            throw new BusinessException("该申请已完成审批");
        }

        Long reviewerId = SecurityUtils.getUserId();
        if (reviewRepository.hasReviewed(command.getApplicationId(), reviewerId)) {
            throw new BusinessException("您已提交过评审");
        }

        PromotionReview review = PromotionReview.builder()
                .applicationId(command.getApplicationId())
                .reviewerId(reviewerId)
                .reviewerName(SecurityUtils.getRealName())
                .reviewerRole(command.getReviewerRole())
                .scoreDetails(toJson(command.getScoreDetails()))
                .totalScore(command.getTotalScore())
                .reviewOpinion(command.getReviewOpinion())
                .reviewComment(command.getReviewComment())
                .reviewTime(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        // 更新申请状态为评审中
        if ("PENDING".equals(app.getStatus())) {
            app.setStatus("REVIEWING");
            applicationRepository.updateById(app);
        }

        log.info("提交晋升评审: applicationNo={}, reviewer={}", app.getApplicationNo(), reviewerId);
    }

    /**
     * 最终审批
     */
    @Transactional
    public void approve(Long id, boolean approved, String comment, LocalDate effectiveDate) {
        PromotionApplication app = applicationRepository.getByIdOrThrow(id, "晋升申请不存在");
        
        if ("APPROVED".equals(app.getStatus()) || "REJECTED".equals(app.getStatus())) {
            throw new BusinessException("该申请已完成审批");
        }

        // 计算评审得分
        List<PromotionReview> reviews = reviewRepository.findByApplicationId(id);
        BigDecimal avgScore = reviews.stream()
                .filter(r -> r.getTotalScore() != null)
                .map(PromotionReview::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!reviews.isEmpty()) {
            avgScore = avgScore.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
        }

        app.setReviewScore(avgScore);
        app.setReviewResult(approved ? "PASS" : "FAIL");
        app.setStatus(approved ? "APPROVED" : "REJECTED");
        app.setApprovedBy(SecurityUtils.getUserId());
        app.setApprovedByName(SecurityUtils.getRealName());
        app.setApprovedAt(LocalDateTime.now());
        app.setApprovalComment(comment);
        if (approved && effectiveDate != null) {
            app.setEffectiveDate(effectiveDate);
        }

        applicationRepository.updateById(app);
        log.info("晋升审批完成: applicationNo={}, result={}", app.getApplicationNo(), approved ? "通过" : "拒绝");
    }

    /**
     * 统计待审批数量
     */
    public int countPending() {
        return applicationRepository.countPending();
    }

    // ==================== 私有方法 ====================

    private CareerLevelDTO toLevelDTO(CareerLevel level) {
        CareerLevelDTO dto = new CareerLevelDTO();
        dto.setId(level.getId());
        dto.setLevelCode(level.getLevelCode());
        dto.setLevelName(level.getLevelName());
        dto.setLevelOrder(level.getLevelOrder());
        dto.setCategory(level.getCategory());
        dto.setCategoryName(getCategoryName(level.getCategory()));
        dto.setDescription(level.getDescription());
        dto.setMinWorkYears(level.getMinWorkYears());
        dto.setMinMatterCount(level.getMinMatterCount());
        dto.setMinRevenue(level.getMinRevenue());
        dto.setRequiredCertificates(parseJsonList(level.getRequiredCertificates()));
        dto.setOtherRequirements(level.getOtherRequirements());
        dto.setSalaryMin(level.getSalaryMin());
        dto.setSalaryMax(level.getSalaryMax());
        if (level.getSalaryMin() != null && level.getSalaryMax() != null) {
            dto.setSalaryRange(level.getSalaryMin() + " - " + level.getSalaryMax());
        }
        dto.setStatus(level.getStatus());
        dto.setStatusName("ACTIVE".equals(level.getStatus()) ? "启用" : "停用");
        dto.setCreatedAt(level.getCreatedAt());
        dto.setUpdatedAt(level.getUpdatedAt());
        return dto;
    }

    private PromotionApplicationDTO toApplicationDTO(PromotionApplication app) {
        PromotionApplicationDTO dto = new PromotionApplicationDTO();
        dto.setId(app.getId());
        dto.setApplicationNo(app.getApplicationNo());
        dto.setEmployeeId(app.getEmployeeId());
        dto.setEmployeeName(app.getEmployeeName());
        dto.setDepartmentId(app.getDepartmentId());
        dto.setDepartmentName(app.getDepartmentName());
        dto.setCurrentLevelId(app.getCurrentLevelId());
        dto.setCurrentLevelName(app.getCurrentLevelName());
        dto.setTargetLevelId(app.getTargetLevelId());
        dto.setTargetLevelName(app.getTargetLevelName());
        dto.setApplyReason(app.getApplyReason());
        dto.setAchievements(app.getAchievements());
        dto.setSelfEvaluation(app.getSelfEvaluation());
        dto.setAttachments(parseJsonList(app.getAttachments()));
        dto.setStatus(app.getStatus());
        dto.setStatusName(getStatusName(app.getStatus()));
        dto.setReviewScore(app.getReviewScore());
        dto.setReviewResult(app.getReviewResult());
        dto.setReviewResultName(getReviewResultName(app.getReviewResult()));
        dto.setReviewComment(app.getReviewComment());
        dto.setApprovedBy(app.getApprovedBy());
        dto.setApprovedByName(app.getApprovedByName());
        dto.setApprovedAt(app.getApprovedAt());
        dto.setApprovalComment(app.getApprovalComment());
        dto.setEffectiveDate(app.getEffectiveDate());
        dto.setApplyDate(app.getApplyDate());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }

    private PromotionReviewDTO toReviewDTO(PromotionReview review) {
        PromotionReviewDTO dto = new PromotionReviewDTO();
        dto.setId(review.getId());
        dto.setApplicationId(review.getApplicationId());
        dto.setReviewerId(review.getReviewerId());
        dto.setReviewerName(review.getReviewerName());
        dto.setReviewerRole(review.getReviewerRole());
        dto.setReviewerRoleName(getReviewerRoleName(review.getReviewerRole()));
        dto.setScoreDetails(parseJsonMap(review.getScoreDetails()));
        dto.setTotalScore(review.getTotalScore());
        dto.setReviewOpinion(review.getReviewOpinion());
        dto.setReviewOpinionName(getReviewOpinionName(review.getReviewOpinion()));
        dto.setReviewComment(review.getReviewComment());
        dto.setReviewTime(review.getReviewTime());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    private String generateApplicationNo() {
        return "PROMO" + System.currentTimeMillis();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private String getCategoryName(String category) {
        if (category == null) return null;
        return switch (category) {
            case "LAWYER" -> "律师通道";
            case "ADMIN" -> "行政通道";
            case "TECH" -> "技术通道";
            default -> category;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "REVIEWING" -> "评审中";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getReviewResultName(String result) {
        if (result == null) return null;
        return switch (result) {
            case "PASS" -> "通过";
            case "FAIL" -> "不通过";
            default -> result;
        };
    }

    private String getReviewerRoleName(String role) {
        if (role == null) return null;
        return switch (role) {
            case "DIRECT_MANAGER" -> "直属上级";
            case "HR" -> "人力资源";
            case "PARTNER" -> "合伙人";
            case "COMMITTEE" -> "评审委员会";
            default -> role;
        };
    }

    private String getReviewOpinionName(String opinion) {
        if (opinion == null) return null;
        return switch (opinion) {
            case "APPROVE" -> "同意";
            case "REJECT" -> "不同意";
            case "ABSTAIN" -> "弃权";
            default -> opinion;
        };
    }
}
