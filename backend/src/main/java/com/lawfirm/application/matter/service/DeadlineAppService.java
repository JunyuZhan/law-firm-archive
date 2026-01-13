package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateDeadlineCommand;
import com.lawfirm.application.matter.command.UpdateDeadlineCommand;
import com.lawfirm.application.matter.dto.DeadlineDTO;
import com.lawfirm.application.matter.dto.DeadlineQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.base.BaseRepository;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Deadline;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.DeadlineRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 期限提醒应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineAppService {

    private final DeadlineRepository deadlineRepository;
    private final DeadlineMapper deadlineMapper;
    private final MatterRepository matterRepository;
    private final UserRepository userRepository;
    private final NotificationAppService notificationAppService;
    private MatterAppService matterAppService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    public void setMatterAppService(MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 分页查询期限提醒
     */
    public PageResult<DeadlineDTO> listDeadlines(DeadlineQueryDTO query) {
        IPage<Deadline> page = deadlineMapper.selectDeadlinePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query
        );

        List<DeadlineDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取期限详情
     */
    public DeadlineDTO getDeadlineById(Long id) {
        Deadline deadline = deadlineRepository.getByIdOrThrow(id, "期限提醒不存在");
        return toDTO(deadline);
    }

    /**
     * 根据项目ID查询期限列表
     */
    public List<DeadlineDTO> getDeadlinesByMatterId(Long matterId) {
        matterRepository.getByIdOrThrow(matterId, "项目不存在");
        List<Deadline> deadlines = deadlineRepository.findByMatterId(matterId);
        return deadlines.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建期限提醒
     */
    @Transactional
    public DeadlineDTO createDeadline(CreateDeadlineCommand command) {
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "项目不存在");
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能创建期限提醒）
        matterAppService.validateMatterOwnership(command.getMatterId());

        // 基准日期默认为当前日期
        LocalDate baseDate = command.getBaseDate() != null ? command.getBaseDate() : LocalDate.now();

        // 验证期限日期不能早于基准日期
        if (command.getDeadlineDate().isBefore(baseDate)) {
            throw new BusinessException("期限日期不能早于基准日期");
        }

        Deadline deadline = Deadline.builder()
                .matterId(command.getMatterId())
                .deadlineType(command.getDeadlineType())
                .deadlineName(command.getDeadlineName())
                .baseDate(baseDate)
                .deadlineDate(command.getDeadlineDate())
                .reminderDays(command.getReminderDays() != null ? command.getReminderDays() : 7)
                .reminderSent(false)
                .status("ACTIVE")
                .description(command.getDescription())
                .createdBy(SecurityUtils.getUserId())
                .build();

        deadlineRepository.getBaseMapper().insert(deadline);
        log.info("创建期限提醒成功: matterId={}, deadlineName={}", command.getMatterId(), command.getDeadlineName());
        return toDTO(deadline);
    }

    /**
     * 自动创建期限提醒（根据项目信息）
     * 根据立案日期、开庭日期等自动计算常见期限
     */
    @Transactional
    public void autoCreateDeadlines(Long matterId) {
        Matter matter = matterRepository.getByIdOrThrow(matterId, "项目不存在");

        // 只对诉讼类项目自动创建期限
        if (!"LITIGATION".equals(matter.getMatterType())) {
            return;
        }

        LocalDate filingDate = matter.getFilingDate();
        if (filingDate == null) {
            log.warn("项目{}没有立案日期，无法自动创建期限提醒", matterId);
            return;
        }

        // 检查是否已存在期限提醒，避免重复创建
        List<Deadline> existingDeadlines = deadlineRepository.findByMatterId(matterId);
        if (!existingDeadlines.isEmpty()) {
            log.info("项目{}已存在期限提醒，跳过自动创建", matterId);
            return;
        }

        // 1. 举证期限：立案后15天（民事诉讼）
        if ("CIVIL".equals(matter.getBusinessType())) {
            LocalDate evidenceDeadline = filingDate.plusDays(15);
            CreateDeadlineCommand cmd1 = CreateDeadlineCommand.builder()
                    .matterId(matterId)
                    .deadlineType("EVIDENCE_SUBMISSION")
                    .deadlineName("举证期限")
                    .baseDate(filingDate)
                    .deadlineDate(evidenceDeadline)
                    .reminderDays(7)
                    .description("民事诉讼举证期限，自立案之日起15天")
                    .build();
            createDeadline(cmd1);
        }

        // 2. 答辩期限：立案后15天（民事诉讼）
        if ("CIVIL".equals(matter.getBusinessType())) {
            LocalDate replyDeadline = filingDate.plusDays(15);
            CreateDeadlineCommand cmd2 = CreateDeadlineCommand.builder()
                    .matterId(matterId)
                    .deadlineType("REPLY")
                    .deadlineName("答辩期限")
                    .baseDate(filingDate)
                    .deadlineDate(replyDeadline)
                    .reminderDays(7)
                    .description("民事诉讼答辩期限，自立案之日起15天")
                    .build();
            createDeadline(cmd2);
        }

        log.info("自动创建期限提醒成功: matterId={}", matterId);
    }

    /**
     * 更新期限提醒
     */
    @Transactional
    public DeadlineDTO updateDeadline(UpdateDeadlineCommand command) {
        Deadline deadline = deadlineRepository.getByIdOrThrow(command.getId(), "期限提醒不存在");
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能更新期限提醒）
        matterAppService.validateMatterOwnership(deadline.getMatterId());

        if (!"ACTIVE".equals(deadline.getStatus())) {
            throw new BusinessException("只有有效状态的期限才能更新");
        }

        if (command.getDeadlineName() != null) {
            deadline.setDeadlineName(command.getDeadlineName());
        }
        if (command.getBaseDate() != null) {
            deadline.setBaseDate(command.getBaseDate());
        }
        if (command.getDeadlineDate() != null) {
            deadline.setDeadlineDate(command.getDeadlineDate());
        }
        if (command.getReminderDays() != null) {
            deadline.setReminderDays(command.getReminderDays());
        }
        if (command.getStatus() != null) {
            deadline.setStatus(command.getStatus());
        }
        if (command.getDescription() != null) {
            deadline.setDescription(command.getDescription());
        }

        deadline.setUpdatedBy(SecurityUtils.getUserId());
        deadlineRepository.getBaseMapper().updateById(deadline);
        log.info("更新期限提醒成功: id={}", command.getId());
        return toDTO(deadline);
    }

    /**
     * 完成期限
     */
    @Transactional
    public DeadlineDTO completeDeadline(Long id) {
        Deadline deadline = deadlineRepository.getByIdOrThrow(id, "期限提醒不存在");
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能完成期限）
        matterAppService.validateMatterOwnership(deadline.getMatterId());

        if (!"ACTIVE".equals(deadline.getStatus())) {
            throw new BusinessException("只有有效状态的期限才能标记为完成");
        }

        deadline.setStatus("COMPLETED");
        deadline.setCompletedAt(LocalDateTime.now());
        deadline.setCompletedBy(SecurityUtils.getUserId());
        deadline.setUpdatedBy(SecurityUtils.getUserId());
        deadlineRepository.getBaseMapper().updateById(deadline);
        log.info("完成期限提醒: id={}", id);
        return toDTO(deadline);
    }

    /**
     * 删除期限提醒
     */
    @Transactional
    public void deleteDeadline(Long id) {
        Deadline deadline = deadlineRepository.getByIdOrThrow(id, "期限提醒不存在");
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能删除期限提醒）
        matterAppService.validateMatterOwnership(deadline.getMatterId());
        
        deadlineRepository.softDelete(id);
        log.info("删除期限提醒成功: id={}", id);
    }

    /**
     * 获取我的即将到期的期限
     */
    public List<DeadlineDTO> getMyUpcomingDeadlines(Integer days, Integer limit) {
        Long userId = SecurityUtils.getUserId();
        List<Deadline> deadlines = deadlineRepository.findMyUpcoming(userId, days, limit);
        return deadlines.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 定时任务：发送期限提醒
     * 每天上午9点执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendDeadlineReminders() {
        log.info("开始执行期限提醒定时任务");
        List<Deadline> deadlines = deadlineRepository.findNeedReminder();

        for (Deadline deadline : deadlines) {
            try {
                Matter matter = matterRepository.getByIdOrThrow(deadline.getMatterId(), "项目不存在");
                Long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline.getDeadlineDate());

                // 发送系统通知
                String message = String.format("【期限提醒】项目【%s】的期限【%s】将于%d天后到期（%s），请及时处理！",
                        matter.getName(), deadline.getDeadlineName(), daysRemaining, deadline.getDeadlineDate());

                // 通知主办律师
                if (matter.getLeadLawyerId() != null) {
                    notificationAppService.sendSystemNotification(
                            matter.getLeadLawyerId(),
                            "期限提醒",
                            message,
                            "DEADLINE",
                            deadline.getId()
                    );
                }

                // 标记已发送提醒
                deadline.setReminderSent(true);
                deadline.setReminderSentAt(LocalDateTime.now());
                deadlineRepository.getBaseMapper().updateById(deadline);

                log.info("发送期限提醒成功: deadlineId={}, matterId={}, daysRemaining={}", 
                        deadline.getId(), deadline.getMatterId(), daysRemaining);
            } catch (Exception e) {
                log.error("发送期限提醒失败: deadlineId={}", deadline.getId(), e);
            }
        }

        log.info("期限提醒定时任务执行完成，共处理{}条提醒", deadlines.size());
    }

    /**
     * 定时任务：更新过期期限状态
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void updateExpiredDeadlines() {
        log.info("开始执行过期期限更新任务");
        List<Deadline> expiredDeadlines = deadlineRepository.findUpcomingDeadlines();

        int count = 0;
        for (Deadline deadline : expiredDeadlines) {
            if (deadline.getDeadlineDate().isBefore(LocalDate.now()) && "ACTIVE".equals(deadline.getStatus())) {
                deadline.setStatus("EXPIRED");
                deadline.setUpdatedBy(null); // 系统自动更新
                deadlineRepository.getBaseMapper().updateById(deadline);
                count++;
            }
        }

        log.info("过期期限更新任务执行完成，共更新{}条", count);
    }

    /**
     * Deadline Entity 转 DTO
     */
    private DeadlineDTO toDTO(Deadline deadline) {
        DeadlineDTO dto = new DeadlineDTO();
        BeanUtils.copyProperties(deadline, dto);

        // 关联项目信息
        if (deadline.getMatterId() != null) {
            try {
                Matter matter = matterRepository.getByIdOrThrow(deadline.getMatterId(), null);
                dto.setMatterNo(matter.getMatterNo());
                dto.setMatterName(matter.getName());
            } catch (Exception e) {
                // 项目不存在，忽略
            }
        }

        // 完成人信息
        if (deadline.getCompletedBy() != null) {
            try {
                User user = userRepository.getByIdOrThrow(deadline.getCompletedBy(), null);
                dto.setCompletedByName(user.getRealName());
            } catch (Exception e) {
                // 用户不存在，忽略
            }
        }

        // 计算剩余天数
        if (deadline.getDeadlineDate() != null) {
            dto.setDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), deadline.getDeadlineDate()));
        }

        // 状态名称
        dto.setStatusName(getDeadlineStatusName(deadline.getStatus()));
        dto.setDeadlineTypeName(getDeadlineTypeName(deadline.getDeadlineType()));

        return dto;
    }

    private String getDeadlineTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "EVIDENCE_SUBMISSION" -> "举证期";
            case "APPEAL" -> "上诉期";
            case "REPLY" -> "答辩期";
            case "EXECUTION_APPLICATION" -> "执行申请期";
            case "HEARING" -> "开庭期";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    private String getDeadlineStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "有效";
            case "COMPLETED" -> "已完成";
            case "EXPIRED" -> "已过期";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}

