package com.lawfirm.application.openapi.service;

import com.lawfirm.application.openapi.dto.PortalMatterDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.matter.entity.*;
import com.lawfirm.domain.openapi.entity.ClientAccessToken;
import com.lawfirm.domain.openapi.entity.MatterShareConfig;
import com.lawfirm.domain.openapi.entity.OpenApiAccessLog;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import com.lawfirm.infrastructure.persistence.mapper.PaymentMapper;
import com.lawfirm.infrastructure.persistence.mapper.TaskMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.ClientAccessTokenMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.MatterShareConfigMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.OpenApiAccessLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户门户数据服务
 * 为客户门户提供脱敏后的项目信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalDataService {

    private final ClientAccessTokenMapper tokenMapper;
    private final MatterShareConfigMapper shareConfigMapper;
    private final OpenApiAccessLogMapper accessLogMapper;
    private final MatterMapper matterMapper;
    private final ClientMapper clientMapper;
    private final TaskMapper taskMapper;
    private final DeadlineMapper deadlineMapper;
    private final MatterParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final FinanceContractMapper financeContractMapper;
    private final PaymentMapper paymentMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 构建项目数据（供推送服务使用）
     * 不需要令牌验证，由调用方保证权限
     */
    public PortalMatterDTO buildMatterData(Long matterId, Set<String> scopes) {
        Matter matter = matterMapper.selectById(matterId);
        if (matter == null || matter.getDeleted()) {
            throw new BusinessException("项目不存在");
        }
        
        PortalMatterDTO dto = new PortalMatterDTO();
        dto.setMatterId(matter.getId());
        
        // 根据 scopes 构建数据
        if (scopes.contains("MATTER_INFO")) {
            dto.setMatterName(matter.getName());
            dto.setMatterNo(matter.getMatterNo());
            dto.setMatterType(matter.getMatterType());
            dto.setMatterTypeName(getMatterTypeName(matter.getMatterType()));
            dto.setStatus(matter.getStatus());
            dto.setStatusName(getMatterStatusName(matter.getStatus()));
        }
        
        if (scopes.contains("MATTER_PROGRESS")) {
            dto.setCurrentPhase(matter.getCurrentPhase());
            dto.setCurrentPhaseName(getPhaseName(matter.getCurrentPhase()));
            dto.setProgress(matter.getProgress());
            dto.setLastUpdateTime(matter.getUpdatedAt() != null ? 
                matter.getUpdatedAt().format(DATE_TIME_FORMATTER) : null);
        }
        
        if (scopes.contains("LAWYER_INFO")) {
            dto.setLawyerList(getLawyerList(matter.getId()));
        }
        
        if (scopes.contains("TASK_LIST")) {
            dto.setTaskList(getTaskList(matter.getId()));
        }
        
        if (scopes.contains("DEADLINE_INFO")) {
            dto.setDeadlineList(getDeadlines(matter.getId()));
        }
        
        if (scopes.contains("FEE_INFO")) {
            setFeeInfo(dto, matter);
        }
        
        return dto;
    }

    /**
     * 验证令牌并获取项目信息
     */
    @Transactional
    public PortalMatterDTO getMatterInfo(String token, String clientIp, String userAgent) {
        // 验证令牌
        ClientAccessToken accessToken = validateToken(token, clientIp);

        // 记录访问日志
        Long matterId = accessToken.getMatterId();
        logAccess(accessToken, "/portal/matter", "GET", clientIp, userAgent, OpenApiAccessLog.RESULT_SUCCESS, null);

        // 更新访问信息
        tokenMapper.updateAccessInfo(accessToken.getId(), clientIp, LocalDateTime.now());

        // 获取项目信息
        if (matterId == null) {
            throw new BusinessException("该令牌未绑定具体项目");
        }

        Matter matter = matterMapper.selectById(matterId);
        if (matter == null || matter.getDeleted()) {
            throw new BusinessException("项目不存在");
        }

        // 获取共享配置
        MatterShareConfig shareConfig = shareConfigMapper.selectByMatterId(matterId);

        // 构建返回数据
        return buildPortalMatterDTO(matter, accessToken, shareConfig);
    }

    /**
     * 验证令牌
     */
    public ClientAccessToken validateToken(String token, String clientIp) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException("令牌不能为空");
        }

        ClientAccessToken accessToken = tokenMapper.selectByToken(token);
        if (accessToken == null) {
            throw new BusinessException("无效的访问令牌");
        }

        // 检查令牌状态
        if (!accessToken.isValid()) {
            String reason = "令牌已失效";
            if (ClientAccessToken.STATUS_REVOKED.equals(accessToken.getStatus())) {
                reason = "令牌已被撤销";
            } else if (ClientAccessToken.STATUS_EXPIRED.equals(accessToken.getStatus())) {
                reason = "令牌已过期";
            } else if (accessToken.getExpiresAt() != null && LocalDateTime.now().isAfter(accessToken.getExpiresAt())) {
                reason = "令牌已过期";
            } else if (accessToken.getMaxAccessCount() != null && accessToken.getAccessCount() >= accessToken.getMaxAccessCount()) {
                reason = "令牌访问次数已达上限";
            }
            throw new BusinessException(reason);
        }

        // 检查 IP 白名单
        if (!accessToken.isIpAllowed(clientIp)) {
            log.warn("【OpenAPI】IP不在白名单中: token={}, ip={}, whitelist={}",
                    accessToken.getId(), clientIp, accessToken.getIpWhitelist());
            throw new BusinessException("访问IP不在允许范围内");
        }

        return accessToken;
    }

    /**
     * 记录访问日志
     */
    @Transactional
    public void logAccess(ClientAccessToken token, String path, String method, String clientIp,
                          String userAgent, String result, String errorMessage) {
        OpenApiAccessLog accessLog = OpenApiAccessLog.builder()
                .tokenId(token.getId())
                .clientId(token.getClientId())
                .matterId(token.getMatterId())
                .requestPath(path)
                .requestMethod(method)
                .clientIp(clientIp)
                .userAgent(truncate(userAgent, 500))
                .accessResult(result)
                .errorMessage(errorMessage)
                .accessAt(LocalDateTime.now())
                .build();

        accessLogMapper.insert(accessLog);
    }

    // ========== 私有方法 ==========

    /**
     * 构建门户项目 DTO
     */
    private PortalMatterDTO buildPortalMatterDTO(Matter matter, ClientAccessToken token, MatterShareConfig config) {
        PortalMatterDTO dto = new PortalMatterDTO();

        // 基本信息（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_MATTER_INFO)) {
            if (config == null || config.getShareBasicInfo()) {
                dto.setMatterNo(matter.getMatterNo());
                dto.setName(matter.getName());
                dto.setMatterType(matter.getMatterType());
                dto.setMatterTypeName(getMatterTypeName(matter.getMatterType()));
                dto.setCaseType(matter.getCaseType());
                dto.setCaseTypeName(getCaseTypeName(matter.getCaseType()));
                dto.setStatus(matter.getStatus());
                dto.setStatusName(getStatusName(matter.getStatus()));
                dto.setFilingDate(matter.getFilingDate());
                dto.setExpectedClosingDate(matter.getExpectedClosingDate());
            }
        }

        // 进度信息（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_MATTER_PROGRESS)) {
            if (config == null || config.getShareProgress()) {
                dto.setCurrentPhase(getCurrentPhase(matter));
                dto.setCurrentPhaseName(getPhaseName(dto.getCurrentPhase()));
                dto.setOverallProgress(calculateProgress(matter));
                dto.setLastUpdateTime(matter.getUpdatedAt() != null ? matter.getUpdatedAt().format(DATE_TIME_FORMATTER) : null);
            }
        }

        // 团队信息（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_LAWYER_INFO)) {
            if (config == null || config.getShareTeamInfo()) {
                dto.setTeamMembers(getTeamMembers(matter.getId()));
            }
        }

        // 任务列表（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_TASK_LIST)) {
            if (config != null && config.getShareTaskList()) {
                dto.setTasks(getTaskSummaries(matter.getId()));
            }
        }

        // 期限信息（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_DEADLINE_INFO)) {
            if (config == null || config.getShareDeadline()) {
                dto.setDeadlines(getDeadlines(matter.getId()));
            }
        }

        // 费用信息（如果授权）
        if (token.hasScope(ClientAccessToken.SCOPE_FEE_INFO)) {
            if (config != null && config.getShareFeeInfo()) {
                setFeeInfo(dto, matter);
            }
        }

        return dto;
    }

    /**
     * 获取团队成员
     */
    private List<PortalMatterDTO.TeamMemberDTO> getTeamMembers(Long matterId) {
        List<MatterParticipant> participants = participantMapper.selectByMatterId(matterId);
        List<PortalMatterDTO.TeamMemberDTO> members = new ArrayList<>();

        for (MatterParticipant p : participants) {
            if (!"ACTIVE".equals(p.getStatus())) continue;

            User user = userMapper.selectById(p.getUserId());
            if (user == null) continue;

            PortalMatterDTO.TeamMemberDTO member = new PortalMatterDTO.TeamMemberDTO();
            member.setName(user.getRealName());
            member.setRole(p.getRole());
            member.setRoleName(getRoleName(p.getRole()));
            // 脱敏处理
            member.setPhone(maskPhone(user.getPhone()));
            member.setEmail(maskEmail(user.getEmail()));
            members.add(member);
        }

        return members;
    }

    /**
     * 获取任务摘要
     */
    private List<PortalMatterDTO.TaskSummaryDTO> getTaskSummaries(Long matterId) {
        List<Task> tasks = taskMapper.selectByMatterId(matterId);
        return tasks.stream()
                .filter(t -> !t.getDeleted())
                .map(t -> {
                    PortalMatterDTO.TaskSummaryDTO dto = new PortalMatterDTO.TaskSummaryDTO();
                    dto.setTitle(t.getTitle());
                    dto.setStatus(t.getStatus());
                    dto.setStatusName(getTaskStatusName(t.getStatus()));
                    dto.setProgress(t.getProgress());
                    dto.setDueDate(t.getDueDate());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取期限列表
     */
    private List<PortalMatterDTO.DeadlineDTO> getDeadlines(Long matterId) {
        List<Deadline> deadlines = deadlineMapper.selectByMatterId(matterId);
        return deadlines.stream()
                .filter(d -> !d.getDeleted())
                .map(d -> {
                    PortalMatterDTO.DeadlineDTO dto = new PortalMatterDTO.DeadlineDTO();
                    dto.setName(d.getDeadlineName());
                    dto.setType(d.getDeadlineType());
                    dto.setDeadline(d.getDeadlineDate());
                    dto.setStatus(d.getStatus());
                    dto.setStatusName(getDeadlineStatusName(d.getStatus()));
                    if (d.getDeadlineDate() != null) {
                        dto.setRemainingDays((int) ChronoUnit.DAYS.between(LocalDate.now(), d.getDeadlineDate()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 设置费用信息
     */
    private void setFeeInfo(PortalMatterDTO dto, Matter matter) {
        if (matter.getContractId() != null) {
            Contract contract = financeContractMapper.selectById(matter.getContractId());
            if (contract != null) {
                dto.setContractAmount(contract.getTotalAmount());
            }
        }

        // 计算已收款和待收款
        BigDecimal received = paymentMapper.sumReceivedByMatterId(matter.getId());
        dto.setReceivedAmount(received != null ? received : BigDecimal.ZERO);

        if (dto.getContractAmount() != null) {
            dto.setPendingAmount(dto.getContractAmount().subtract(dto.getReceivedAmount()));
        }
    }

    // ========== 工具方法 ==========

    private String getMatterTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "LITIGATION" -> "诉讼案件";
            case "NON_LITIGATION" -> "非诉项目";
            default -> type;
        };
    }

    private String getCaseTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "CIVIL" -> "民事";
            case "CRIMINAL" -> "刑事";
            case "ADMINISTRATIVE" -> "行政";
            case "BANKRUPTCY" -> "破产";
            case "IP" -> "知识产权";
            case "ARBITRATION" -> "仲裁";
            case "ENFORCEMENT" -> "执行";
            case "LEGAL_COUNSEL" -> "法律顾问";
            case "SPECIAL_SERVICE" -> "专项服务";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "ACTIVE" -> "进行中";
            case "SUSPENDED" -> "暂停";
            case "CLOSED" -> "已结案";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
    }

    private String getRoleName(String role) {
        if (role == null) return "";
        return switch (role) {
            case "LEAD" -> "主办律师";
            case "CO_COUNSEL" -> "协办律师";
            case "PARALEGAL" -> "律师助理";
            case "TRAINEE" -> "实习律师";
            default -> role;
        };
    }

    private String getTaskStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "TODO" -> "待办";
            case "IN_PROGRESS" -> "进行中";
            case "DONE" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getDeadlineStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待处理";
            case "COMPLETED" -> "已完成";
            case "OVERDUE" -> "已逾期";
            default -> status;
        };
    }

    private String getCurrentPhase(Matter matter) {
        // 根据项目状态判断当前阶段
        if ("DRAFT".equals(matter.getStatus()) || "PENDING".equals(matter.getStatus())) {
            return "PREPARATION";
        } else if ("ACTIVE".equals(matter.getStatus())) {
            return "PROCESSING";
        } else if ("CLOSED".equals(matter.getStatus()) || "ARCHIVED".equals(matter.getStatus())) {
            return "COMPLETED";
        }
        return "UNKNOWN";
    }

    private String getPhaseName(String phase) {
        if (phase == null) return "";
        return switch (phase) {
            case "PREPARATION" -> "准备阶段";
            case "PROCESSING" -> "办理中";
            case "COMPLETED" -> "已完成";
            default -> phase;
        };
    }

    private Integer calculateProgress(Matter matter) {
        // 简单的进度计算逻辑
        return switch (matter.getStatus()) {
            case "DRAFT" -> 5;
            case "PENDING" -> 10;
            case "ACTIVE" -> 50;
            case "SUSPENDED" -> 50;
            case "CLOSED" -> 100;
            case "ARCHIVED" -> 100;
            default -> 0;
        };
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}

