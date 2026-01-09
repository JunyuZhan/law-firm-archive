package com.lawfirm.application.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.hr.command.CreateCareerLevelCommand;
import com.lawfirm.application.hr.dto.CareerLevelDTO;
import com.lawfirm.application.hr.dto.CareerLevelQueryDTO;
import com.lawfirm.application.hr.dto.PromotionApplicationDTO;
import com.lawfirm.application.hr.dto.PromotionQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.hr.entity.CareerLevel;
import com.lawfirm.domain.hr.repository.CareerLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 晋升管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionAppService {

    private final CareerLevelRepository careerLevelRepository;

    // 类别映射
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
    static {
        CATEGORY_MAP.put("LAWYER", "律师通道");
        CATEGORY_MAP.put("ADMIN", "行政通道");
        CATEGORY_MAP.put("TECH", "技术通道");
    }

    // 状态映射
    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put("ACTIVE", "启用");
        STATUS_MAP.put("INACTIVE", "停用");
    }

    // ========== 职级管理 ==========

    /**
     * 分页查询职级列表
     */
    public PageResult<CareerLevelDTO> listCareerLevels(CareerLevelQueryDTO query) {
        Page<CareerLevel> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<CareerLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CareerLevel::getDeleted, false);
        
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(CareerLevel::getLevelCode, query.getKeyword())
                    .or().like(CareerLevel::getLevelName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(CareerLevel::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareerLevel::getStatus, query.getStatus());
        }
        
        wrapper.orderByAsc(CareerLevel::getCategory, CareerLevel::getLevelOrder);
        
        Page<CareerLevel> result = careerLevelRepository.page(page, wrapper);
        
        List<CareerLevelDTO> dtoList = result.getRecords().stream()
                .map(this::toCareerLevelDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(dtoList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取职级详情
     */
    public CareerLevelDTO getCareerLevelById(Long id) {
        CareerLevel level = careerLevelRepository.getById(id);
        return level != null ? toCareerLevelDTO(level) : null;
    }

    /**
     * 按类别获取职级列表
     */
    public List<CareerLevelDTO> getCareerLevelsByCategory(String category) {
        List<CareerLevel> levels = careerLevelRepository.findByCategory(category);
        return levels.stream()
                .map(this::toCareerLevelDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建职级
     */
    @Transactional
    public CareerLevelDTO createCareerLevel(CreateCareerLevelCommand command) {
        CareerLevel level = CareerLevel.builder()
                .levelCode(command.getLevelCode())
                .levelName(command.getLevelName())
                .levelOrder(command.getLevelOrder())
                .category(command.getCategory())
                .description(command.getDescription())
                .minWorkYears(command.getMinWorkYears())
                .minMatterCount(command.getMinMatterCount())
                .minRevenue(command.getMinRevenue())
                .requiredCertificates(command.getRequiredCertificates() != null ? 
                        String.join(",", command.getRequiredCertificates()) : null)
                .otherRequirements(command.getOtherRequirements())
                .salaryMin(command.getSalaryMin())
                .salaryMax(command.getSalaryMax())
                .status("ACTIVE")
                .build();
        
        careerLevelRepository.save(level);
        log.info("创建职级: id={}, code={}", level.getId(), level.getLevelCode());
        return toCareerLevelDTO(level);
    }

    /**
     * 更新职级
     */
    @Transactional
    public CareerLevelDTO updateCareerLevel(Long id, CreateCareerLevelCommand command) {
        CareerLevel level = careerLevelRepository.getById(id);
        if (level == null) {
            throw new RuntimeException("职级不存在");
        }
        
        level.setLevelCode(command.getLevelCode());
        level.setLevelName(command.getLevelName());
        level.setLevelOrder(command.getLevelOrder());
        level.setCategory(command.getCategory());
        level.setDescription(command.getDescription());
        level.setMinWorkYears(command.getMinWorkYears());
        level.setMinMatterCount(command.getMinMatterCount());
        level.setMinRevenue(command.getMinRevenue());
        level.setRequiredCertificates(command.getRequiredCertificates() != null ? 
                String.join(",", command.getRequiredCertificates()) : null);
        level.setOtherRequirements(command.getOtherRequirements());
        level.setSalaryMin(command.getSalaryMin());
        level.setSalaryMax(command.getSalaryMax());
        
        careerLevelRepository.updateById(level);
        log.info("更新职级: id={}", id);
        return toCareerLevelDTO(level);
    }

    /**
     * 删除职级
     */
    @Transactional
    public void deleteCareerLevel(Long id) {
        careerLevelRepository.removeById(id);
        log.info("删除职级: id={}", id);
    }

    /**
     * 启用职级
     */
    @Transactional
    public void enableCareerLevel(Long id) {
        CareerLevel level = careerLevelRepository.getById(id);
        if (level != null) {
            level.setStatus("ACTIVE");
            careerLevelRepository.updateById(level);
            log.info("启用职级: id={}", id);
        }
    }

    /**
     * 停用职级
     */
    @Transactional
    public void disableCareerLevel(Long id) {
        CareerLevel level = careerLevelRepository.getById(id);
        if (level != null) {
            level.setStatus("INACTIVE");
            careerLevelRepository.updateById(level);
            log.info("停用职级: id={}", id);
        }
    }

    // ========== 晋升申请 (暂时返回空数据) ==========

    /**
     * 分页查询晋升申请
     */
    public PageResult<PromotionApplicationDTO> listPromotionApplications(PromotionQueryDTO query) {
        // TODO: 实现晋升申请查询
        return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
    }

    /**
     * 统计待审批数量
     */
    public long countPendingApplications() {
        // TODO: 实现待审批数量统计
        return 0;
    }

    // ========== 私有方法 ==========

    private CareerLevelDTO toCareerLevelDTO(CareerLevel level) {
        CareerLevelDTO dto = new CareerLevelDTO();
        dto.setId(level.getId());
        dto.setLevelCode(level.getLevelCode());
        dto.setLevelName(level.getLevelName());
        dto.setLevelOrder(level.getLevelOrder());
        dto.setCategory(level.getCategory());
        dto.setCategoryName(CATEGORY_MAP.getOrDefault(level.getCategory(), level.getCategory()));
        dto.setDescription(level.getDescription());
        dto.setMinWorkYears(level.getMinWorkYears());
        dto.setMinMatterCount(level.getMinMatterCount());
        dto.setMinRevenue(level.getMinRevenue());
        
        if (StringUtils.hasText(level.getRequiredCertificates())) {
            dto.setRequiredCertificates(Arrays.asList(level.getRequiredCertificates().split(",")));
        }
        
        dto.setOtherRequirements(level.getOtherRequirements());
        dto.setSalaryMin(level.getSalaryMin());
        dto.setSalaryMax(level.getSalaryMax());
        
        if (level.getSalaryMin() != null && level.getSalaryMax() != null) {
            dto.setSalaryRange(level.getSalaryMin() + " - " + level.getSalaryMax());
        }
        
        dto.setStatus(level.getStatus());
        dto.setStatusName(STATUS_MAP.getOrDefault(level.getStatus(), level.getStatus()));
        dto.setCreatedAt(level.getCreatedAt());
        dto.setUpdatedAt(level.getUpdatedAt());
        
        return dto;
    }
}

