package com.lawfirm.application.knowledge.service;

import com.lawfirm.application.knowledge.command.CreateQualityIssueCommand;
import com.lawfirm.application.knowledge.dto.QualityIssueDTO;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.knowledge.entity.QualityIssue;
import com.lawfirm.domain.knowledge.repository.QualityIssueRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.QualityIssueMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问题整改应用服务（M10-032） */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityIssueAppService {

  /** 质量问题仓储 */
  private final QualityIssueRepository issueRepository;

  /** 质量问题Mapper */
  private final QualityIssueMapper issueMapper;

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 创建问题
   *
   * @param command 创建命令
   * @return 问题DTO
   */
  @Transactional
  public QualityIssueDTO createIssue(final CreateQualityIssueCommand command) {
    matterRepository.getByIdOrThrow(command.getMatterId(), "项目不存在");

    QualityIssue issue =
        QualityIssue.builder()
            .issueNo(generateIssueNo())
            .checkId(command.getCheckId())
            .matterId(command.getMatterId())
            .issueType(command.getIssueType())
            .issueDescription(command.getIssueDescription())
            .responsibleUserId(command.getResponsibleUserId())
            .status(QualityIssue.STATUS_OPEN)
            .priority(
                command.getPriority() != null
                    ? command.getPriority()
                    : QualityIssue.PRIORITY_MEDIUM)
            .dueDate(command.getDueDate())
            .build();

    issueRepository.save(issue);
    log.info("创建问题整改: issueNo={}, matterId={}", issue.getIssueNo(), command.getMatterId());
    return toDTO(issue);
  }

  /**
   * 更新问题状态
   *
   * @param id 问题ID
   * @param status 状态
   * @param resolution 解决方案
   * @return 问题DTO
   */
  @Transactional
  public QualityIssueDTO updateIssueStatus(
      final Long id, final String status, final String resolution) {
    QualityIssue issue = issueRepository.getByIdOrThrow(id, "问题不存在");
    Long userId = SecurityUtils.getUserId();

    issue.setStatus(status);
    if (QualityIssue.STATUS_RESOLVED.equals(status)) {
      issue.setResolution(resolution);
      issue.setResolvedAt(LocalDateTime.now());
      issue.setResolvedBy(userId);
    } else if (QualityIssue.STATUS_IN_PROGRESS.equals(status)) {
      // 开始整改，无需额外操作
      // No-op: status already set, no additional action needed
      log.debug("问题状态更新为整改中: issueId={}", id);
    } else if (QualityIssue.STATUS_CLOSED.equals(status)) {
      issue.setVerifiedAt(LocalDateTime.now());
      issue.setVerifiedBy(userId);
    }

    issueRepository.updateById(issue);
    log.info("更新问题状态: id={}, status={}", id, status);
    return toDTO(issue);
  }

  /**
   * 获取问题详情
   *
   * @param id 问题ID
   * @return 问题DTO
   */
  public QualityIssueDTO getIssueById(final Long id) {
    QualityIssue issue = issueRepository.getByIdOrThrow(id, "问题不存在");
    return toDTO(issue);
  }

  /**
   * 获取项目的所有问题
   *
   * @param matterId 项目ID
   * @return 问题列表
   */
  public List<QualityIssueDTO> getIssuesByMatterId(final Long matterId) {
    List<QualityIssue> issues = issueMapper.selectByMatterId(matterId);
    return issues.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取待整改的问题
   *
   * @return 问题列表
   */
  public List<QualityIssueDTO> getPendingIssues() {
    List<QualityIssue> issues = issueMapper.selectPendingIssues();
    return issues.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 生成问题编号.
   *
   * @return 问题编号
   */
  private String generateIssueNo() {
    return "QI-"
        + java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  }

  /**
   * 获取问题类型名称.
   *
   * @param issueType 问题类型代码
   * @return 问题类型名称
   */
  private String getIssueTypeName(final String issueType) {
    if (issueType == null) {
      return null;
    }
    return switch (issueType) {
      case QualityIssue.TYPE_CRITICAL -> "严重";
      case QualityIssue.TYPE_MAJOR -> "重要";
      case QualityIssue.TYPE_MINOR -> "一般";
      default -> issueType;
    };
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case QualityIssue.STATUS_OPEN -> "待整改";
      case QualityIssue.STATUS_IN_PROGRESS -> "整改中";
      case QualityIssue.STATUS_RESOLVED -> "已解决";
      case QualityIssue.STATUS_CLOSED -> "已关闭";
      default -> status;
    };
  }

  /**
   * 获取优先级名称.
   *
   * @param priority 优先级代码
   * @return 优先级名称
   */
  private static String getPriorityName(final String priority) {
    if (priority == null) {
      return null;
    }
    return switch (priority) {
      case QualityIssue.PRIORITY_HIGH -> "高";
      case QualityIssue.PRIORITY_MEDIUM -> "中";
      case QualityIssue.PRIORITY_LOW -> "低";
      default -> priority;
    };
  }

  /**
   * Entity转DTO.
   *
   * @param issue 质量问题实体
   * @return 质量问题DTO
   */
  private QualityIssueDTO toDTO(final QualityIssue issue) {
    QualityIssueDTO dto = new QualityIssueDTO();
    dto.setId(issue.getId());
    dto.setIssueNo(issue.getIssueNo());
    dto.setCheckId(issue.getCheckId());
    dto.setMatterId(issue.getMatterId());
    dto.setIssueType(issue.getIssueType());
    dto.setIssueTypeName(getIssueTypeName(issue.getIssueType()));
    dto.setIssueDescription(issue.getIssueDescription());
    dto.setResponsibleUserId(issue.getResponsibleUserId());
    dto.setStatus(issue.getStatus());
    dto.setStatusName(getStatusName(issue.getStatus()));
    dto.setPriority(issue.getPriority());
    dto.setPriorityName(getPriorityName(issue.getPriority()));
    dto.setDueDate(issue.getDueDate());
    dto.setResolution(issue.getResolution());
    dto.setResolvedAt(issue.getResolvedAt());
    dto.setResolvedBy(issue.getResolvedBy());
    dto.setVerifiedAt(issue.getVerifiedAt());
    dto.setVerifiedBy(issue.getVerifiedBy());
    dto.setCreatedAt(issue.getCreatedAt());
    dto.setUpdatedAt(issue.getUpdatedAt());

    // 获取项目信息
    Matter matter = matterRepository.getById(issue.getMatterId());
    if (matter != null) {
      dto.setMatterName(matter.getName());
    }

    // 获取责任人信息
    if (issue.getResponsibleUserId() != null) {
      User user = userRepository.getById(issue.getResponsibleUserId());
      if (user != null) {
        dto.setResponsibleUserName(user.getRealName());
      }
    }

    return dto;
  }
}
