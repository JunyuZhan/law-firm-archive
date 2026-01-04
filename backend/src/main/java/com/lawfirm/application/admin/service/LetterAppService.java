package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateLetterApplicationCommand;
import com.lawfirm.application.admin.dto.LetterApplicationDTO;
import com.lawfirm.application.admin.dto.LetterTemplateDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.LetterApplication;
import com.lawfirm.domain.admin.entity.LetterTemplate;
import com.lawfirm.domain.admin.repository.LetterApplicationRepository;
import com.lawfirm.domain.admin.repository.LetterTemplateRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.LetterApplicationMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 出函管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LetterAppService {

    private final LetterTemplateRepository templateRepository;
    private final LetterApplicationRepository applicationRepository;
    private final LetterApplicationMapper applicationMapper;
    private final MatterRepository matterRepository;
    private final UserMapper userMapper;
    private final NotificationAppService notificationAppService;

    // ==================== 模板管理 ====================

    /**
     * 获取启用的模板列表
     */
    public List<LetterTemplateDTO> listActiveTemplates() {
        LambdaQueryWrapper<LetterTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LetterTemplate::getStatus, "ACTIVE")
               .orderByAsc(LetterTemplate::getSortOrder);
        return templateRepository.list(wrapper).stream()
                .map(this::toTemplateDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建模板
     */
    @Transactional
    public LetterTemplateDTO createTemplate(String name, String letterType, String content, String description) {
        String templateNo = "LT" + LocalDate.now().toString().replace("-", "").substring(2) 
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        
        LetterTemplate template = LetterTemplate.builder()
                .templateNo(templateNo)
                .name(name)
                .letterType(letterType)
                .content(content)
                .description(description)
                .status("ACTIVE")
                .build();
        templateRepository.save(template);
        log.info("创建出函模板: {}", name);
        return toTemplateDTO(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public LetterTemplateDTO updateTemplate(Long id, String name, String letterType, String content, String description) {
        LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        if (name != null) template.setName(name);
        if (letterType != null) template.setLetterType(letterType);
        if (content != null) template.setContent(content);
        if (description != null) template.setDescription(description);
        templateRepository.updateById(template);
        log.info("更新出函模板: {}", template.getName());
        return toTemplateDTO(template);
    }

    /**
     * 启用/停用模板
     */
    @Transactional
    public void toggleTemplateStatus(Long id) {
        LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        template.setStatus("ACTIVE".equals(template.getStatus()) ? "DISABLED" : "ACTIVE");
        templateRepository.updateById(template);
        log.info("模板状态变更: {} -> {}", template.getName(), template.getStatus());
    }

    /**
     * 获取所有模板（管理员用）
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
     */
    public LetterTemplateDTO getTemplateById(Long id) {
        LetterTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        return toTemplateDTO(template);
    }

    // ==================== 出函申请 ====================

    /**
     * 创建出函申请（律师）
     */
    @Transactional
    public LetterApplicationDTO createApplication(CreateLetterApplicationCommand cmd) {
        // 验证模板
        LetterTemplate template = templateRepository.getByIdOrThrow(cmd.getTemplateId(), "模板不存在");
        
        // 验证项目
        Matter matter = matterRepository.getByIdOrThrow(cmd.getMatterId(), "项目不存在");
        if (!"ACTIVE".equals(matter.getStatus())) {
            throw new BusinessException("只能为进行中的项目申请出函");
        }

        // 生成申请编号
        String applicationNo = "LA" + LocalDate.now().toString().replace("-", "").substring(2) 
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 获取律师姓名
        String lawyerNames = "";
        String lawyerIds = "";
        if (cmd.getLawyerIds() != null && !cmd.getLawyerIds().isEmpty()) {
            lawyerIds = cmd.getLawyerIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            lawyerNames = cmd.getLawyerIds().stream()
                    .map(id -> {
                        var user = userMapper.selectById(id);
                        return user != null ? user.getRealName() : "";
                    })
                    .collect(Collectors.joining(","));
        }

        // 生成函件内容（替换模板变量）
        String content = generateContent(template.getContent(), matter, cmd.getTargetUnit(), lawyerNames);

        LetterApplication application = LetterApplication.builder()
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
                .status("PENDING")
                .remark(cmd.getRemark())
                .build();

        applicationRepository.save(application);
        log.info("创建出函申请: {} -> {}", applicationNo, cmd.getTargetUnit());
        
        // 发送通知给行政人员（角色为ADMIN的用户）
        notifyAdminStaff("新出函申请", 
                String.format("收到新的出函申请 [%s]，申请人：%s，请及时审批", applicationNo, SecurityUtils.getRealName()),
                "LETTER", application.getId());
        
        return toApplicationDTO(application);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Long id, String comment) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException("只能审批待审批状态的申请");
        }
        app.setStatus("APPROVED");
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
                "LETTER", app.getId());
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(Long id, String comment) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException("只能审批待审批状态的申请");
        }
        app.setStatus("REJECTED");
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
                "LETTER", app.getId());
    }

    /**
     * 退回修改（审批人退回给申请人修改）
     */
    @Transactional
    public void returnForRevision(Long id, String comment) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException("只能退回待审批状态的申请");
        }
        app.setStatus("RETURNED");
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
                "LETTER", app.getId());
    }

    /**
     * 重新提交（申请人修改后重新提交）
     */
    @Transactional
    public LetterApplicationDTO resubmit(Long id, CreateLetterApplicationCommand cmd) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        // 只能重新提交自己的申请
        if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能重新提交自己的申请");
        }
        
        // 只能重新提交被退回或被拒绝的申请
        if (!"RETURNED".equals(app.getStatus()) && !"REJECTED".equals(app.getStatus())) {
            throw new BusinessException("只能重新提交被退回或被拒绝的申请");
        }

        // 验证模板
        LetterTemplate template = templateRepository.getByIdOrThrow(cmd.getTemplateId(), "模板不存在");
        
        // 验证项目
        Matter matter = matterRepository.getByIdOrThrow(cmd.getMatterId(), "项目不存在");
        if (!"ACTIVE".equals(matter.getStatus())) {
            throw new BusinessException("只能为进行中的项目申请出函");
        }

        // 获取律师姓名
        String lawyerNames = "";
        String lawyerIds = "";
        if (cmd.getLawyerIds() != null && !cmd.getLawyerIds().isEmpty()) {
            lawyerIds = cmd.getLawyerIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            lawyerNames = cmd.getLawyerIds().stream()
                    .map(lid -> {
                        var user = userMapper.selectById(lid);
                        return user != null ? user.getRealName() : "";
                    })
                    .collect(Collectors.joining(","));
        }

        // 重新生成函件内容
        String content = generateContent(template.getContent(), matter, cmd.getTargetUnit(), lawyerNames);

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
        app.setStatus("PENDING");
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
     */
    @Transactional
    public LetterApplicationDTO updateApplication(Long id, CreateLetterApplicationCommand cmd) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        // 只能修改自己的申请
        if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能修改自己的申请");
        }
        
        // 只能修改被退回或被拒绝的申请
        if (!"RETURNED".equals(app.getStatus()) && !"REJECTED".equals(app.getStatus())) {
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
            lawyerNames = cmd.getLawyerIds().stream()
                    .map(lid -> {
                        var user = userMapper.selectById(lid);
                        return user != null ? user.getRealName() : "";
                    })
                    .collect(Collectors.joining(","));
        }

        // 重新生成函件内容
        String content = generateContent(template.getContent(), matter, cmd.getTargetUnit(), lawyerNames);

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
     */
    @Transactional
    public void submitForApproval(Long id) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        // 只能提交自己的申请
        if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能提交自己的申请");
        }
        
        // 只能提交被退回或被拒绝的申请
        if (!"RETURNED".equals(app.getStatus()) && !"REJECTED".equals(app.getStatus())) {
            throw new BusinessException("只能提交被退回或被拒绝的申请");
        }

        app.setStatus("PENDING");
        // 清除之前的审批信息
        app.setApprovedBy(null);
        app.setApprovedAt(null);
        app.setApprovalComment(null);
        
        applicationRepository.updateById(app);
        log.info("出函申请重新提交审批: {}", app.getApplicationNo());
    }

    /**
     * 获取全部申请列表（行政管理用）
     */
    public List<LetterApplicationDTO> listAllApplications(String applicationNo, String matterName, 
            String status, String startDate, String endDate) {
        return applicationMapper.selectAllApplications(applicationNo, matterName, status, startDate, endDate)
                .stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待打印列表（行政用）
     */
    public List<LetterApplicationDTO> listPendingPrint() {
        return applicationMapper.selectPendingPrint().stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 确认打印（行政）
     */
    @Transactional
    public void confirmPrint(Long id) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        if (!"APPROVED".equals(app.getStatus())) {
            throw new BusinessException("只能打印已审批通过的申请");
        }
        app.setStatus("PRINTED");
        app.setPrintedBy(SecurityUtils.getUserId());
        app.setPrintedAt(LocalDateTime.now());
        applicationRepository.updateById(app);
        log.info("出函已打印: {}", app.getApplicationNo());
        
        // 发送通知给申请人
        notificationAppService.sendSystemNotification(
                app.getApplicantId(),
                "出函已打印",
                String.format("您的出函申请 [%s] 已打印完成，请前往行政部领取", app.getApplicationNo()),
                "LETTER", app.getId());
    }

    /**
     * 确认领取
     */
    @Transactional
    public void confirmReceive(Long id) {
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
     */
    public List<LetterApplicationDTO> listByMatter(Long matterId) {
        return applicationMapper.selectByMatterId(matterId).stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取申请详情
     */
    public LetterApplicationDTO getById(Long id) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        return toApplicationDTO(app);
    }

    /**
     * 我的申请列表（律师用）
     */
    public List<LetterApplicationDTO> listMyApplications() {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<LetterApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LetterApplication::getApplicantId, userId)
               .orderByDesc(LetterApplication::getCreatedAt);
        return applicationRepository.list(wrapper).stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    /**
     * 取消申请（律师）
     */
    @Transactional
    public void cancelApplication(Long id) {
        LetterApplication app = applicationRepository.getByIdOrThrow(id, "申请不存在");
        
        // 只能取消自己的申请
        if (!app.getApplicantId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("只能取消自己的申请");
        }
        
        // 只能取消待审批状态的申请
        if (!"PENDING".equals(app.getStatus())) {
            throw new BusinessException("只能取消待审批状态的申请");
        }
        
        app.setStatus("CANCELLED");
        applicationRepository.updateById(app);
        log.info("出函申请已取消: {}", app.getApplicationNo());
    }

    /**
     * 待审批列表（审批人用）
     */
    public List<LetterApplicationDTO> listPendingApproval() {
        LambdaQueryWrapper<LetterApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LetterApplication::getStatus, "PENDING")
               .orderByAsc(LetterApplication::getCreatedAt);
        return applicationRepository.list(wrapper).stream()
                .map(this::toApplicationDTO)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    private String generateContent(String template, Matter matter, String targetUnit, String lawyerNames) {
        return template
                .replace("${matterName}", matter.getName() != null ? matter.getName() : "")
                .replace("${matterNo}", matter.getMatterNo() != null ? matter.getMatterNo() : "")
                .replace("${targetUnit}", targetUnit != null ? targetUnit : "")
                .replace("${lawyerNames}", lawyerNames != null ? lawyerNames : "")
                .replace("${date}", LocalDate.now().toString());
    }

    /**
     * 通知行政人员（发送给所有行政角色的用户）
     */
    private void notifyAdminStaff(String title, String content, String businessType, Long businessId) {
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

    private String getLetterTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "INTRODUCTION" -> "介绍信";
            case "MEETING" -> "会见函";
            case "INVESTIGATION" -> "调查函";
            case "FILE_REVIEW" -> "阅卷函";
            case "LEGAL_OPINION" -> "法律意见函";
            default -> "其他";
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            case "RETURNED" -> "已退回";
            case "PRINTED" -> "已打印";
            case "RECEIVED" -> "已领取";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private LetterTemplateDTO toTemplateDTO(LetterTemplate t) {
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

    private LetterApplicationDTO toApplicationDTO(LetterApplication a) {
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
}
