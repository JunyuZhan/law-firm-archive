package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateQualityCheckCommand;
import com.lawfirm.application.knowledge.dto.QualityCheckDTO;
import com.lawfirm.application.knowledge.dto.QualityCheckDetailDTO;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.QualityCheck;
import com.lawfirm.domain.knowledge.entity.QualityCheckDetail;
import com.lawfirm.domain.knowledge.repository.QualityCheckDetailRepository;
import com.lawfirm.domain.knowledge.repository.QualityCheckRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckDetailMapper;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 质量检查应用服务（M10-031） */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityCheckAppService {

  /** 质量检查仓储 */
  private final QualityCheckRepository checkRepository;

  /** 质量检查Mapper */
  private final QualityCheckMapper checkMapper;

  /** 质量检查明细仓储 */
  private final QualityCheckDetailRepository detailRepository;

  /** 质量检查明细Mapper */
  private final QualityCheckDetailMapper detailMapper;

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 质量检查合格阈值（百分比） 可通过配置文件 quality.check.pass-threshold 设置，默认80% */
  @Value("${quality.check.pass-threshold:80}")
  private int passThreshold;

  /** 是否要求所有项目都通过才算合格 可通过配置文件 quality.check.require-all-pass 设置，默认true */
  @Value("${quality.check.require-all-pass:true}")
  private boolean requireAllPass;

  /**
   * 创建质量检查
   *
   * @param command 创建命令
   * @return 质量检查DTO
   */
  @Transactional
  public QualityCheckDTO createCheck(final CreateQualityCheckCommand command) {
    matterRepository.getByIdOrThrow(command.getMatterId(), "项目不存在");
    Long checkerId = SecurityUtils.getUserId();

    QualityCheck check =
        QualityCheck.builder()
            .checkNo(generateCheckNo())
            .matterId(command.getMatterId())
            .checkerId(checkerId)
            .checkDate(command.getCheckDate())
            .checkType(command.getCheckType())
            .status(QualityCheck.STATUS_IN_PROGRESS)
            .build();

    checkRepository.save(check);

    // 创建检查明细
    if (command.getDetails() != null && !command.getDetails().isEmpty()) {
      BigDecimal totalScore = BigDecimal.ZERO;
      BigDecimal maxScore = BigDecimal.ZERO;
      boolean allPass = true;

      for (CreateQualityCheckCommand.CheckDetailCommand detailCmd : command.getDetails()) {
        QualityCheckDetail detail =
            QualityCheckDetail.builder()
                .checkId(check.getId())
                .standardId(detailCmd.getStandardId())
                .checkResult(detailCmd.getCheckResult())
                .score(detailCmd.getScore())
                .maxScore(detailCmd.getMaxScore())
                .findings(detailCmd.getFindings())
                .suggestions(detailCmd.getSuggestions())
                .build();

        detailRepository.save(detail);

        if (detail.getScore() != null) {
          totalScore = totalScore.add(detail.getScore());
        }
        if (detail.getMaxScore() != null) {
          maxScore = maxScore.add(detail.getMaxScore());
        }
        if (!QualityCheckDetail.RESULT_PASS.equals(detail.getCheckResult())) {
          allPass = false;
        }
      }

      check.setTotalScore(totalScore);

      // 计算是否合格（使用可配置的阈值）
      boolean scoreQualified = false;
      if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal thresholdScore =
            maxScore
                .multiply(BigDecimal.valueOf(passThreshold))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        scoreQualified = totalScore.compareTo(thresholdScore) >= 0;
      }

      // 根据配置决定是否要求所有项目都通过
      boolean qualified = requireAllPass ? (allPass && scoreQualified) : scoreQualified;
      check.setQualified(qualified);

      log.debug(
          "质量检查结果: totalScore={}, maxScore={}, threshold={}%, allPass={}, qualified={}",
          totalScore, maxScore, passThreshold, allPass, qualified);

      check.setCheckSummary(command.getCheckSummary());
      check.setStatus(QualityCheck.STATUS_COMPLETED);
      checkRepository.updateById(check);
    }

    log.info("创建质量检查: checkNo={}, matterId={}", check.getCheckNo(), command.getMatterId());
    return toDTO(check);
  }

  /**
   * 获取检查详情
   *
   * @param id 检查ID
   * @return 质量检查DTO
   */
  public QualityCheckDTO getCheckById(final Long id) {
    QualityCheck check = checkRepository.getByIdOrThrow(id, "质量检查不存在");
    QualityCheckDTO dto = toDTO(check);

    // 获取检查明细
    List<QualityCheckDetail> details = detailMapper.selectByCheckId(id);
    dto.setDetails(details.stream().map(this::toDetailDTO).collect(Collectors.toList()));

    return dto;
  }

  /**
   * 获取项目的所有检查
   *
   * @param matterId 项目ID
   * @return 检查列表
   */
  public List<QualityCheckDTO> getChecksByMatterId(final Long matterId) {
    List<QualityCheck> checks = checkMapper.selectByMatterId(matterId);
    return checks.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取进行中的检查
   *
   * @return 检查列表
   */
  public List<QualityCheckDTO> getInProgressChecks() {
    List<QualityCheck> checks = checkMapper.selectInProgress();
    return checks.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 生成检查编号
   *
   * @return 检查编号
   */
  private String generateCheckNo() {
    return "QC-" + System.currentTimeMillis();
  }

  /**
   * 获取检查类型名称.
   *
   * @param checkType 检查类型
   * @return 类型名称
   */
  private String getCheckTypeName(final String checkType) {
    if (checkType == null) {
      return null;
    }
    return switch (checkType) {
      case QualityCheck.TYPE_ROUTINE -> "常规检查";
      case QualityCheck.TYPE_RANDOM -> "随机检查";
      case QualityCheck.TYPE_SPECIAL -> "专项检查";
      default -> checkType;
    };
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case QualityCheck.STATUS_IN_PROGRESS -> "进行中";
      case QualityCheck.STATUS_COMPLETED -> "已完成";
      default -> status;
    };
  }

  /**
   * Entity转DTO.
   *
   * @param check 质量检查实体
   * @return 质量检查DTO
   */
  private QualityCheckDTO toDTO(final QualityCheck check) {
    QualityCheckDTO dto = new QualityCheckDTO();
    dto.setId(check.getId());
    dto.setCheckNo(check.getCheckNo());
    dto.setMatterId(check.getMatterId());
    dto.setCheckerId(check.getCheckerId());
    dto.setCheckDate(check.getCheckDate());
    dto.setCheckType(check.getCheckType());
    dto.setCheckTypeName(getCheckTypeName(check.getCheckType()));
    dto.setStatus(check.getStatus());
    dto.setStatusName(getStatusName(check.getStatus()));
    dto.setTotalScore(check.getTotalScore());
    dto.setQualified(check.getQualified());
    dto.setCheckSummary(check.getCheckSummary());
    dto.setCreatedAt(check.getCreatedAt());
    dto.setUpdatedAt(check.getUpdatedAt());

    // 获取项目信息
    Matter matter = matterRepository.getById(check.getMatterId());
    if (matter != null) {
      dto.setMatterName(matter.getName());
    }

    // 获取检查人信息
    User checker = userRepository.getById(check.getCheckerId());
    if (checker != null) {
      dto.setCheckerName(checker.getRealName());
    }

    return dto;
  }

  /**
   * Entity转DTO.
   *
   * @param detail 质量检查明细实体
   * @return 质量检查明细DTO
   */
  private QualityCheckDetailDTO toDetailDTO(final QualityCheckDetail detail) {
    QualityCheckDetailDTO dto = new QualityCheckDetailDTO();
    dto.setId(detail.getId());
    dto.setCheckId(detail.getCheckId());
    dto.setStandardId(detail.getStandardId());
    dto.setCheckResult(detail.getCheckResult());
    dto.setScore(detail.getScore());
    dto.setMaxScore(detail.getMaxScore());
    dto.setFindings(detail.getFindings());
    dto.setSuggestions(detail.getSuggestions());
    return dto;
  }
}
