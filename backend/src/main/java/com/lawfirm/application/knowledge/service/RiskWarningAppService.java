package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateRiskWarningCommand;
import com.lawfirm.application.knowledge.dto.RiskWarningDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.RiskWarning;
import com.lawfirm.domain.knowledge.repository.RiskWarningRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.RiskWarningMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 风险预警应用服务（M10-033）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskWarningAppService {

    private final RiskWarningRepository warningRepository;
    private final RiskWarningMapper warningMapper;
    private final MatterRepository matterRepository;
    private final UserRepository userRepository;

    /**
     * 创建风险预警
     */
    @Transactional
    public RiskWarningDTO createWarning(CreateRiskWarningCommand command) {
        matterRepository.getByIdOrThrow(command.getMatterId(), "项目不存在");

        RiskWarning warning = RiskWarning.builder()
                .warningNo(generateWarningNo())
                .matterId(command.getMatterId())
                .riskType(command.getRiskType())
                .riskLevel(command.getRiskLevel())
                .riskDescription(command.getRiskDescription())
                .warningReason(command.getWarningReason())
                .suggestedAction(command.getSuggestedAction())
                .status(RiskWarning.STATUS_ACTIVE)
                .build();

        warningRepository.save(warning);
        log.info("创建风险预警: warningNo={}, matterId={}", warning.getWarningNo(), command.getMatterId());
        return toDTO(warning);
    }

    /**
     * 确认预警
     */
    @Transactional
    public RiskWarningDTO acknowledgeWarning(Long id) {
        RiskWarning warning = warningRepository.getByIdOrThrow(id, "风险预警不存在");
        Long userId = SecurityUtils.getUserId();

        warning.setStatus(RiskWarning.STATUS_ACKNOWLEDGED);
        warning.setAcknowledgedAt(LocalDateTime.now());
        warning.setAcknowledgedBy(userId);

        warningRepository.updateById(warning);
        log.info("确认风险预警: id={}", id);
        return toDTO(warning);
    }

    /**
     * 解决预警
     */
    @Transactional
    public RiskWarningDTO resolveWarning(Long id) {
        RiskWarning warning = warningRepository.getByIdOrThrow(id, "风险预警不存在");
        Long userId = SecurityUtils.getUserId();

        warning.setStatus(RiskWarning.STATUS_RESOLVED);
        warning.setResolvedAt(LocalDateTime.now());
        warning.setResolvedBy(userId);

        warningRepository.updateById(warning);
        log.info("解决风险预警: id={}", id);
        return toDTO(warning);
    }

    /**
     * 关闭预警
     */
    @Transactional
    public RiskWarningDTO closeWarning(Long id) {
        RiskWarning warning = warningRepository.getByIdOrThrow(id, "风险预警不存在");
        warning.setStatus(RiskWarning.STATUS_CLOSED);
        warningRepository.updateById(warning);
        log.info("关闭风险预警: id={}", id);
        return toDTO(warning);
    }

    /**
     * 获取预警详情
     */
    public RiskWarningDTO getWarningById(Long id) {
        RiskWarning warning = warningRepository.getByIdOrThrow(id, "风险预警不存在");
        return toDTO(warning);
    }

    /**
     * 获取项目的所有预警
     */
    public List<RiskWarningDTO> getWarningsByMatterId(Long matterId) {
        List<RiskWarning> warnings = warningMapper.selectByMatterId(matterId);
        return warnings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取活跃的预警
     */
    public List<RiskWarningDTO> getActiveWarnings() {
        List<RiskWarning> warnings = warningMapper.selectActiveWarnings();
        return warnings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取高风险预警
     */
    public List<RiskWarningDTO> getHighRiskWarnings() {
        List<RiskWarning> warnings = warningMapper.selectHighRiskWarnings();
        return warnings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private String generateWarningNo() {
        return "RW-" + System.currentTimeMillis();
    }

    private String getRiskTypeName(String riskType) {
        if (riskType == null) return null;
        return switch (riskType) {
            case RiskWarning.TYPE_DEADLINE -> "期限风险";
            case RiskWarning.TYPE_QUALITY -> "质量风险";
            case RiskWarning.TYPE_COST -> "成本风险";
            case RiskWarning.TYPE_LEGAL -> "法律风险";
            case RiskWarning.TYPE_OTHER -> "其他风险";
            default -> riskType;
        };
    }

    private String getRiskLevelName(String riskLevel) {
        if (riskLevel == null) return null;
        return switch (riskLevel) {
            case RiskWarning.LEVEL_HIGH -> "高";
            case RiskWarning.LEVEL_MEDIUM -> "中";
            case RiskWarning.LEVEL_LOW -> "低";
            default -> riskLevel;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case RiskWarning.STATUS_ACTIVE -> "活跃";
            case RiskWarning.STATUS_ACKNOWLEDGED -> "已确认";
            case RiskWarning.STATUS_RESOLVED -> "已解决";
            case RiskWarning.STATUS_CLOSED -> "已关闭";
            default -> status;
        };
    }

    private RiskWarningDTO toDTO(RiskWarning warning) {
        RiskWarningDTO dto = new RiskWarningDTO();
        dto.setId(warning.getId());
        dto.setWarningNo(warning.getWarningNo());
        dto.setMatterId(warning.getMatterId());
        dto.setRiskType(warning.getRiskType());
        dto.setRiskTypeName(getRiskTypeName(warning.getRiskType()));
        dto.setRiskLevel(warning.getRiskLevel());
        dto.setRiskLevelName(getRiskLevelName(warning.getRiskLevel()));
        dto.setRiskDescription(warning.getRiskDescription());
        dto.setWarningReason(warning.getWarningReason());
        dto.setSuggestedAction(warning.getSuggestedAction());
        dto.setStatus(warning.getStatus());
        dto.setStatusName(getStatusName(warning.getStatus()));
        dto.setAcknowledgedAt(warning.getAcknowledgedAt());
        dto.setAcknowledgedBy(warning.getAcknowledgedBy());
        dto.setResolvedAt(warning.getResolvedAt());
        dto.setResolvedBy(warning.getResolvedBy());
        dto.setCreatedAt(warning.getCreatedAt());
        dto.setUpdatedAt(warning.getUpdatedAt());

        // 获取项目信息
        Matter matter = matterRepository.getById(warning.getMatterId());
        if (matter != null) {
            dto.setMatterName(matter.getName());
        }

        // 获取确认人信息
        if (warning.getAcknowledgedBy() != null) {
            User user = userRepository.getById(warning.getAcknowledgedBy());
            if (user != null) {
                dto.setAcknowledgedByName(user.getRealName());
            }
        }

        // 获取解决人信息
        if (warning.getResolvedBy() != null) {
            User user = userRepository.getById(warning.getResolvedBy());
            if (user != null) {
                dto.setResolvedByName(user.getRealName());
            }
        }

        return dto;
    }
}

