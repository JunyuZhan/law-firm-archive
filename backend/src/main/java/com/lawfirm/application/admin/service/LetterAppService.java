package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.admin.command.CreateLetterApplicationCommand;
import com.lawfirm.application.admin.dto.LetterApplicationDTO;
import com.lawfirm.application.admin.dto.LetterTemplateDTO;
import com.lawfirm.application.admin.util.LetterTemplateFormatter;
import com.lawfirm.application.matter.dto.MatterClientDTO;
import com.lawfirm.application.system.service.CauseOfActionService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.common.constant.LetterStatus;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.entity.LetterTemplate;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import com.lawfirm.domain.admin.repository.LetterTemplateRepository;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.LetterApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 出函管理应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LetterAppService {

  /** 函件模板仓储 */
  private final LetterTemplateRepository templateRepository;

  /** 函件申请仓储 */
  private final LetterApplicationRepository applicationRepository;

  /** 函件申请Mapper */
  private final LetterApplicationMapper applicationMapper;

  /** 项目仓储 */
  private final MatterRepository matterRepository;

  /** 项目客户关联仓储 */
  private final MatterClientRepository matterClientRepository;

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 客户仓储 */
  private final ClientRepository clientRepository;

  /** 用户Mapper */
  private final UserMapper userMapper;

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /** 系统配置应用服务 */
  private final SysConfigAppService sysConfigAppService;

  /** 审批服务 */
  private final ApprovalService approvalService;

  /** JSON对象映射器 */
  private final ObjectMapper objectMapper;

  /** 案由服务 */
  private final CauseOfActionService causeOfActionService;

  /** 函件模板格式化器 */
  private final LetterTemplateFormatter letterTemplateFormatter;

  /** 项目应用服务 */
  private com.lawfirm.application.matter.service.MatterAppService matterAppService;

  @org.springframework.beans.factory.annotation.Autowired
  @org.springframework.context.annotation.Lazy
  public void setMatterAppService(
      final com.lawfirm.application.matter.service.MatterAppService matterAppService) {
    this.matterAppService = matterAppService;
  }

  // ==================== 模板管理 ====================

  /**
   * 获取启用的模板列表
   *
   * @return 模板列表
   */
  public List<LetterTemplateDTO> listActiveTemplates() {
    LambdaQueryWrapper<LetterTemplate> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(LetterTemplate::getStatus, LetterStatus.TEMPLATE_ACTIVE)
        .orderByAsc(LetterTemplate::getSortOrder);
    return templateRepository.list(wrapper).stream()
        .map(this::toTemplateDTO)
        .collect(Collectors.toList());
  }

  /**
   * 创建模板
   *
   * @param name 模板名称
   * @param letterType 函件类型
   * @param content 模板内容
   * @param description 描述
   * @return 模板DTO
   */
  @Transactional
  public LetterTemplateDTO createTemplate(
      final String name, final String letterType, final String content, final String description) {
    String templateNo =
        "LT"
            + LocalDate.now().toString().replace("-", "").substring(2)
            + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

    LetterTemplate template =
        LetterTemplate.builder()
            .templateNo(templateNo)
            .name(name)
            .letterType(letterType)
            .content(content)
            .description(description)
            .status(LetterStatus.TEMPLATE_ACTIVE)
            .build();
    templateRepository.save(template);
    log.info("创建出函模板: {}", name);
    return toTemplateDTO(template);
  }

  /**
   * 更新模板
   *
   * @param id 模板ID
   * @param name 模板名称
   * @param letterType 函件类型
   * @param content 模板内容
   * @param description 描述
   * @return 模板DTO
   */
  @Transactional
  public LetterTemplateDTO updateTemplate(
      final Long id,
      final String name,
      final String letterType,
      final String content,
      final String description) {
    LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
    if (name != null) {
      template.setName(name);
    }
    if (letterType != null) {
      template.setLetterType(letterType);
    }
    if (content != null) {
      template.setContent(content);
    }
    if (description != null) {
      template.setDescription(description);
    }
    templateRepository.updateById(template);
    log.info("更新出函模板: {}", template.getName());
    return toTemplateDTO(template);
  }

  /**
   * 启用/停用模板
   *
   * @param id 模板ID
   */
  @Transactional
  public void toggleTemplateStatus(final Long id) {
    LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
    template.setStatus(
        LetterStatus.TEMPLATE_ACTIVE.equals(template.getStatus())
            ? LetterStatus.TEMPLATE_DISABLED
            : LetterStatus.TEMPLATE_ACTIVE);
    templateRepository.updateById(template);
    log.info("模板状态变更: {} -> {}", template.getName(), template.getStatus());
  }

  /**
   * 获取所有模板（管理员用）
   *
   * @return 模板列表
   */
  public List<LetterTemplateDTO> listAllTemplates() {
    LambdaQueryWrapper<LetterTemplate> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByAsc(LetterTemplate::getSortOrder);
    return templateRepository.list(wrapper).stream()
        .map(this::toTemplateDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取模板详情
   *
   * @param id 模板ID
   * @return 模板DTO
   */
  public LetterTemplateDTO getTemplateById(final Long id) {
    LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
    return toTemplateDTO(template);
  }

  /**
   * 删除模板（软删除）
   *
   * @param id 模板ID
   */
  @Transactional
  public void deleteTemplate(final Long id) {
    LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
    templateRepository.removeById(id);
    log.info("删除出函模板: {}", template.getName());
  }

  // ==================== 出函申请 ====================

  /**
   * 创建出函申请（律师）
   *
   * @param cmd 创建出函申请命令
   * @return 出函申请DTO
   */
  @Transactional
  public LetterApplicationDTO createApplication(final CreateLetterApplicationCommand cmd) {
    // 验证模板
    LetterTemplate template = templateRepository.getByIdOrThrow(cmd.getTemplateId(), "模板不存在");

    // 验证项目
    Matter matter = matterRepository.getByIdOrThrow(cmd.getMatterId(), "项目不存在");
    if (!MatterConstants.STATUS_ACTIVE.equals(matter.getStatus())) {
      throw new BusinessException("只能为进行中的项目申请出函");
    }

    // 验证用户是否是项目负责人或参与者（只有项目成员才能申请出函）
    matterAppService.validateMatterOwnership(cmd.getMatterId());

    // 生成申请编号：合同编号 + "-" + 序号（第一次-1，第二次-2，以此类推）
    String applicationNo = generateApplicationNo(matter);

    // 获取律师姓名
    String lawyerNames = "";
    String lawyerIds = "";
    if (cmd.getLawyerIds() != null && !cmd.getLawyerIds().isEmpty()) {
      lawyerIds = cmd.getLawyerIds().stream().map(String::valueOf).collect(Collectors.joining(","));
      lawyerNames =
          cmd.getLawyerIds().stream()
              .map(
                  id -> {
                    var user = userMapper.selectById(id);
                    return user != null ? user.getRealName() : "";
                  })
              .collect(Collectors.joining(","));
    }

    // 生成函件内容（替换模板变量）
    String content =
        generateContent(
            template.getContent(),
            matter,
            applicationNo,
            cmd.getTargetUnit(),
            cmd.getTargetAddress(),
            cmd.getLawyerIds(),
            lawyerNames);

    LetterApplication application =
        LetterApplication.builder()
            .applicationNo(applicationNo)
            .templateId(cmd.getTemplateId())
            .matterId(cmd.getMatterId())
            .clientId(matter.getClientId())
            .applicantId(SecurityUtils.getUserId())
            .applicantName(SecurityUtils.getRealName())
            .departmentId(SecurityUtils.getDepartmentId())
            .letterType(template.getLetterType())
            .targetUnit(cmd.getTargetUnit())
            .targetContact(cmd.getTargetContact())
            .targetPhone(cmd.getTargetPhone())
            .targetAddress(cmd.getTargetAddress())
            .purpose(cmd.getPurpose())
            .lawyerIds(lawyerIds)
            .lawyerNames(lawyerNames)
            .content(content)
            .copies(cmd.getCopies() != null ? cmd.getCopies() : 1)
            .expectedDate(cmd.getExpectedDate())
            .status(LetterStatus.PENDING)
            .assignedApproverId(cmd.getApproverId())
            .remark(cmd.getRemark())
            .build();

    applicationRepository.save(application);
    log.info("创建出函申请: {} -> {}", applicationNo, cmd.getTargetUnit());

    // 如果指定了审批人，创建审批中心的审批记录
    if (cmd.getApproverId() != null) {
      try {
        // 构建业务数据快照
        String businessSnapshot = buildBusinessSnapshot(application, matter, template);

        // 创建审批中心记录
        String businessTitle =
            String.format(
                "出函申请-%s-%s",
                matter.getName() != null ? matter.getName() : "",
                cmd.getTargetUnit() != null ? cmd.getTargetUnit() : "");

        Long approvalId =
            approvalService.createApproval(
                "LETTER_APPLICATION",
                application.getId(),
                applicationNo,
                businessTitle,
                cmd.getApproverId(),
                "MEDIUM",
                "NORMAL",
                businessSnapshot);

        // 保存审批记录ID到出函申请，用于后续关联
        application.setApprovalId(approvalId);
        applicationRepository.updateById(application);

        log.info("已创建审批中心记录: approvalId={}, applicationId={}", approvalId, application.getId());
      } catch (Exception e) {
        log.error("创建审批中心记录失败，但出函申请已创建: applicationId={}", application.getId(), e);
        // 降级处理：发送传统通知
        String notifyTitle = "新出函申请";
        String notifyContent =
            String.format("收到新的出函申请 [%s]，申请人：%s，请及时审批", applicationNo, SecurityUtils.getRealName());
        notificationAppService.sendSystemNotification(
            cmd.getApproverId(), notifyTitle, notifyContent, "LETTER", application.getId());
      }
    } else {
      // 未指定审批人时，通知所有行政人员
      String notifyTitle = "新出函申请";
      String notifyContent =
          String.format("收到新的出函申请 [%s]，申请人：%s，请及时审批", applicationNo, SecurityUtils.getRealName());
      notifyAdminStaff(notifyTitle, notifyContent, "LETTER", application.getId());
    }

    return toApplicationDTO(application);
  }

  /**
   * 审批通过
   *
   * @param id 申请ID
   * @param comment 审批意见
   */
  @Transactional
  public void approve(final Long id, final String comment) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    if (!LetterStatus.canApprove(app.getStatus())) {
      throw new BusinessException("只能审批待审批状态的申请");
    }
    app.setStatus(LetterStatus.APPROVED);
    app.setApprovedBy(SecurityUtils.getUserId());
    app.setApprovedAt(LocalDateTime.now());
    app.setApprovalComment(comment);
    applicationRepository.updateById(app);
    log.info("出函申请审批通过: {}", app.getApplicationNo());

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函申请已审批通过",
        String.format("您的出函申请 [%s] 已审批通过，请等待打印", app.getApplicationNo()),
        "LETTER",
        app.getId());
  }

  /**
   * 审批拒绝
   *
   * @param id 申请ID
   * @param comment 拒绝原因
   */
  @Transactional
  public void reject(final Long id, final String comment) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    if (!LetterStatus.canApprove(app.getStatus())) {
      throw new BusinessException("只能审批待审批状态的申请");
    }
    app.setStatus(LetterStatus.REJECTED);
    app.setApprovedBy(SecurityUtils.getUserId());
    app.setApprovedAt(LocalDateTime.now());
    app.setApprovalComment(comment);
    applicationRepository.updateById(app);
    log.info("出函申请被拒绝: {}", app.getApplicationNo());

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函申请被拒绝",
        String.format("您的出函申请 [%s] 被拒绝，原因：%s", app.getApplicationNo(), comment),
        "LETTER",
        app.getId());
  }

  /**
   * 退回修改（审批人退回给申请人修改）
   *
   * @param id 申请ID
   * @param comment 退回原因
   */
  @Transactional
  public void returnForRevision(final Long id, final String comment) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    if (!LetterStatus.canApprove(app.getStatus())) {
      throw new BusinessException("只能退回待审批状态的申请");
    }
    app.setStatus(LetterStatus.RETURNED);
    app.setApprovedBy(SecurityUtils.getUserId());
    app.setApprovedAt(LocalDateTime.now());
    app.setApprovalComment(comment);
    applicationRepository.updateById(app);
    log.info("出函申请被退回修改: {}", app.getApplicationNo());

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函申请被退回修改",
        String.format("您的出函申请 [%s] 被退回，请修改后重新提交。原因：%s", app.getApplicationNo(), comment),
        "LETTER",
        app.getId());
  }

  /**
   * 重新提交（申请人修改后重新提交）
   *
   * @param id 申请ID
   * @param cmd 创建出函申请命令
   * @return 出函申请DTO
   */
  @Transactional
  public LetterApplicationDTO resubmit(final Long id, final CreateLetterApplicationCommand cmd) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 只能重新提交自己的申请
    if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能重新提交自己的申请");
    }

    // 只能重新提交被退回或被拒绝的申请
    if (!LetterStatus.canResubmit(app.getStatus())) {
      throw new BusinessException("只能重新提交被退回或被拒绝的申请");
    }

    // 验证模板
    LetterTemplate template = templateRepository.getByIdOrThrow(cmd.getTemplateId(), "模板不存在");

    // 验证项目
    Matter matter = matterRepository.getByIdOrThrow(cmd.getMatterId(), "项目不存在");
    if (!MatterConstants.STATUS_ACTIVE.equals(matter.getStatus())) {
      throw new BusinessException("只能为进行中的项目申请出函");
    }

    // 获取律师姓名
    String lawyerNames = "";
    String lawyerIds = "";
    if (cmd.getLawyerIds() != null && !cmd.getLawyerIds().isEmpty()) {
      lawyerIds = cmd.getLawyerIds().stream().map(String::valueOf).collect(Collectors.joining(","));
      lawyerNames =
          cmd.getLawyerIds().stream()
              .map(
                  lid -> {
                    var user = userMapper.selectById(lid);
                    return user != null ? user.getRealName() : "";
                  })
              .collect(Collectors.joining(","));
    }

    // 重新生成函件内容
    String content =
        generateContent(
            template.getContent(),
            matter,
            app.getApplicationNo(),
            cmd.getTargetUnit(),
            cmd.getTargetAddress(),
            cmd.getLawyerIds(),
            lawyerNames);

    // 更新申请信息
    app.setTemplateId(cmd.getTemplateId());
    app.setMatterId(cmd.getMatterId());
    app.setClientId(matter.getClientId());
    app.setLetterType(template.getLetterType());
    app.setTargetUnit(cmd.getTargetUnit());
    app.setTargetContact(cmd.getTargetContact());
    app.setTargetPhone(cmd.getTargetPhone());
    app.setTargetAddress(cmd.getTargetAddress());
    app.setPurpose(cmd.getPurpose());
    app.setLawyerIds(lawyerIds);
    app.setLawyerNames(lawyerNames);
    app.setContent(content);
    app.setCopies(cmd.getCopies() != null ? cmd.getCopies() : 1);
    app.setExpectedDate(cmd.getExpectedDate());
    app.setStatus(LetterStatus.PENDING);
    app.setRemark(cmd.getRemark());
    // 清除之前的审批信息
    app.setApprovedBy(null);
    app.setApprovedAt(null);
    app.setApprovalComment(null);

    applicationRepository.updateById(app);
    log.info("出函申请重新提交: {}", app.getApplicationNo());
    return toApplicationDTO(app);
  }

  /**
   * 更新申请（申请人在被退回后修改申请内容，但不提交）
   *
   * @param id 申请ID
   * @param cmd 创建出函申请命令
   * @return 出函申请DTO
   */
  @Transactional
  public LetterApplicationDTO updateApplication(
      final Long id, final CreateLetterApplicationCommand cmd) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 只能修改自己的申请
    if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能修改自己的申请");
    }

    // 只能修改被退回或被拒绝的申请
    if (!LetterStatus.canResubmit(app.getStatus())) {
      throw new BusinessException("只能修改被退回或被拒绝的申请");
    }

    // 验证模板
    LetterTemplate template = templateRepository.getByIdOrThrow(cmd.getTemplateId(), "模板不存在");

    // 验证项目
    Matter matter = matterRepository.getByIdOrThrow(cmd.getMatterId(), "项目不存在");

    // 获取律师姓名
    String lawyerNames = "";
    String lawyerIds = "";
    if (cmd.getLawyerIds() != null && !cmd.getLawyerIds().isEmpty()) {
      lawyerIds = cmd.getLawyerIds().stream().map(String::valueOf).collect(Collectors.joining(","));
      lawyerNames =
          cmd.getLawyerIds().stream()
              .map(
                  lid -> {
                    var user = userMapper.selectById(lid);
                    return user != null ? user.getRealName() : "";
                  })
              .collect(Collectors.joining(","));
    }

    // 重新生成函件内容
    String content =
        generateContent(
            template.getContent(),
            matter,
            app.getApplicationNo(),
            cmd.getTargetUnit(),
            cmd.getTargetAddress(),
            cmd.getLawyerIds(),
            lawyerNames);

    // 更新申请信息（保持状态不变）
    app.setTemplateId(cmd.getTemplateId());
    app.setMatterId(cmd.getMatterId());
    app.setClientId(matter.getClientId());
    app.setLetterType(template.getLetterType());
    app.setTargetUnit(cmd.getTargetUnit());
    app.setTargetContact(cmd.getTargetContact());
    app.setTargetPhone(cmd.getTargetPhone());
    app.setTargetAddress(cmd.getTargetAddress());
    app.setPurpose(cmd.getPurpose());
    app.setLawyerIds(lawyerIds);
    app.setLawyerNames(lawyerNames);
    app.setContent(content);
    app.setCopies(cmd.getCopies() != null ? cmd.getCopies() : 1);
    app.setExpectedDate(cmd.getExpectedDate());
    app.setRemark(cmd.getRemark());

    applicationRepository.updateById(app);
    log.info("出函申请已更新: {}", app.getApplicationNo());
    return toApplicationDTO(app);
  }

  /**
   * 重新提交审批（仅改变状态为待审批）
   *
   * @param id 申请ID
   */
  @Transactional
  public void submitForApproval(final Long id) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 只能提交自己的申请
    if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能提交自己的申请");
    }

    // 只能提交被退回或被拒绝的申请
    if (!LetterStatus.canResubmit(app.getStatus())) {
      throw new BusinessException("只能提交被退回或被拒绝的申请");
    }

    app.setStatus(LetterStatus.PENDING);
    // 清除之前的审批信息
    app.setApprovedBy(null);
    app.setApprovedAt(null);
    app.setApprovalComment(null);

    applicationRepository.updateById(app);
    log.info("出函申请重新提交审批: {}", app.getApplicationNo());
  }

  /**
   * 获取全部申请列表（行政管理用）
   *
   * @param applicationNo 申请编号
   * @param matterName 项目名称
   * @param status 状态
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 申请列表
   */
  public List<LetterApplicationDTO> listAllApplications(
      final String applicationNo,
      final String matterName,
      final String status,
      final String startDate,
      final String endDate) {
    return applicationMapper
        .selectAllApplications(applicationNo, matterName, status, startDate, endDate)
        .stream()
        .map(this::toApplicationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取待打印列表（行政用）
   *
   * @return 待打印申请列表
   */
  public List<LetterApplicationDTO> listPendingPrint() {
    return applicationMapper.selectPendingPrint().stream()
        .map(this::toApplicationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 确认打印（行政）
   *
   * @param id 申请ID
   */
  @Transactional
  public void confirmPrint(final Long id) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    if (!LetterStatus.APPROVED.equals(app.getStatus())) {
      throw new BusinessException("只能打印已审批通过的申请");
    }
    app.setStatus(LetterStatus.PRINTED);
    app.setPrintedBy(SecurityUtils.getUserId());
    app.setPrintedAt(LocalDateTime.now());
    applicationRepository.updateById(app);
    log.info("出函已打印: {}", app.getApplicationNo());

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函已打印",
        String.format("您的出函申请 [%s] 已打印完成，请前往行政部领取", app.getApplicationNo()),
        "LETTER",
        app.getId());
  }

  /**
   * 确认领取
   *
   * @param id 申请ID
   */
  @Transactional
  public void confirmReceive(final Long id) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    if (!"PRINTED".equals(app.getStatus())) {
      throw new BusinessException("只能领取已打印的函件");
    }
    app.setStatus("RECEIVED");
    app.setReceivedBy(SecurityUtils.getUserId());
    app.setReceivedAt(LocalDateTime.now());
    applicationRepository.updateById(app);
    log.info("出函已领取: {}", app.getApplicationNo());
  }

  /**
   * 查询项目的出函记录
   *
   * @param matterId 项目ID
   * @return 出函申请列表
   */
  public List<LetterApplicationDTO> listByMatter(final Long matterId) {
    return applicationMapper.selectByMatterId(matterId).stream()
        .map(this::toApplicationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 获取申请详情
   *
   * @param id 申请ID
   * @return 出函申请DTO
   */
  public LetterApplicationDTO getById(final Long id) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
    return toApplicationDTO(app);
  }

  /**
   * 我的申请列表（律师用）
   *
   * @return 申请列表
   */
  public List<LetterApplicationDTO> listMyApplications() {
    Long userId = SecurityUtils.getUserId();
    LambdaQueryWrapper<LetterApplication> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(LetterApplication::getApplicantId, userId)
        .orderByDesc(LetterApplication::getCreatedAt);
    return applicationRepository.list(wrapper).stream()
        .map(this::toApplicationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 取消申请（律师）
   *
   * @param id 申请ID
   */
  @Transactional
  public void cancelApplication(final Long id) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 只能取消自己的申请
    if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
      throw new BusinessException("只能取消自己的申请");
    }

    // 只能取消待审批状态的申请
    if (!LetterStatus.canApprove(app.getStatus())) {
      throw new BusinessException("只能取消待审批状态的申请");
    }

    app.setStatus(LetterStatus.CANCELLED);
    applicationRepository.updateById(app);
    log.info("出函申请已取消: {}", app.getApplicationNo());
  }

  /**
   * 待审批列表（审批人用）
   *
   * @return 待审批申请列表
   */
  public List<LetterApplicationDTO> listPendingApproval() {
    LambdaQueryWrapper<LetterApplication> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(LetterApplication::getStatus, LetterStatus.PENDING)
        .orderByAsc(LetterApplication::getCreatedAt);
    return applicationRepository.list(wrapper).stream()
        .map(this::toApplicationDTO)
        .collect(Collectors.toList());
  }

  /**
   * 更新函件内容（行政人员用，可以编辑任何申请的函件内容）
   *
   * @param id 申请ID
   * @param content 函件内容
   * @return 出函申请DTO
   */
  @Transactional
  public LetterApplicationDTO updateContent(final Long id, final String content) {
    LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");

    // 行政人员可以编辑任何状态的函件内容（通常在审批通过后、打印前进行格式调整）
    app.setContent(content);
    applicationRepository.updateById(app);
    log.info("行政人员更新函件内容: {}", app.getApplicationNo());
    return toApplicationDTO(app);
  }

  // ==================== 审批中心回调方法 ====================

  /**
   * 审批通过回调（供审批中心事件监听器调用） 此方法不做权限检查，由审批中心保证权限
   *
   * @param applicationId 申请ID
   * @param approverId 审批人ID（从审批中心传入）
   * @param comment 审批意见
   */
  @Transactional
  public void onApprovalApproved(
      final Long applicationId, final Long approverId, final String comment) {
    LetterApplication app = applicationRepository.findById(applicationId);
    if (app == null) {
      log.warn("审批中心回调：出函申请不存在, id={}", applicationId);
      return;
    }
    if (!LetterStatus.canApprove(app.getStatus())) {
      log.warn("审批中心回调：出函申请状态不是待审批, id={}, status={}", applicationId, app.getStatus());
      return;
    }
    app.setStatus(LetterStatus.APPROVED);
    app.setApprovedBy(approverId); // 设置审批人ID
    app.setApprovedAt(LocalDateTime.now());
    app.setApprovalComment(comment);
    applicationRepository.updateById(app);
    log.info("审批中心回调-出函申请审批通过: {}, 审批人: {}", app.getApplicationNo(), approverId);

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函申请已审批通过",
        String.format("您的出函申请 [%s] 已审批通过，请等待打印", app.getApplicationNo()),
        "LETTER",
        app.getId());
  }

  /**
   * 审批通过回调（兼容旧版本，从当前用户获取审批人）
   *
   * @param applicationId 申请ID
   * @param comment 审批意见
   */
  @Transactional
  public void onApprovalApproved(final Long applicationId, final String comment) {
    onApprovalApproved(applicationId, SecurityUtils.getUserId(), comment);
  }

  /**
   * 审批拒绝回调（供审批中心事件监听器调用） 此方法不做权限检查，由审批中心保证权限
   *
   * @param applicationId 申请ID
   * @param approverId 审批人ID（从审批中心传入）
   * @param comment 审批意见
   */
  @Transactional
  public void onApprovalRejected(
      final Long applicationId, final Long approverId, final String comment) {
    LetterApplication app = applicationRepository.findById(applicationId);
    if (app == null) {
      log.warn("审批中心回调：出函申请不存在, id={}", applicationId);
      return;
    }
    if (!LetterStatus.canApprove(app.getStatus())) {
      log.warn("审批中心回调：出函申请状态不是待审批, id={}, status={}", applicationId, app.getStatus());
      return;
    }
    app.setStatus(LetterStatus.REJECTED);
    app.setApprovedBy(approverId); // 设置审批人ID
    app.setApprovedAt(LocalDateTime.now());
    app.setApprovalComment(comment);
    applicationRepository.updateById(app);
    log.info("审批中心回调-出函申请被拒绝: {}, 审批人: {}", app.getApplicationNo(), approverId);

    // 发送通知给申请人
    notificationAppService.sendSystemNotification(
        app.getApplicantId(),
        "出函申请被拒绝",
        String.format("您的出函申请 [%s] 被拒绝，原因：%s", app.getApplicationNo(), comment),
        "LETTER",
        app.getId());
  }

  /**
   * 审批拒绝回调（兼容旧版本，从当前用户获取审批人）
   *
   * @param applicationId 申请ID
   * @param comment 拒绝原因
   */
  @Transactional
  public void onApprovalRejected(final Long applicationId, final String comment) {
    onApprovalRejected(applicationId, SecurityUtils.getUserId(), comment);
  }

  // ==================== 私有方法 ====================

  /**
   * 构建业务数据快照（JSON格式）
   *
   * @param application 出函申请
   * @param matter 项目
   * @param template 模板
   * @return 业务数据快照JSON字符串
   */
  private String buildBusinessSnapshot(
      final LetterApplication application, final Matter matter, final LetterTemplate template) {
    try {
      java.util.Map<String, Object> snapshot = new java.util.HashMap<>();
      snapshot.put("applicationNo", application.getApplicationNo());
      snapshot.put("letterType", application.getLetterType());
      snapshot.put("letterTypeName", getLetterTypeName(application.getLetterType()));
      snapshot.put("targetUnit", application.getTargetUnit());
      snapshot.put("targetContact", application.getTargetContact());
      snapshot.put("targetAddress", application.getTargetAddress());
      snapshot.put("purpose", application.getPurpose());
      snapshot.put("lawyerNames", application.getLawyerNames());
      snapshot.put("copies", application.getCopies());
      snapshot.put(
          "expectedDate",
          application.getExpectedDate() != null ? application.getExpectedDate().toString() : null);
      snapshot.put("applicantName", application.getApplicantName());

      // 项目信息
      if (matter != null) {
        snapshot.put("matterName", matter.getName());
        snapshot.put("matterNo", matter.getMatterNo());
      }

      // 模板信息
      if (template != null) {
        snapshot.put("templateName", template.getName());
      }

      return objectMapper.writeValueAsString(snapshot);
    } catch (Exception e) {
      log.warn("构建业务数据快照失败", e);
      return "{}";
    }
  }

  /**
   * 生成出函申请编号（并发安全） 格式：合同编号 + "-" + 序号（第一次-1，第二次-2，以此类推） 如果没有合同，则使用项目编号
   *
   * <p>并发安全策略： 1. 使用 MAX(序号) + 1 而非 COUNT(*) + 1，避免删除记录导致的编号冲突 2. 数据库需添加唯一约束：ALTER TABLE
   * letter_application ADD UNIQUE INDEX uk_application_no (application_no); 3. 调用方在遇到唯一约束冲突时应重试
   *
   * @param matter 项目信息
   * @return 申请编号
   */
  private String generateApplicationNo(final Matter matter) {
    // 1. 获取合同编号（优先）或项目编号
    String baseNo = getBaseNoForMatter(matter);

    // 2. 使用 synchronized 和数据库查询最大序号，确保并发安全
    // 查询该项目已有的出函申请的最大序号
    int nextSequence;
    synchronized (this) {
      Integer maxSequence = applicationMapper.selectMaxSequenceByMatterId(matter.getId());
      nextSequence = (maxSequence != null ? maxSequence : 0) + 1;
    }

    // 3. 生成编号：合同编号 + "-" + 序号
    String applicationNo = baseNo + "-" + nextSequence;

    log.debug("生成出函申请编号: matterId={}, applicationNo={}", matter.getId(), applicationNo);
    return applicationNo;
  }

  /**
   * 获取项目的基础编号（合同号或项目号）
   *
   * @param matter 项目
   * @return 基础编号
   */
  private String getBaseNoForMatter(final Matter matter) {
    if (matter.getContractId() != null) {
      try {
        Contract contract = contractRepository.getById(matter.getContractId());
        if (contract != null
            && contract.getContractNo() != null
            && !contract.getContractNo().isEmpty()) {
          return contract.getContractNo();
        }
      } catch (Exception e) {
        log.warn(
            "获取合同编号失败，使用项目编号: matterId={}, contractId={}",
            matter.getId(),
            matter.getContractId(),
            e);
      }
    }
    // 没有合同或获取失败，使用项目编号
    return matter.getMatterNo() != null && !matter.getMatterNo().isEmpty()
        ? matter.getMatterNo()
        : "M" + matter.getId();
  }

  /**
   * 生成函件内容（替换模板变量） 支持模板中定义的所有变量，根据项目实际信息进行替换 支持结构化模板格式和传统HTML格式
   *
   * @param template 模板内容
   * @param matter 项目
   * @param applicationNo 申请编号
   * @param targetUnit 目标单位
   * @param targetAddress 目标地址
   * @param lawyerIds 律师ID列表
   * @param lawyerNames 律师姓名
   * @return 生成的函件内容
   */
  private String generateContent(
      final String template,
      final Matter matter,
      final String applicationNo,
      final String targetUnit,
      final String targetAddress,
      final List<Long> lawyerIds,
      final String lawyerNames) {
    if (template == null || template.isEmpty()) {
      return "";
    }

    // 先收集所有变量值
    Map<String, String> variables =
        collectVariables(matter, applicationNo, targetUnit, targetAddress, lawyerIds, lawyerNames);

    // 检查是否为结构化格式
    if (letterTemplateFormatter.isStructuredFormat(template)) {
      // 结构化格式：先格式化再替换变量（格式化过程中会替换变量）
      return letterTemplateFormatter.formatStructuredLetter(template, variables);
    }

    // 传统格式：直接替换变量
    String result = template;

    // ========== 项目信息 ==========
    result = result.replace("${matterName}", matter.getName() != null ? matter.getName() : "");
    result =
        result.replace("${matterNo}", matter.getMatterNo() != null ? matter.getMatterNo() : "");
    // 案由：使用案由名称（根据案件类型智能查询）
    String causeOfActionName = "";
    if (matter.getCauseOfAction() != null && !matter.getCauseOfAction().isEmpty()) {
      String causeType =
          switch (matter.getCaseType() != null ? matter.getCaseType() : "") {
            case "CRIMINAL" -> CauseOfActionService.TYPE_CRIMINAL;
            case "ADMINISTRATIVE" -> CauseOfActionService.TYPE_ADMIN;
            default -> CauseOfActionService.TYPE_CIVIL;
          };
      causeOfActionName = causeOfActionService.getCauseName(matter.getCauseOfAction(), causeType);
    }
    result = result.replace("${causeOfAction}", causeOfActionName);

    // ========== 合同信息 ==========
    String contractNo = "";
    String trialStage = "";
    if (matter.getContractId() != null) {
      try {
        Contract contract = contractRepository.getById(matter.getContractId());
        if (contract != null) {
          if (contract.getContractNo() != null) {
            contractNo = contract.getContractNo();
          }
          // 获取案件阶段（从合同获取）
          if (contract.getTrialStage() != null && !contract.getTrialStage().isEmpty()) {
            trialStage = getTrialStageName(contract.getTrialStage());
          }
        }
      } catch (Exception e) {
        log.warn(
            "获取项目合同信息失败: matterId={}, contractId={}", matter.getId(), matter.getContractId(), e);
      }
    }
    result = result.replace("${contractNo}", contractNo);
    // 案件阶段变量（支持两个名称：trialStage 和 procedureStage）
    result = result.replace("${trialStage}", trialStage);
    result = result.replace("${procedureStage}", trialStage);

    // ========== 客户信息 ==========
    String clientName = "";
    String clientIdNumber = "";
    if (matter.getClientId() != null) {
      try {
        // 优先从项目的主要客户获取
        var primaryClient = matterClientRepository.findPrimaryClient(matter.getId());
        Long clientId =
            primaryClient
                .map(com.lawfirm.domain.matter.entity.MatterClient::getClientId)
                .orElse(matter.getClientId());

        Client client = clientRepository.getById(clientId);
        if (client != null) {
          clientName = client.getName() != null ? client.getName() : "";
          // 根据客户类型获取身份证号或统一社会信用代码
          if ("INDIVIDUAL".equals(client.getClientType())) {
            clientIdNumber = client.getIdCard() != null ? client.getIdCard() : "";
          } else {
            clientIdNumber = client.getCreditCode() != null ? client.getCreditCode() : "";
          }
        }
      } catch (Exception e) {
        log.warn("获取客户信息失败: matterId={}, clientId={}", matter.getId(), matter.getClientId(), e);
      }
    }
    result = result.replace("${clientName}", clientName);
    result = result.replace("${clientIdNumber}", clientIdNumber);

    // ========== 对方当事人信息 ==========
    String opposingParty = matter.getOpposingParty() != null ? matter.getOpposingParty() : "";
    result = result.replace("${opposingParty}", opposingParty);

    // ========== 律师信息 ==========
    result = result.replace("${lawyerNames}", lawyerNames != null ? lawyerNames : "");

    // 获取律师执业证号（多个律师用逗号分隔）
    String lawyerLicenseNos = "";
    if (lawyerIds != null && !lawyerIds.isEmpty()) {
      lawyerLicenseNos =
          lawyerIds.stream()
              .map(
                  id -> {
                    try {
                      User user = userMapper.selectById(id);
                      return user != null && user.getLawyerLicenseNo() != null
                          ? user.getLawyerLicenseNo()
                          : "";
                    } catch (Exception e) {
                      log.warn("获取律师执业证号失败: lawyerId={}", id, e);
                      return "";
                    }
                  })
              .filter(no -> !no.isEmpty())
              .collect(Collectors.joining(","));
    }
    result = result.replace("${lawyerLicenseNo}", lawyerLicenseNos);

    // ========== 接收单位信息 ==========
    result = result.replace("${targetUnit}", targetUnit != null ? targetUnit : "");
    result = result.replace("${targetAddress}", targetAddress != null ? targetAddress : "");

    // ========== 律所信息（从系统配置获取）==========
    String firmName = "";
    String firmAddress = "";
    String firmPhone = "";
    String firmLicense = "";
    try {
      firmName = sysConfigAppService.getConfigValue("firm.name");
      if (firmName == null) {
        firmName = "";
      }

      firmAddress = sysConfigAppService.getConfigValue("firm.address");
      if (firmAddress == null) {
        firmAddress = "";
      }

      firmPhone = sysConfigAppService.getConfigValue("firm.phone");
      if (firmPhone == null) {
        firmPhone = "";
      }

      firmLicense = sysConfigAppService.getConfigValue("firm.license");
      if (firmLicense == null) {
        firmLicense = "";
      }
    } catch (Exception e) {
      log.warn("获取律所信息失败", e);
    }
    result = result.replace("${firmName}", firmName);
    result = result.replace("${firmAddress}", firmAddress);
    result = result.replace("${firmPhone}", firmPhone);
    result = result.replace("${firmLicense}", firmLicense);

    // ========== 函件信息 ==========
    result = result.replace("${letterNo}", applicationNo != null ? applicationNo : "");

    // ========== 日期信息 ==========
    LocalDate now = LocalDate.now();
    // 格式化为中文日期：YYYY年MM月DD日
    String chineseDate =
        String.format("%d年%02d月%02d日", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    result = result.replace("${date}", chineseDate);
    result = result.replace("${currentYear}", String.valueOf(now.getYear()));

    return result;
  }

  /**
   * 收集所有变量值（用于结构化模板格式化）
   *
   * @param matter 项目
   * @param applicationNo 申请编号
   * @param targetUnit 目标单位
   * @param targetAddress 目标地址
   * @param lawyerIds 律师ID列表
   * @param lawyerNames 律师姓名
   * @return 变量值映射
   */
  private Map<String, String> collectVariables(
      final Matter matter,
      final String applicationNo,
      final String targetUnit,
      final String targetAddress,
      final List<Long> lawyerIds,
      final String lawyerNames) {
    Map<String, String> vars = new HashMap<>();

    // ========== 项目信息 ==========
    vars.put("matterName", matter.getName() != null ? matter.getName() : "");
    vars.put("matterNo", matter.getMatterNo() != null ? matter.getMatterNo() : "");

    // 案由
    String causeOfActionName = "";
    if (matter.getCauseOfAction() != null && !matter.getCauseOfAction().isEmpty()) {
      String causeType =
          switch (matter.getCaseType() != null ? matter.getCaseType() : "") {
            case "CRIMINAL" -> CauseOfActionService.TYPE_CRIMINAL;
            case "ADMINISTRATIVE" -> CauseOfActionService.TYPE_ADMIN;
            default -> CauseOfActionService.TYPE_CIVIL;
          };
      causeOfActionName = causeOfActionService.getCauseName(matter.getCauseOfAction(), causeType);
    }
    vars.put("causeOfAction", causeOfActionName);

    // ========== 合同信息 ==========
    String contractNo = "";
    String trialStage = "";
    if (matter.getContractId() != null) {
      try {
        Contract contract = contractRepository.getById(matter.getContractId());
        if (contract != null) {
          if (contract.getContractNo() != null) {
            contractNo = contract.getContractNo();
          }
          if (contract.getTrialStage() != null && !contract.getTrialStage().isEmpty()) {
            trialStage = getTrialStageName(contract.getTrialStage());
          }
        }
      } catch (Exception e) {
        log.warn(
            "获取项目合同信息失败: matterId={}, contractId={}", matter.getId(), matter.getContractId(), e);
      }
    }
    vars.put("contractNo", contractNo);
    vars.put("trialStage", trialStage);
    vars.put("procedureStage", trialStage);

    // ========== 客户信息 ==========
    String clientName = "";
    String clientIdNumber = "";
    String clientRole = "";
    String clientRoleName = "";
    if (matter.getClientId() != null) {
      try {
        var primaryClient = matterClientRepository.findPrimaryClient(matter.getId());
        Long clientId =
            primaryClient
                .map(com.lawfirm.domain.matter.entity.MatterClient::getClientId)
                .orElse(matter.getClientId());

        // 获取客户角色（诉讼地位）
        if (primaryClient.isPresent()) {
          var matterClient = primaryClient.get();
          if (matterClient.getClientRole() != null && !matterClient.getClientRole().isEmpty()) {
            clientRole = matterClient.getClientRole();
            clientRoleName =
                com.lawfirm.application.matter.dto.MatterClientDTO.getClientRoleName(clientRole);
          }
        }

        Client client = clientRepository.getById(clientId);
        if (client != null) {
          clientName = client.getName() != null ? client.getName() : "";
          if ("INDIVIDUAL".equals(client.getClientType())) {
            clientIdNumber = client.getIdCard() != null ? client.getIdCard() : "";
          } else {
            clientIdNumber = client.getCreditCode() != null ? client.getCreditCode() : "";
          }
        }
      } catch (Exception e) {
        log.warn("获取客户信息失败: matterId={}, clientId={}", matter.getId(), matter.getClientId(), e);
      }
    }
    vars.put("clientName", clientName);
    vars.put("clientIdNumber", clientIdNumber);
    vars.put("clientRole", clientRole);
    vars.put("clientRoleName", clientRoleName);

    // ========== 对方当事人信息 ==========
    vars.put("opposingParty", matter.getOpposingParty() != null ? matter.getOpposingParty() : "");

    // 对方当事人诉讼地位（根据委托人诉讼地位推导）
    String opposingPartyRole = getOpposingPartyRole(clientRole);
    String opposingPartyRoleName = MatterClientDTO.getClientRoleName(opposingPartyRole);
    vars.put("opposingPartyRole", opposingPartyRole);
    vars.put("opposingPartyRoleName", opposingPartyRoleName);

    // ========== 律师信息 ==========
    vars.put("lawyerNames", lawyerNames != null ? lawyerNames : "");

    // 获取律师执业证号
    String lawyerLicenseNos = "";
    if (lawyerIds != null && !lawyerIds.isEmpty()) {
      lawyerLicenseNos =
          lawyerIds.stream()
              .map(
                  id -> {
                    try {
                      User user = userMapper.selectById(id);
                      return user != null && user.getLawyerLicenseNo() != null
                          ? user.getLawyerLicenseNo()
                          : "";
                    } catch (Exception e) {
                      log.warn("获取律师执业证号失败: lawyerId={}", id, e);
                      return "";
                    }
                  })
              .filter(no -> !no.isEmpty())
              .collect(Collectors.joining(","));
    }
    vars.put("lawyerLicenseNo", lawyerLicenseNos);

    // ========== 接收单位信息 ==========
    vars.put("targetUnit", targetUnit != null ? targetUnit : "");
    vars.put("targetAddress", targetAddress != null ? targetAddress : "");

    // ========== 律所信息 ==========
    String firmName = "";
    String firmAddress = "";
    String firmPhone = "";
    String firmLicense = "";
    try {
      firmName = sysConfigAppService.getConfigValue("firm.name");
      if (firmName == null) {
        firmName = "";
      }

      firmAddress = sysConfigAppService.getConfigValue("firm.address");
      if (firmAddress == null) {
        firmAddress = "";
      }

      firmPhone = sysConfigAppService.getConfigValue("firm.phone");
      if (firmPhone == null) {
        firmPhone = "";
      }

      firmLicense = sysConfigAppService.getConfigValue("firm.license");
      if (firmLicense == null) {
        firmLicense = "";
      }
    } catch (Exception e) {
      log.warn("获取律所信息失败", e);
    }
    vars.put("firmName", firmName);
    vars.put("firmAddress", firmAddress);
    vars.put("firmPhone", firmPhone);
    vars.put("firmLicense", firmLicense);

    // ========== 函件信息 ==========
    vars.put("letterNo", applicationNo != null ? applicationNo : "");

    // ========== 日期信息 ==========
    LocalDate now = LocalDate.now();
    String chineseDate =
        String.format("%d年%02d月%02d日", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    vars.put("date", chineseDate);
    vars.put("currentYear", String.valueOf(now.getYear()));
    vars.put("currentDate", chineseDate);

    return vars;
  }

  /**
   * 通知行政人员（发送给所有行政角色的用户）
   *
   * @param title 通知标题
   * @param content 通知内容
   * @param businessType 业务类型
   * @param businessId 业务ID
   */
  private void notifyAdminStaff(
      final String title, final String content, final String businessType, final Long businessId) {
    try {
      // 查询所有行政人员（角色为ADMIN或有行政权限的用户）
      List<User> adminUsers = userMapper.selectAdminUsers();
      for (User admin : adminUsers) {
        notificationAppService.sendSystemNotification(
            admin.getId(), title, content, businessType, businessId);
      }
    } catch (Exception e) {
      log.warn("发送通知给行政人员失败: {}", e.getMessage());
    }
  }

  /**
   * 获取函件类型名称
   *
   * @param type 函件类型
   * @return 类型名称
   */
  private String getLetterTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "INTRODUCTION" -> "介绍信";
      case "MEETING" -> "会见函";
      case "INVESTIGATION" -> "调查函";
      case "FILE_REVIEW" -> "阅卷函";
      case "LEGAL_OPINION" -> "法律意见函";
      default -> "其他";
    };
  }

  /**
   * 获取审理阶段名称（支持多选，逗号分隔）
   *
   * @param stage 审理阶段
   * @return 阶段名称
   */
  private String getTrialStageName(final String stage) {
    if (stage == null || stage.isEmpty()) {
      return "";
    }
    // 支持多选：逗号分隔的多个值
    return java.util.Arrays.stream(stage.split(","))
        .map(
            s ->
                switch (s.trim()) {
                    // 通用阶段
                  case "FIRST_INSTANCE" -> "一审";
                  case "SECOND_INSTANCE" -> "二审";
                  case "RETRIAL" -> "再审";
                  case "EXECUTION" -> "执行";
                  case "NON_LITIGATION" -> "非诉服务";
                  case "ARBITRATION" -> "仲裁阶段";
                    // 刑事案件阶段
                  case "INVESTIGATION" -> "侦查阶段";
                  case "PROSECUTION_REVIEW" -> "审查起诉";
                  case "DEATH_PENALTY_REVIEW" -> "死刑复核";
                    // 行政案件阶段
                  case "ADMINISTRATIVE_RECONSIDERATION" -> "行政复议";
                    // 执行案件阶段
                  case "EXECUTION_OBJECTION" -> "执行异议";
                  case "EXECUTION_REVIEW" -> "执行复议";
                  default -> s;
                })
        .collect(java.util.stream.Collectors.joining("、"));
  }

  private String getStatusName(final String status) {
    return LetterStatus.getStatusName(status);
  }

  private LetterTemplateDTO toTemplateDTO(final LetterTemplate t) {
    LetterTemplateDTO dto = new LetterTemplateDTO();
    dto.setId(t.getId());
    dto.setTemplateNo(t.getTemplateNo());
    dto.setName(t.getName());
    dto.setLetterType(t.getLetterType());
    dto.setLetterTypeName(getLetterTypeName(t.getLetterType()));
    dto.setContent(t.getContent());
    dto.setDescription(t.getDescription());
    dto.setStatus(t.getStatus());
    dto.setSortOrder(t.getSortOrder());
    dto.setCreatedAt(t.getCreatedAt());
    dto.setUpdatedAt(t.getUpdatedAt());
    return dto;
  }

  private LetterApplicationDTO toApplicationDTO(final LetterApplication a) {
    LetterApplicationDTO dto = new LetterApplicationDTO();
    dto.setId(a.getId());
    dto.setApplicationNo(a.getApplicationNo());
    dto.setTemplateId(a.getTemplateId());
    // 填充模板名称
    if (a.getTemplateId() != null) {
      LetterTemplate template = templateRepository.getById(a.getTemplateId());
      if (template != null) {
        dto.setTemplateName(template.getName());
      }
    }
    dto.setMatterId(a.getMatterId());
    // 填充项目信息
    if (a.getMatterId() != null) {
      Matter matter = matterRepository.getById(a.getMatterId());
      if (matter != null) {
        dto.setMatterName(matter.getName());
        dto.setMatterNo(matter.getMatterNo());
      }
    }
    dto.setClientId(a.getClientId());
    dto.setApplicantId(a.getApplicantId());
    dto.setApplicantName(a.getApplicantName());
    dto.setDepartmentId(a.getDepartmentId());
    dto.setLetterType(a.getLetterType());
    dto.setLetterTypeName(getLetterTypeName(a.getLetterType()));
    dto.setTargetUnit(a.getTargetUnit());
    dto.setTargetContact(a.getTargetContact());
    dto.setTargetPhone(a.getTargetPhone());
    dto.setTargetAddress(a.getTargetAddress());
    dto.setPurpose(a.getPurpose());
    dto.setLawyerIds(a.getLawyerIds());
    dto.setLawyerNames(a.getLawyerNames());
    dto.setContent(a.getContent());
    dto.setCopies(a.getCopies());
    dto.setExpectedDate(a.getExpectedDate());
    dto.setStatus(a.getStatus());
    dto.setStatusName(getStatusName(a.getStatus()));
    dto.setApprovedBy(a.getApprovedBy());
    // 填充审批人姓名
    if (a.getApprovedBy() != null) {
      var approver = userMapper.selectById(a.getApprovedBy());
      if (approver != null) {
        dto.setApproverName(approver.getRealName());
      }
    }
    dto.setApprovedAt(a.getApprovedAt());
    dto.setApprovalComment(a.getApprovalComment());
    dto.setPrintedBy(a.getPrintedBy());
    // 填充打印人姓名
    if (a.getPrintedBy() != null) {
      var printer = userMapper.selectById(a.getPrintedBy());
      if (printer != null) {
        dto.setPrinterName(printer.getRealName());
      }
    }
    dto.setPrintedAt(a.getPrintedAt());
    dto.setReceivedBy(a.getReceivedBy());
    // 填充领取人姓名
    if (a.getReceivedBy() != null) {
      var receiver = userMapper.selectById(a.getReceivedBy());
      if (receiver != null) {
        dto.setReceiverName(receiver.getRealName());
      }
    }
    dto.setReceivedAt(a.getReceivedAt());
    dto.setRemark(a.getRemark());
    dto.setCreatedAt(a.getCreatedAt());
    dto.setUpdatedAt(a.getUpdatedAt());
    return dto;
  }

  /**
   * 根据委托人诉讼地位推导对方当事人诉讼地位
   *
   * @param clientRole 委托人诉讼地位代码
   * @return 对方当事人诉讼地位代码
   */
  private String getOpposingPartyRole(final String clientRole) {
    if (clientRole == null || clientRole.isEmpty()) {
      return "";
    }

    return switch (clientRole) {
        // 一审/普通诉讼
      case "PLAINTIFF" -> "DEFENDANT"; // 原告 → 被告
      case "DEFENDANT" -> "PLAINTIFF"; // 被告 → 原告

        // 二审
      case "APPELLANT" -> "APPELLEE"; // 上诉人 → 被上诉人
      case "APPELLEE" -> "APPELLANT"; // 被上诉人 → 上诉人

        // 仲裁
      case "APPLICANT" -> "RESPONDENT"; // 申请人 → 被申请人
      case "RESPONDENT" -> "APPLICANT"; // 被申请人 → 申请人

        // 执行
      case "EXECUTION_APPLICANT" -> "EXECUTION_RESPONDENT"; // 申请执行人 → 被执行人
      case "EXECUTION_RESPONDENT" -> "EXECUTION_APPLICANT"; // 被执行人 → 申请执行人

        // 再审
      case "RETRIAL_APPLICANT" -> "RETRIAL_RESPONDENT"; // 再审申请人 → 再审被申请人
      case "RETRIAL_RESPONDENT" -> "RETRIAL_APPLICANT"; // 再审被申请人 → 再审申请人

        // 其他情况（第三人、犯罪嫌疑人、被告人等）没有对应的对方角色
      default -> "";
    };
  }
}
