package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateHandoverCommand;
import com.lawfirm.application.system.dto.*;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.Lead;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.LeadRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.system.entity.DataHandover;
import com.lawfirm.domain.system.entity.DataHandoverDetail;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.DataHandoverDetailRepository;
import com.lawfirm.domain.system.repository.DataHandoverRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据交接服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataHandoverService {

    private final DataHandoverRepository handoverRepository;
    private final DataHandoverDetailRepository detailRepository;
    private final MatterRepository matterRepository;
    private final MatterParticipantRepository participantRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 预览离职交接数据
     */
    public DataHandoverPreviewDTO previewResignationHandover(Long fromUserId) {
        User user = userRepository.getByIdOrThrow(fromUserId, "用户不存在");
        
        DataHandoverPreviewDTO preview = new DataHandoverPreviewDTO();
        preview.setUserId(fromUserId);
        preview.setUserName(user.getRealName());
        
        // 1. 主办项目（进行中的）
        List<Matter> leadMatters = matterRepository.lambdaQuery()
                .eq(Matter::getLeadLawyerId, fromUserId)
                .eq(Matter::getDeleted, false)
                .notIn(Matter::getStatus, "ARCHIVED", "CLOSED")
                .list();
        preview.setLeadMatterCount(leadMatters.size());
        preview.setLeadMatters(leadMatters.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("matterNo", m.getMatterNo());
            map.put("name", m.getName());
            map.put("status", m.getStatus());
            return map;
        }).collect(Collectors.toList()));
        
        // 2. 参与项目（活跃的）
        List<MatterParticipant> participations = participantRepository.lambdaQuery()
                .eq(MatterParticipant::getUserId, fromUserId)
                .eq(MatterParticipant::getStatus, "ACTIVE")
                .eq(MatterParticipant::getDeleted, false)
                .list();
        preview.setParticipantMatterCount(participations.size());
        preview.setParticipantMatters(participations.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("matterId", p.getMatterId());
            map.put("role", p.getRole());
            return map;
        }).collect(Collectors.toList()));
        
        // 3. 案源人项目
        long originatorMatterCount = matterRepository.lambdaQuery()
                .eq(Matter::getOriginatorId, fromUserId)
                .eq(Matter::getDeleted, false)
                .notIn(Matter::getStatus, "ARCHIVED")
                .count();
        preview.setOriginatorMatterCount((int) originatorMatterCount);
        
        // 4. 负责客户
        List<Client> clients = clientRepository.lambdaQuery()
                .eq(Client::getResponsibleLawyerId, fromUserId)
                .eq(Client::getDeleted, false)
                .list();
        preview.setClientCount(clients.size());
        preview.setClients(clients.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("clientNo", c.getClientNo());
            map.put("name", c.getName());
            return map;
        }).collect(Collectors.toList()));
        
        // 5. 负责案源（未转化的）
        List<Lead> leads = leadRepository.lambdaQuery()
                .eq(Lead::getResponsibleUserId, fromUserId)
                .eq(Lead::getDeleted, false)
                .notIn(Lead::getStatus, "CONVERTED", "ABANDONED")
                .list();
        preview.setLeadCount(leads.size());
        preview.setLeads(leads.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("leadNo", l.getLeadNo());
            map.put("leadName", l.getLeadName());
            map.put("status", l.getStatus());
            return map;
        }).collect(Collectors.toList()));
        
        // 6. 待办任务
        List<Task> tasks = taskRepository.findMyTodoTasks(fromUserId);
        preview.setTaskCount(tasks.size());
        preview.setTasks(tasks.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("taskNo", t.getTaskNo());
            map.put("title", t.getTitle());
            map.put("status", t.getStatus());
            return map;
        }).collect(Collectors.toList()));
        
        return preview;
    }

    /**
     * 创建离职交接单
     */
    @Transactional
    public DataHandoverDTO createResignationHandover(CreateHandoverCommand command) {
        User fromUser = userRepository.getByIdOrThrow(command.getFromUserId(), "移交人不存在");
        User toUser = userRepository.getByIdOrThrow(command.getToUserId(), "接收人不存在");
        
        // 验证不能自己交接给自己
        if (command.getFromUserId().equals(command.getToUserId())) {
            throw new BusinessException("移交人和接收人不能是同一人");
        }
        
        // 创建交接单（初始状态为待审批）
        DataHandover handover = DataHandover.builder()
                .handoverNo(generateHandoverNo())
                .fromUserId(command.getFromUserId())
                .fromUsername(fromUser.getRealName())
                .toUserId(command.getToUserId())
                .toUsername(toUser.getRealName())
                .handoverType("RESIGNATION")
                .handoverReason(command.getReason())
                .status("PENDING_APPROVAL")
                .submittedBy(SecurityUtils.getUserId())
                .submittedAt(LocalDateTime.now())
                .remark(command.getRemark())
                .build();
        
        handoverRepository.save(handover);
        
        // 创建交接明细
        int matterCount = createMatterHandoverDetails(handover, command.getFromUserId(), 
                command.getToUserId(), command.getIncludeOriginator());
        int clientCount = createClientHandoverDetails(handover, command.getFromUserId(), command.getToUserId());
        int leadCount = createLeadHandoverDetails(handover, command.getFromUserId(), command.getToUserId());
        int taskCount = createTaskHandoverDetails(handover, command.getFromUserId(), command.getToUserId());
        
        // 更新统计
        handover.setMatterCount(matterCount);
        handover.setClientCount(clientCount);
        handover.setLeadCount(leadCount);
        handover.setTaskCount(taskCount);
        
        // 创建审批记录
        Long approverId = approverService.findDataHandoverApprover(handover.getHandoverType());
        if (approverId == null) {
            throw new BusinessException("未找到可用的审批人，请联系管理员");
        }
        
        String businessTitle = String.format("离职交接: %s → %s", fromUser.getRealName(), toUser.getRealName());
        Long approvalId = approvalService.createApproval(
                "DATA_HANDOVER",
                handover.getId(),
                handover.getHandoverNo(),
                businessTitle,
                approverId,
                "HIGH",
                "NORMAL",
                null
        );
        
        handover.setApprovalId(approvalId);
        handoverRepository.updateById(handover);
        
        log.info("创建离职交接单: {} -> {}, 交接单号: {}, 审批ID: {}, 项目{}个, 客户{}个, 案源{}个, 任务{}个",
                fromUser.getRealName(), toUser.getRealName(), handover.getHandoverNo(), approvalId,
                matterCount, clientCount, leadCount, taskCount);
        
        return toDTO(handover);
    }

    /**
     * 创建项目移交单
     */
    @Transactional
    public DataHandoverDTO createMatterHandover(CreateHandoverCommand command) {
        User fromUser = userRepository.getByIdOrThrow(command.getFromUserId(), "移交人不存在");
        User toUser = userRepository.getByIdOrThrow(command.getToUserId(), "接收人不存在");
        
        if (command.getMatterIds() == null || command.getMatterIds().isEmpty()) {
            throw new BusinessException("请选择要移交的项目");
        }
        
        // 创建交接单（初始状态为待审批）
        DataHandover handover = DataHandover.builder()
                .handoverNo(generateHandoverNo())
                .fromUserId(command.getFromUserId())
                .fromUsername(fromUser.getRealName())
                .toUserId(command.getToUserId())
                .toUsername(toUser.getRealName())
                .handoverType("PROJECT")
                .handoverReason(command.getReason())
                .status("PENDING_APPROVAL")
                .submittedBy(SecurityUtils.getUserId())
                .submittedAt(LocalDateTime.now())
                .remark(command.getRemark())
                .build();
        
        handoverRepository.save(handover);
        
        // 创建指定项目的交接明细
        int matterCount = 0;
        for (Long matterId : command.getMatterIds()) {
            Matter matter = matterRepository.findById(matterId);
            if (matter != null && matter.getLeadLawyerId() != null 
                    && matter.getLeadLawyerId().equals(command.getFromUserId())) {
                createDetail(handover.getId(), "MATTER", matter.getId(),
                        matter.getMatterNo(), matter.getName(),
                        "lead_lawyer_id", command.getFromUserId().toString(), command.getToUserId().toString());
                matterCount++;
            }
        }
        
        handover.setMatterCount(matterCount);
        
        // 创建审批记录
        Long approverId = approverService.findDataHandoverApprover(handover.getHandoverType());
        if (approverId == null) {
            throw new BusinessException("未找到可用的审批人，请联系管理员");
        }
        
        String businessTitle = String.format("项目移交: %s → %s (%d个项目)", 
                fromUser.getRealName(), toUser.getRealName(), matterCount);
        Long approvalId = approvalService.createApproval(
                "DATA_HANDOVER",
                handover.getId(),
                handover.getHandoverNo(),
                businessTitle,
                approverId,
                "MEDIUM",
                "NORMAL",
                null
        );
        
        handover.setApprovalId(approvalId);
        handoverRepository.updateById(handover);
        
        log.info("创建项目移交单: {} -> {}, 交接单号: {}, 审批ID: {}, 项目{}个",
                fromUser.getRealName(), toUser.getRealName(), handover.getHandoverNo(), approvalId, matterCount);
        
        return toDTO(handover);
    }

    /**
     * 创建客户移交单
     */
    @Transactional
    public DataHandoverDTO createClientHandover(CreateHandoverCommand command) {
        User fromUser = userRepository.getByIdOrThrow(command.getFromUserId(), "移交人不存在");
        User toUser = userRepository.getByIdOrThrow(command.getToUserId(), "接收人不存在");
        
        if (command.getClientIds() == null || command.getClientIds().isEmpty()) {
            throw new BusinessException("请选择要移交的客户");
        }
        
        // 创建交接单（初始状态为待审批）
        DataHandover handover = DataHandover.builder()
                .handoverNo(generateHandoverNo())
                .fromUserId(command.getFromUserId())
                .fromUsername(fromUser.getRealName())
                .toUserId(command.getToUserId())
                .toUsername(toUser.getRealName())
                .handoverType("CLIENT")
                .handoverReason(command.getReason())
                .status("PENDING_APPROVAL")
                .submittedBy(SecurityUtils.getUserId())
                .submittedAt(LocalDateTime.now())
                .remark(command.getRemark())
                .build();
        
        handoverRepository.save(handover);
        
        // 创建指定客户的交接明细
        int clientCount = 0;
        for (Long clientId : command.getClientIds()) {
            Client client = clientRepository.findById(clientId);
            if (client != null && client.getResponsibleLawyerId() != null
                    && client.getResponsibleLawyerId().equals(command.getFromUserId())) {
                createDetail(handover.getId(), "CLIENT", client.getId(),
                        client.getClientNo(), client.getName(),
                        "responsible_lawyer_id", command.getFromUserId().toString(), command.getToUserId().toString());
                clientCount++;
            }
        }
        
        handover.setClientCount(clientCount);
        
        // 创建审批记录
        Long approverId = approverService.findDataHandoverApprover(handover.getHandoverType());
        if (approverId == null) {
            throw new BusinessException("未找到可用的审批人，请联系管理员");
        }
        
        String businessTitle = String.format("客户移交: %s → %s (%d个客户)", 
                fromUser.getRealName(), toUser.getRealName(), clientCount);
        Long approvalId = approvalService.createApproval(
                "DATA_HANDOVER",
                handover.getId(),
                handover.getHandoverNo(),
                businessTitle,
                approverId,
                "MEDIUM",
                "NORMAL",
                null
        );
        
        handover.setApprovalId(approvalId);
        handoverRepository.updateById(handover);
        
        log.info("创建客户移交单: {} -> {}, 交接单号: {}, 审批ID: {}, 客户{}个",
                fromUser.getRealName(), toUser.getRealName(), handover.getHandoverNo(), approvalId, clientCount);
        
        return toDTO(handover);
    }

    /**
     * 确认交接（审批通过后执行）
     * 只有接收人或管理员可以确认执行
     */
    @Transactional
    public void confirmHandover(Long handoverId) {
        DataHandover handover = handoverRepository.getByIdOrThrow(handoverId, "交接单不存在");
        
        // 只有审批通过（APPROVED）状态才能确认执行
        if (!"APPROVED".equals(handover.getStatus())) {
            throw new BusinessException("交接单尚未通过审批，无法确认执行");
        }
        
        Long currentUserId = SecurityUtils.getUserId();
        
        // 验证操作人身份：只有接收人或管理员可以确认执行
        if (!handover.getToUserId().equals(currentUserId) && !isAdminOrDirector()) {
            throw new BusinessException("只有接收人或管理员可以确认执行交接");
        }
        
        // 执行数据交接
        List<DataHandoverDetail> details = detailRepository.findByHandoverId(handoverId);
        int successCount = 0;
        int failCount = 0;
        
        for (DataHandoverDetail detail : details) {
            try {
                executeHandover(detail);
                detail.setStatus("DONE");
                detail.setExecutedAt(LocalDateTime.now());
                successCount++;
            } catch (Exception e) {
                detail.setStatus("FAILED");
                detail.setErrorMessage(e.getMessage());
                failCount++;
                log.error("数据交接失败: {} - {} - {}", detail.getDataType(), detail.getDataId(), e.getMessage());
            }
            detailRepository.updateById(detail);
        }
        
        // 更新交接单状态
        handover.setStatus("CONFIRMED");
        handover.setConfirmedBy(currentUserId);
        handover.setConfirmedAt(LocalDateTime.now());
        handoverRepository.updateById(handover);
        
        log.info("交接单确认完成: {}, 成功{}条, 失败{}条", handover.getHandoverNo(), successCount, failCount);
    }

    /**
     * 审批通过后更新状态
     * （由审批回调调用）
     */
    @Transactional
    public void onApprovalApproved(Long handoverId) {
        DataHandover handover = handoverRepository.getById(handoverId);
        if (handover == null) {
            log.warn("审批回调：交接单不存在, id={}", handoverId);
            return;
        }
        
        if (!"PENDING_APPROVAL".equals(handover.getStatus())) {
            log.warn("审批回调：交接单状态不是待审批, id={}, status={}", handoverId, handover.getStatus());
            return;
        }
        
        handover.setStatus("APPROVED");
        handoverRepository.updateById(handover);
        
        log.info("交接单审批通过: {}", handover.getHandoverNo());
    }

    /**
     * 审批拒绝后更新状态
     * （由审批回调调用）
     */
    @Transactional
    public void onApprovalRejected(Long handoverId, String reason) {
        DataHandover handover = handoverRepository.getById(handoverId);
        if (handover == null) {
            log.warn("审批回调：交接单不存在, id={}", handoverId);
            return;
        }
        
        handover.setStatus("REJECTED");
        handover.setRemark(reason);
        handoverRepository.updateById(handover);
        
        log.info("交接单审批拒绝: {}, 原因: {}", handover.getHandoverNo(), reason);
    }

    /**
     * 取消交接
     * 只有移交人、接收人或管理员可以取消
     */
    @Transactional
    public void cancelHandover(Long handoverId, String reason) {
        DataHandover handover = handoverRepository.getByIdOrThrow(handoverId, "交接单不存在");
        
        // 待审批或审批通过待执行的都可以取消
        if (!"PENDING_APPROVAL".equals(handover.getStatus()) && !"APPROVED".equals(handover.getStatus())) {
            throw new BusinessException("当前状态不允许取消");
        }
        
        // 验证操作人身份：只有移交人、接收人或管理员可以取消
        Long currentUserId = SecurityUtils.getUserId();
        if (!handover.getFromUserId().equals(currentUserId) 
                && !handover.getToUserId().equals(currentUserId) 
                && !isAdminOrDirector()) {
            throw new BusinessException("只有移交人、接收人或管理员可以取消交接");
        }
        
        handover.setStatus("CANCELLED");
        handover.setRemark(reason);
        handoverRepository.updateById(handover);
        
        // 如果有关联的审批记录，也取消
        if (handover.getApprovalId() != null) {
            try {
                approvalService.cancelApproval(handover.getApprovalId());
            } catch (Exception e) {
                log.warn("取消关联审批失败: {}", e.getMessage());
            }
        }
        
        log.info("交接单已取消: {}, 原因: {}", handover.getHandoverNo(), reason);
    }

    /**
     * 获取交接单详情
     */
    public DataHandoverDTO getHandoverById(Long id) {
        DataHandover handover = handoverRepository.getByIdOrThrow(id, "交接单不存在");
        DataHandoverDTO dto = toDTO(handover);
        
        // 加载明细
        List<DataHandoverDetail> details = detailRepository.findByHandoverId(id);
        dto.setDetails(details.stream().map(this::toDetailDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 分页查询交接单
     */
    public PageResult<DataHandoverDTO> listHandovers(DataHandoverQueryDTO query) {
        IPage<DataHandover> page = handoverRepository.findPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getFromUserId(),
                query.getToUserId(),
                query.getHandoverType(),
                query.getStatus()
        );
        
        List<DataHandoverDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    // ========== 辅助方法 ==========

    /**
     * 判断当前用户是否为管理员或主任
     */
    private boolean isAdminOrDirector() {
        java.util.Set<String> roles = SecurityUtils.getRoles();
        return roles != null && (roles.contains("ADMIN") || roles.contains("DIRECTOR"));
    }

    /**
     * 执行单条数据交接
     */
    private void executeHandover(DataHandoverDetail detail) {
        Long newUserId = Long.parseLong(detail.getNewValue());
        
        switch (detail.getDataType()) {
            case "MATTER" -> executeMatterHandover(detail, newUserId);
            case "CLIENT" -> executeClientHandover(detail, newUserId);
            case "LEAD" -> executeLeadHandover(detail, newUserId);
            case "TASK" -> executeTaskHandover(detail, newUserId);
            case "MATTER_PARTICIPANT" -> executeMatterParticipantHandover(detail, newUserId);
            default -> throw new BusinessException("不支持的数据类型: " + detail.getDataType());
        }
    }

    private void executeMatterHandover(DataHandoverDetail detail, Long newUserId) {
        Matter matter = matterRepository.findById(detail.getDataId());
        if (matter == null) {
            throw new BusinessException("项目不存在");
        }
        
        if ("lead_lawyer_id".equals(detail.getFieldName())) {
            matter.setLeadLawyerId(newUserId);
        } else if ("originator_id".equals(detail.getFieldName())) {
            matter.setOriginatorId(newUserId);
        }
        matterRepository.updateById(matter);
    }

    private void executeClientHandover(DataHandoverDetail detail, Long newUserId) {
        Client client = clientRepository.findById(detail.getDataId());
        if (client == null) {
            throw new BusinessException("客户不存在");
        }
        
        if ("responsible_lawyer_id".equals(detail.getFieldName())) {
            client.setResponsibleLawyerId(newUserId);
        } else if ("originator_id".equals(detail.getFieldName())) {
            client.setOriginatorId(newUserId);
        }
        clientRepository.updateById(client);
    }

    private void executeLeadHandover(DataHandoverDetail detail, Long newUserId) {
        Lead lead = leadRepository.findById(detail.getDataId());
        if (lead == null) {
            throw new BusinessException("案源不存在");
        }
        
        if ("responsible_user_id".equals(detail.getFieldName())) {
            lead.setResponsibleUserId(newUserId);
        } else if ("originator_id".equals(detail.getFieldName())) {
            lead.setOriginatorId(newUserId);
        }
        leadRepository.updateById(lead);
    }

    private void executeTaskHandover(DataHandoverDetail detail, Long newUserId) {
        Task task = taskRepository.findById(detail.getDataId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        task.setAssigneeId(newUserId);
        // 更新执行人姓名
        User newUser = userRepository.findById(newUserId);
        if (newUser != null) {
            task.setAssigneeName(newUser.getRealName());
        }
        taskRepository.updateById(task);
    }

    private void executeMatterParticipantHandover(DataHandoverDetail detail, Long newUserId) {
        MatterParticipant participant = participantRepository.findById(detail.getDataId());
        if (participant == null) {
            throw new BusinessException("项目参与人记录不存在");
        }
        
        // 检查新用户是否已在团队中
        if (participantRepository.existsByMatterIdAndUserId(participant.getMatterId(), newUserId)) {
            // 如果新用户已在团队中，只需将原记录设为退出
            participant.setStatus("EXITED");
            participant.setExitDate(LocalDate.now());
        } else {
            // 更换用户
            participant.setUserId(newUserId);
        }
        participantRepository.updateById(participant);
    }

    private int createMatterHandoverDetails(DataHandover handover, Long fromUserId, Long toUserId, 
                                             Boolean includeOriginator) {
        int count = 0;
        
        // 1. 主办项目（进行中的）
        List<Matter> leadMatters = matterRepository.lambdaQuery()
                .eq(Matter::getLeadLawyerId, fromUserId)
                .eq(Matter::getDeleted, false)
                .notIn(Matter::getStatus, "ARCHIVED", "CLOSED")
                .list();
        
        for (Matter matter : leadMatters) {
            createDetail(handover.getId(), "MATTER", matter.getId(),
                    matter.getMatterNo(), matter.getName(),
                    "lead_lawyer_id", fromUserId.toString(), toUserId.toString());
            count++;
        }
        
        // 2. 案源人身份（可选）
        if (Boolean.TRUE.equals(includeOriginator)) {
            List<Matter> originatorMatters = matterRepository.lambdaQuery()
                    .eq(Matter::getOriginatorId, fromUserId)
                    .eq(Matter::getDeleted, false)
                    .notIn(Matter::getStatus, "ARCHIVED")
                    .list();
            
            for (Matter matter : originatorMatters) {
                createDetail(handover.getId(), "MATTER", matter.getId(),
                        matter.getMatterNo(), matter.getName() + " (案源人)",
                        "originator_id", fromUserId.toString(), toUserId.toString());
            }
        }
        
        // 3. 项目参与人（活跃的）
        List<MatterParticipant> participants = participantRepository.lambdaQuery()
                .eq(MatterParticipant::getUserId, fromUserId)
                .eq(MatterParticipant::getStatus, "ACTIVE")
                .eq(MatterParticipant::getDeleted, false)
                .list();
        
        for (MatterParticipant p : participants) {
            createDetail(handover.getId(), "MATTER_PARTICIPANT", p.getId(),
                    null, "项目参与 (ID:" + p.getMatterId() + ")",
                    "user_id", fromUserId.toString(), toUserId.toString());
        }
        
        return count;
    }

    private int createClientHandoverDetails(DataHandover handover, Long fromUserId, Long toUserId) {
        int count = 0;
        
        List<Client> clients = clientRepository.lambdaQuery()
                .eq(Client::getResponsibleLawyerId, fromUserId)
                .eq(Client::getDeleted, false)
                .list();
        
        for (Client client : clients) {
            createDetail(handover.getId(), "CLIENT", client.getId(),
                    client.getClientNo(), client.getName(),
                    "responsible_lawyer_id", fromUserId.toString(), toUserId.toString());
            count++;
        }
        
        return count;
    }

    private int createLeadHandoverDetails(DataHandover handover, Long fromUserId, Long toUserId) {
        int count = 0;
        
        List<Lead> leads = leadRepository.lambdaQuery()
                .eq(Lead::getResponsibleUserId, fromUserId)
                .eq(Lead::getDeleted, false)
                .notIn(Lead::getStatus, "CONVERTED", "ABANDONED")
                .list();
        
        for (Lead lead : leads) {
            createDetail(handover.getId(), "LEAD", lead.getId(),
                    lead.getLeadNo(), lead.getLeadName(),
                    "responsible_user_id", fromUserId.toString(), toUserId.toString());
            count++;
        }
        
        return count;
    }

    private int createTaskHandoverDetails(DataHandover handover, Long fromUserId, Long toUserId) {
        int count = 0;
        
        List<Task> tasks = taskRepository.findMyTodoTasks(fromUserId);
        
        for (Task task : tasks) {
            createDetail(handover.getId(), "TASK", task.getId(),
                    task.getTaskNo(), task.getTitle(),
                    "assignee_id", fromUserId.toString(), toUserId.toString());
            count++;
        }
        
        return count;
    }

    private void createDetail(Long handoverId, String dataType, Long dataId,
                               String dataNo, String dataName,
                               String fieldName, String oldValue, String newValue) {
        DataHandoverDetail detail = DataHandoverDetail.builder()
                .handoverId(handoverId)
                .dataType(dataType)
                .dataId(dataId)
                .dataNo(dataNo)
                .dataName(dataName)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .status("PENDING")
                .build();
        detailRepository.save(detail);
    }

    private String generateHandoverNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "HO" + datePart + random;
    }

    // ========== DTO转换 ==========

    private DataHandoverDTO toDTO(DataHandover handover) {
        DataHandoverDTO dto = new DataHandoverDTO();
        dto.setId(handover.getId());
        dto.setHandoverNo(handover.getHandoverNo());
        dto.setFromUserId(handover.getFromUserId());
        dto.setFromUsername(handover.getFromUsername());
        dto.setToUserId(handover.getToUserId());
        dto.setToUsername(handover.getToUsername());
        dto.setHandoverType(handover.getHandoverType());
        dto.setHandoverTypeName(getHandoverTypeName(handover.getHandoverType()));
        dto.setHandoverReason(handover.getHandoverReason());
        dto.setStatus(handover.getStatus());
        dto.setStatusName(getStatusName(handover.getStatus()));
        dto.setMatterCount(handover.getMatterCount());
        dto.setClientCount(handover.getClientCount());
        dto.setLeadCount(handover.getLeadCount());
        dto.setTaskCount(handover.getTaskCount());
        dto.setSubmittedBy(handover.getSubmittedBy());
        dto.setSubmittedAt(handover.getSubmittedAt());
        dto.setConfirmedBy(handover.getConfirmedBy());
        dto.setConfirmedAt(handover.getConfirmedAt());
        dto.setRemark(handover.getRemark());
        dto.setCreatedAt(handover.getCreatedAt());
        
        // 查询提交人姓名
        if (handover.getSubmittedBy() != null) {
            User submitter = userRepository.findById(handover.getSubmittedBy());
            if (submitter != null) {
                dto.setSubmittedByName(submitter.getRealName());
            }
        }
        
        // 查询确认人姓名
        if (handover.getConfirmedBy() != null) {
            User confirmer = userRepository.findById(handover.getConfirmedBy());
            if (confirmer != null) {
                dto.setConfirmedByName(confirmer.getRealName());
            }
        }
        
        return dto;
    }

    private DataHandoverDetailDTO toDetailDTO(DataHandoverDetail detail) {
        DataHandoverDetailDTO dto = new DataHandoverDetailDTO();
        dto.setId(detail.getId());
        dto.setHandoverId(detail.getHandoverId());
        dto.setDataType(detail.getDataType());
        dto.setDataTypeName(getDataTypeName(detail.getDataType()));
        dto.setDataId(detail.getDataId());
        dto.setDataNo(detail.getDataNo());
        dto.setDataName(detail.getDataName());
        dto.setFieldName(detail.getFieldName());
        dto.setFieldDisplayName(getFieldDisplayName(detail.getFieldName()));
        dto.setOldValue(detail.getOldValue());
        dto.setNewValue(detail.getNewValue());
        dto.setStatus(detail.getStatus());
        dto.setStatusName(getDetailStatusName(detail.getStatus()));
        dto.setErrorMessage(detail.getErrorMessage());
        dto.setExecutedAt(detail.getExecutedAt());
        dto.setCreatedAt(detail.getCreatedAt());
        
        // 查询用户姓名
        if (detail.getOldValue() != null) {
            try {
                User oldUser = userRepository.findById(Long.parseLong(detail.getOldValue()));
                if (oldUser != null) {
                    dto.setOldUserName(oldUser.getRealName());
                }
            } catch (NumberFormatException ignored) {}
        }
        
        if (detail.getNewValue() != null) {
            try {
                User newUser = userRepository.findById(Long.parseLong(detail.getNewValue()));
                if (newUser != null) {
                    dto.setNewUserName(newUser.getRealName());
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return dto;
    }

    private String getHandoverTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "RESIGNATION" -> "离职交接";
            case "PROJECT" -> "项目移交";
            case "CLIENT" -> "客户移交";
            case "LEAD" -> "案源移交";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING_APPROVAL" -> "待审批";
            case "APPROVED" -> "审批通过待执行";
            case "REJECTED" -> "已拒绝";
            case "CONFIRMED" -> "已确认";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getDataTypeName(String dataType) {
        if (dataType == null) return null;
        return switch (dataType) {
            case "MATTER" -> "项目";
            case "CLIENT" -> "客户";
            case "LEAD" -> "案源";
            case "TASK" -> "任务";
            case "MATTER_PARTICIPANT" -> "项目参与";
            case "CONTRACT_PARTICIPANT" -> "合同参与";
            default -> dataType;
        };
    }

    private String getFieldDisplayName(String fieldName) {
        if (fieldName == null) return null;
        return switch (fieldName) {
            case "lead_lawyer_id" -> "主办律师";
            case "responsible_lawyer_id" -> "负责律师";
            case "responsible_user_id" -> "负责人";
            case "originator_id" -> "案源人";
            case "assignee_id" -> "执行人";
            case "user_id" -> "用户";
            default -> fieldName;
        };
    }

    private String getDetailStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待处理";
            case "DONE" -> "已完成";
            case "FAILED" -> "失败";
            default -> status;
        };
    }
}

