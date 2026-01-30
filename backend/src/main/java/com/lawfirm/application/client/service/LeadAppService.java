package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.ConvertLeadCommand;
import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.application.client.command.CreateFollowUpCommand;
import com.lawfirm.application.client.command.CreateLeadCommand;
import com.lawfirm.application.client.command.UpdateLeadCommand;
import com.lawfirm.application.client.dto.ClientDTO;
import com.lawfirm.application.client.dto.LeadDTO;
import com.lawfirm.application.client.dto.LeadFollowUpDTO;
import com.lawfirm.application.client.dto.LeadQueryDTO;
import com.lawfirm.application.client.dto.LeadStatisticsDTO;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.service.MatterAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Lead;
import com.lawfirm.domain.client.entity.LeadFollowUp;
import com.lawfirm.domain.client.repository.LeadFollowUpRepository;
import com.lawfirm.domain.client.repository.LeadRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.LeadMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 案源应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadAppService {

  /** 案源仓储. */
  private final LeadRepository leadRepository;

  /** 案源跟进记录仓储. */
  private final LeadFollowUpRepository leadFollowUpRepository;

  /** 案源Mapper. */
  private final LeadMapper leadMapper;

  /** 客户应用服务. */
  private final ClientAppService clientAppService;

  /** 项目应用服务. */
  private final MatterAppService matterAppService;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /**
   * 分页查询案源列表 数据权限：只能查看自己的案源（我创建的案源）
   *
   * @param query 查询条件
   * @return 案源分页结果
   */
  public PageResult<LeadDTO> listLeads(final LeadQueryDTO query) {
    Long currentUserId = SecurityUtils.getUserId();

    // 只查询当前用户创建的案源
    List<Long> myUserIds = Collections.singletonList(currentUserId);

    List<Lead> leads =
        leadMapper.selectLeadPage(
            query.getLeadName(),
            query.getStatus(),
            query.getOriginatorId(),
            query.getResponsibleUserId(),
            query.getSourceChannel(),
            myUserIds, // 过滤：我是负责用户
            myUserIds // 过滤：我是案源人
            );

    // 手动分页
    int offset = query.getOffset();
    int limit = query.getPageSize();
    int total = leads.size();
    List<Lead> pagedLeads = leads.stream().skip(offset).limit(limit).collect(Collectors.toList());

    List<LeadDTO> records = pagedLeads.stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, total, query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取案源详情
   *
   * @param id 案源ID
   * @return 案源DTO
   */
  public LeadDTO getLead(final Long id) {
    Lead lead = leadRepository.findById(id);
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }
    return toDTO(lead);
  }

  /**
   * 创建案源
   *
   * @param command 创建案源命令
   * @return 案源DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public LeadDTO createLead(final CreateLeadCommand command) {
    // 生成案源编号
    String leadNo = generateLeadNo();

    Lead lead =
        Lead.builder()
            .leadNo(leadNo)
            .leadName(command.getLeadName())
            .leadType(command.getLeadType())
            .contactName(command.getContactName())
            .contactPhone(command.getContactPhone())
            .contactEmail(command.getContactEmail())
            .sourceChannel(command.getSourceChannel())
            .sourceDetail(command.getSourceDetail())
            .status("PENDING")
            .priority(command.getPriority() != null ? command.getPriority() : "NORMAL")
            .businessType(command.getBusinessType())
            .estimatedAmount(command.getEstimatedAmount())
            .description(command.getDescription())
            .nextFollowTime(command.getNextFollowTime())
            .originatorId(command.getOriginatorId())
            .responsibleUserId(
                command.getResponsibleUserId() != null
                    ? command.getResponsibleUserId()
                    : SecurityUtils.getUserId())
            .followCount(0)
            .remark(command.getRemark())
            .createdBy(SecurityUtils.getUserId())
            .createdAt(LocalDateTime.now())
            .build();

    leadRepository.getBaseMapper().insert(lead);
    log.info("创建案源: leadNo={}, leadName={}", leadNo, command.getLeadName());

    return toDTO(lead);
  }

  /**
   * 更新案源
   *
   * @param id 案源ID
   * @param command 更新案源命令
   * @return 案源DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public LeadDTO updateLead(final Long id, final UpdateLeadCommand command) {
    Lead lead = leadRepository.findById(id);
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }

    if ("CONVERTED".equals(lead.getStatus())) {
      throw new BusinessException("已转化的案源不能修改");
    }

    if (command.getLeadName() != null) {
      lead.setLeadName(command.getLeadName());
    }
    if (command.getLeadType() != null) {
      lead.setLeadType(command.getLeadType());
    }
    if (command.getContactName() != null) {
      lead.setContactName(command.getContactName());
    }
    if (command.getContactPhone() != null) {
      lead.setContactPhone(command.getContactPhone());
    }
    if (command.getContactEmail() != null) {
      lead.setContactEmail(command.getContactEmail());
    }
    if (command.getSourceChannel() != null) {
      lead.setSourceChannel(command.getSourceChannel());
    }
    if (command.getSourceDetail() != null) {
      lead.setSourceDetail(command.getSourceDetail());
    }
    if (command.getStatus() != null) {
      lead.setStatus(command.getStatus());
    }
    if (command.getPriority() != null) {
      lead.setPriority(command.getPriority());
    }
    if (command.getBusinessType() != null) {
      lead.setBusinessType(command.getBusinessType());
    }
    if (command.getEstimatedAmount() != null) {
      lead.setEstimatedAmount(command.getEstimatedAmount());
    }
    if (command.getDescription() != null) {
      lead.setDescription(command.getDescription());
    }
    if (command.getNextFollowTime() != null) {
      lead.setNextFollowTime(command.getNextFollowTime());
    }
    if (command.getResponsibleUserId() != null) {
      lead.setResponsibleUserId(command.getResponsibleUserId());
    }
    if (command.getRemark() != null) {
      lead.setRemark(command.getRemark());
    }

    lead.setUpdatedBy(SecurityUtils.getUserId());
    lead.setUpdatedAt(LocalDateTime.now());
    leadRepository.getBaseMapper().updateById(lead);

    return toDTO(lead);
  }

  /**
   * 删除案源
   *
   * @param id 案源ID
   */
  @Transactional(rollbackFor = Exception.class)
  public void deleteLead(final Long id) {
    Lead lead = leadRepository.findById(id);
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }

    if ("CONVERTED".equals(lead.getStatus())) {
      throw new BusinessException("已转化的案源不能删除");
    }

    leadRepository.softDelete(id);
  }

  /**
   * 创建跟进记录
   *
   * @param command 创建跟进记录命令
   * @return 跟进记录DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public LeadFollowUpDTO createFollowUp(final CreateFollowUpCommand command) {
    Lead lead = leadRepository.findById(command.getLeadId());
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }

    if ("CONVERTED".equals(lead.getStatus()) || "ABANDONED".equals(lead.getStatus())) {
      throw new BusinessException("已转化或已放弃的案源不能跟进");
    }

    // 创建跟进记录
    LeadFollowUp followUp =
        LeadFollowUp.builder()
            .leadId(command.getLeadId())
            .followType(command.getFollowType())
            .followContent(command.getFollowContent())
            .followResult(command.getFollowResult())
            .nextFollowTime(command.getNextFollowTime())
            .nextFollowPlan(command.getNextFollowPlan())
            .followUserId(SecurityUtils.getUserId())
            .createdAt(LocalDateTime.now())
            .createdBy(SecurityUtils.getUserId())
            .build();

    leadFollowUpRepository.getBaseMapper().insert(followUp);

    // 更新案源的跟进信息
    lead.setLastFollowTime(LocalDateTime.now());
    lead.setNextFollowTime(command.getNextFollowTime());
    lead.setFollowCount(lead.getFollowCount() != null ? lead.getFollowCount() + 1 : 1);
    if (!"FOLLOWING".equals(lead.getStatus())) {
      lead.setStatus("FOLLOWING");
    }
    lead.setUpdatedAt(LocalDateTime.now());
    leadRepository.getBaseMapper().updateById(lead);

    log.info("创建案源跟进记录: leadId={}, followType={}", command.getLeadId(), command.getFollowType());

    return toFollowUpDTO(followUp);
  }

  /**
   * 查询案源的跟进记录
   *
   * @param leadId 案源ID
   * @return 跟进记录列表
   */
  public List<LeadFollowUpDTO> listFollowUps(final Long leadId) {
    List<LeadFollowUp> followUps = leadFollowUpRepository.findByLeadId(leadId);
    return followUps.stream().map(this::toFollowUpDTO).collect(Collectors.toList());
  }

  /**
   * 案源转化
   *
   * @param command 转化案源命令
   * @return 案源DTO
   */
  @Transactional(rollbackFor = Exception.class)
  public LeadDTO convertLead(final ConvertLeadCommand command) {
    Lead lead = leadRepository.findById(command.getLeadId());
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }

    if ("CONVERTED".equals(lead.getStatus())) {
      throw new BusinessException("案源已转化");
    }

    Long clientId = null;
    Long matterId = null;

    // 1. 处理客户
    if (Boolean.TRUE.equals(command.getCreateNewClient())) {
      // 创建新客户
      CreateClientCommand createClientCmd = new CreateClientCommand();
      createClientCmd.setName(command.getClientName());
      createClientCmd.setClientType(command.getClientType());
      createClientCmd.setContactPhone(command.getContactPhone());
      createClientCmd.setContactEmail(command.getContactEmail());
      createClientCmd.setSource(lead.getSourceChannel());
      createClientCmd.setOriginatorId(lead.getOriginatorId());

      ClientDTO client = clientAppService.createClient(createClientCmd);
      clientId = client.getId();
    } else {
      clientId = command.getClientId();
      if (clientId == null) {
        throw new BusinessException("客户ID不能为空");
      }
    }

    // 2. 处理项目（可选）
    if (Boolean.TRUE.equals(command.getCreateMatter())) {
      CreateMatterCommand createMatterCmd = new CreateMatterCommand();
      createMatterCmd.setName(command.getMatterName());
      createMatterCmd.setMatterType(command.getMatterType());
      createMatterCmd.setBusinessType(command.getBusinessType());
      createMatterCmd.setClientId(clientId);
      createMatterCmd.setLeadLawyerId(command.getLeadLawyerId());

      MatterDTO matter = matterAppService.createMatter(createMatterCmd);
      matterId = matter.getId();
    }

    // 3. 更新案源状态
    lead.setStatus("CONVERTED");
    lead.setConvertedAt(LocalDateTime.now());
    lead.setConvertedToClientId(clientId);
    lead.setConvertedToMatterId(matterId);
    lead.setUpdatedAt(LocalDateTime.now());
    lead.setUpdatedBy(SecurityUtils.getUserId());
    leadRepository.getBaseMapper().updateById(lead);

    log.info(
        "案源转化成功: leadId={}, clientId={}, matterId={}", command.getLeadId(), clientId, matterId);

    return toDTO(lead);
  }

  /**
   * 放弃案源
   *
   * @param id 案源ID
   * @param reason 放弃原因
   */
  @Transactional(rollbackFor = Exception.class)
  public void abandonLead(final Long id, final String reason) {
    Lead lead = leadRepository.findById(id);
    if (lead == null) {
      throw new BusinessException("案源不存在");
    }

    if ("CONVERTED".equals(lead.getStatus())) {
      throw new BusinessException("已转化的案源不能放弃");
    }

    lead.setStatus("ABANDONED");
    if (reason != null) {
      lead.setRemark((lead.getRemark() != null ? lead.getRemark() + "\n" : "") + "放弃原因: " + reason);
    }
    lead.setUpdatedAt(LocalDateTime.now());
    lead.setUpdatedBy(SecurityUtils.getUserId());
    leadRepository.getBaseMapper().updateById(lead);
  }

  // ========== 工具方法 ==========

  /**
   * 生成案源编号
   *
   * @return 案源编号
   */
  private String generateLeadNo() {
    String prefix = "LD";
    String date =
        LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    return prefix + date + random;
  }

  /**
   * 转换为DTO
   *
   * @param lead 案源实体
   * @return 案源DTO
   */
  private LeadDTO toDTO(final Lead lead) {
    LeadDTO dto = new LeadDTO();
    BeanUtils.copyProperties(lead, dto);

    // 查询关联信息
    if (lead.getOriginatorId() != null) {
      User originator = userRepository.findById(lead.getOriginatorId());
      if (originator != null) {
        dto.setOriginatorName(originator.getRealName());
      }
    }

    if (lead.getResponsibleUserId() != null) {
      User responsible = userRepository.findById(lead.getResponsibleUserId());
      if (responsible != null) {
        dto.setResponsibleUserName(responsible.getRealName());
      }
    }

    // 查询转化后的客户名称
    if (lead.getConvertedToClientId() != null) {
      try {
        ClientDTO client = clientAppService.getClientById(lead.getConvertedToClientId());
        if (client != null) {
          dto.setConvertedToClientName(client.getName());
        }
      } catch (Exception e) {
        log.debug("获取转化客户信息失败: clientId={}", lead.getConvertedToClientId());
      }
    }

    // 查询转化后的项目名称
    if (lead.getConvertedToMatterId() != null) {
      try {
        MatterDTO matter = matterAppService.getMatterById(lead.getConvertedToMatterId());
        if (matter != null) {
          dto.setConvertedToMatterName(matter.getName());
        }
      } catch (Exception e) {
        log.debug("获取转化项目信息失败: matterId={}", lead.getConvertedToMatterId());
      }
    }

    return dto;
  }

  /**
   * 转换为跟进记录DTO
   *
   * @param followUp 跟进记录实体
   * @return 跟进记录DTO
   */
  private LeadFollowUpDTO toFollowUpDTO(final LeadFollowUp followUp) {
    LeadFollowUpDTO dto = new LeadFollowUpDTO();
    BeanUtils.copyProperties(followUp, dto);

    if (followUp.getFollowUserId() != null) {
      User user = userRepository.findById(followUp.getFollowUserId());
      if (user != null) {
        dto.setFollowUserName(user.getRealName());
      }
    }

    return dto;
  }

  /**
   * 获取案源统计（M2-033~M2-034）
   *
   * @return 案源统计数据
   */
  public LeadStatisticsDTO getLeadStatistics() {
    LeadStatisticsDTO statistics = new LeadStatisticsDTO();

    // 总案源数
    Long totalLeads = leadMapper.countTotalLeads();
    statistics.setTotalLeads(totalLeads != null ? totalLeads : 0L);

    // 已转化案源数
    Long convertedLeads = leadMapper.countConvertedLeads();
    statistics.setConvertedLeads(convertedLeads != null ? convertedLeads : 0L);

    // 计算转化率
    if (totalLeads != null && totalLeads > 0) {
      double conversionRate =
          (convertedLeads != null ? convertedLeads.doubleValue() : 0.0) / totalLeads * 100;
      statistics.setConversionRate(conversionRate);
    } else {
      statistics.setConversionRate(0.0);
    }

    // 按来源渠道统计（M2-033）
    List<Map<String, Object>> bySourceChannel = leadMapper.countBySourceChannel();
    statistics.setBySourceChannel(bySourceChannel);

    // 按状态统计
    List<Map<String, Object>> byStatus = leadMapper.countByStatus();
    statistics.setByStatus(byStatus);

    // 按案源人统计
    List<Map<String, Object>> byOriginator = leadMapper.countByOriginator();
    statistics.setByOriginator(byOriginator);

    // 转化率分析（M2-034）
    List<Map<String, Object>> conversionAnalysis = leadMapper.analyzeConversionRate();
    statistics.setConversionAnalysis(conversionAnalysis);

    // 按时间统计转化趋势
    List<Map<String, Object>> conversionTrend = leadMapper.countConversionTrend();
    statistics.setConversionTrend(conversionTrend);

    log.info(
        "获取案源统计: total={}, converted={}, rate={}%",
        totalLeads, convertedLeads, statistics.getConversionRate());
    return statistics;
  }
}
