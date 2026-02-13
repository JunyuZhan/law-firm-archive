package com.lawfirm.application.document.service;

import com.lawfirm.application.document.dto.MatterContextDTO;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.matter.repository.MatterParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 项目上下文信息收集服务 用于 AI 文书生成时收集项目相关的所有信息 支持选择性收集和文档内容提取. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterContextCollector {

  /** 项目仓储. */
  private final MatterRepository matterRepository;

  /** 项目客户关联仓储. */
  private final MatterClientRepository matterClientRepository;

  /** 项目参与人仓储. */
  private final MatterParticipantRepository matterParticipantRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** 文档Mapper. */
  private final DocumentMapper documentMapper;

  /** AI文档内容提取器. */
  private final AiDocumentContentExtractor contentExtractor;

  /** 收集选项. */
  @Data
  public static class CollectOptions {
    /** 是否收集项目基本信息. */
    private boolean includeMatterInfo = true;

    /** 是否收集客户信息. */
    private boolean includeClients = true;

    /** 是否收集参与人信息. */
    private boolean includeParticipants = true;

    /** 是否收集文档列表. */
    private boolean includeDocuments = false;

    /** 是否提取文档内容（需要先启用 includeDocuments）. */
    private boolean extractDocumentContent = false;

    /** 指定要收集的文档ID列表（为空则收集全部）. */
    private Set<Long> selectedDocumentIds;

    /**
     * 创建包含所有选项的配置。
     *
     * @return 包含所有选项的配置
     */
    public static CollectOptions all() {
      CollectOptions options = new CollectOptions();
      options.setIncludeDocuments(true);
      options.setExtractDocumentContent(true);
      return options;
    }

    /**
     * 创建基础配置（不包含文档）。
     *
     * @return 基础配置
     */
    public static CollectOptions basic() {
      return new CollectOptions();
    }

    /**
     * 创建包含文档的配置（不提取内容）。
     *
     * @return 包含文档的配置
     */
    public static CollectOptions withDocuments() {
      CollectOptions options = new CollectOptions();
      options.setIncludeDocuments(true);
      return options;
    }
  }

  /**
   * 收集项目完整上下文信息（使用默认选项）。
   *
   * @param matterId 项目ID
   * @param includeDocuments 是否包含文档
   * @return 项目上下文DTO
   */
  public MatterContextDTO collectContext(final Long matterId, final boolean includeDocuments) {
    CollectOptions options = new CollectOptions();
    options.setIncludeDocuments(includeDocuments);
    return collectContext(matterId, options);
  }

  /**
   * 收集项目上下文信息（使用自定义选项）。
   *
   * @param matterId 项目ID
   * @param options 收集选项
   * @return 项目上下文DTO
   */
  public MatterContextDTO collectContext(final Long matterId, final CollectOptions options) {
    log.info("开始收集项目上下文, matterId={}, options={}", matterId, options);

    MatterContextDTO context = new MatterContextDTO();

    // 1. 获取项目基本信息
    Matter matter = matterRepository.findById(matterId);
    if (matter == null) {
      log.warn("项目不存在: {}", matterId);
      return context;
    }

    if (options.isIncludeMatterInfo()) {
      context.setMatter(toMatterInfo(matter));
    }

    // 2. 获取客户信息
    if (options.isIncludeClients()) {
      List<MatterClient> matterClients = matterClientRepository.findByMatterId(matterId);
      if (matterClients != null && !matterClients.isEmpty()) {
        context.setClients(
            matterClients.stream().map(this::toClientInfo).collect(Collectors.toList()));
      } else if (matter.getClientId() != null) {
        Client client = clientRepository.findById(matter.getClientId());
        if (client != null) {
          MatterContextDTO.ClientInfo clientInfo = toClientInfo(client);
          clientInfo.setPrimary(true);
          clientInfo.setRole("委托人");
          context.setClients(List.of(clientInfo));
        }
      }
    }

    // 3. 获取参与人信息
    if (options.isIncludeParticipants()) {
      List<MatterParticipant> participants = matterParticipantRepository.findByMatterId(matterId);
      if (participants != null && !participants.isEmpty()) {
        context.setParticipants(
            participants.stream().map(this::toParticipantInfo).collect(Collectors.toList()));
      }
    }

    // 4. 获取相关文档
    if (options.isIncludeDocuments()) {
      List<Document> documents = documentMapper.selectByMatterId(matterId);
      if (documents != null && !documents.isEmpty()) {
        // 如果指定了文档ID，则只收集指定的文档
        if (options.getSelectedDocumentIds() != null
            && !options.getSelectedDocumentIds().isEmpty()) {
          documents =
              documents.stream()
                  .filter(d -> options.getSelectedDocumentIds().contains(d.getId()))
                  .collect(Collectors.toList());
        }

        context.setDocuments(
            documents.stream()
                .map(doc -> toDocumentInfo(doc, options.isExtractDocumentContent()))
                .collect(Collectors.toList()));
      }
    }

    log.info(
        "项目上下文收集完成: clients={}, participants={}, documents={}",
        context.getClients() != null ? context.getClients().size() : 0,
        context.getParticipants() != null ? context.getParticipants().size() : 0,
        context.getDocuments() != null ? context.getDocuments().size() : 0);

    return context;
  }

  /**
   * 获取项目下所有可选文档列表（供前端选择）。
   *
   * @param matterId 项目ID
   * @return 文档信息列表
   */
  public List<MatterContextDTO.DocumentInfo> getAvailableDocuments(final Long matterId) {
    List<Document> documents = documentMapper.selectByMatterId(matterId);
    if (documents == null || documents.isEmpty()) {
      return List.of();
    }

    return documents.stream()
        .map(
            doc -> {
              MatterContextDTO.DocumentInfo info = new MatterContextDTO.DocumentInfo();
              info.setId(doc.getId());
              info.setTitle(doc.getTitle());
              info.setFileName(doc.getFileName());
              info.setFileType(doc.getFileType());
              info.setCategory(doc.getFileCategory());
              info.setDescription(doc.getDescription());
              // 标记是否支持内容提取
              return info;
            })
        .collect(Collectors.toList());
  }

  /**
   * 判断文档是否支持内容提取。
   *
   * @param fileType 文件类型
   * @return 是否支持
   */
  public boolean isDocumentContentExtractable(final String fileType) {
    return contentExtractor.isSupported(fileType);
  }

  /**
   * 转换项目实体为项目信息DTO。
   *
   * @param matter 项目实体
   * @return 项目信息DTO
   */
  private MatterContextDTO.MatterInfo toMatterInfo(final Matter matter) {
    MatterContextDTO.MatterInfo info = new MatterContextDTO.MatterInfo();
    info.setId(matter.getId());
    info.setMatterNo(matter.getMatterNo());
    info.setName(matter.getName());
    info.setMatterType(MatterConstants.getMatterTypeName(matter.getMatterType()));
    info.setCaseType(MatterConstants.getCaseTypeName(matter.getCaseType()));
    info.setStatus(MatterConstants.getMatterStatusName(matter.getStatus()));
    info.setDescription(matter.getDescription());
    info.setOpposingParty(matter.getOpposingParty());
    info.setClaimAmount(
        matter.getClaimAmount() != null ? matter.getClaimAmount().toString() + "元" : null);
    return info;
  }

  /**
   * 转换 MatterClient 为 ClientInfo。
   *
   * @param mc MatterClient实体
   * @return 客户信息DTO
   */
  private MatterContextDTO.ClientInfo toClientInfo(final MatterClient mc) {
    MatterContextDTO.ClientInfo info = new MatterContextDTO.ClientInfo();
    info.setPrimary(Boolean.TRUE.equals(mc.getIsPrimary()));
    info.setRole(getClientRoleName(mc.getClientRole()));

    if (mc.getClientId() != null) {
      Client client = clientRepository.findById(mc.getClientId());
      if (client != null) {
        copyClientInfo(client, info);
      }
    }
    return info;
  }

  /**
   * 转换 Client 为 ClientInfo。
   *
   * @param client 客户实体
   * @return 客户信息DTO
   */
  private MatterContextDTO.ClientInfo toClientInfo(final Client client) {
    MatterContextDTO.ClientInfo info = new MatterContextDTO.ClientInfo();
    copyClientInfo(client, info);
    return info;
  }

  /**
   * 复制客户信息到 DTO。
   *
   * @param client 客户实体
   * @param info 客户信息DTO
   */
  private void copyClientInfo(final Client client, final MatterContextDTO.ClientInfo info) {
    info.setId(client.getId());
    info.setName(client.getName());
    info.setClientType(client.getClientType());
    info.setCreditCode(client.getCreditCode());
    info.setIdCard(client.getIdCard());
    info.setLegalRepresentative(client.getLegalRepresentative());
    info.setContactPerson(client.getContactPerson());
    info.setContactPhone(client.getContactPhone());
    info.setContactEmail(client.getContactEmail());
    info.setRegisteredAddress(client.getRegisteredAddress());
  }

  /**
   * 转换参与人为 ParticipantInfo。
   *
   * @param p 参与人实体
   * @return 参与人信息DTO
   */
  private MatterContextDTO.ParticipantInfo toParticipantInfo(final MatterParticipant p) {
    MatterContextDTO.ParticipantInfo info = new MatterContextDTO.ParticipantInfo();
    info.setUserId(p.getUserId());
    info.setRole(getParticipantRoleName(p.getRole()));

    if (p.getUserId() != null) {
      User user = userRepository.findById(p.getUserId());
      if (user != null) {
        info.setName(user.getRealName());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setLawyerLicenseNo(user.getLawyerLicenseNo());
      }
    }
    return info;
  }

  /**
   * 转换文档为 DocumentInfo。
   *
   * @param doc 文档实体
   * @param extractContent 是否提取内容
   * @return 文档信息DTO
   */
  private MatterContextDTO.DocumentInfo toDocumentInfo(
      final Document doc, final boolean extractContent) {
    MatterContextDTO.DocumentInfo info = new MatterContextDTO.DocumentInfo();
    info.setId(doc.getId());
    info.setTitle(doc.getTitle());
    info.setFileName(doc.getFileName());
    info.setFileType(doc.getFileType());
    info.setCategory(doc.getFileCategory());
    info.setDescription(doc.getDescription());

    // 提取文档内容
    if (extractContent && contentExtractor.isSupported(doc.getFileType())) {
      try {
        String content = contentExtractor.extractContent(doc);
        if (content != null && !content.isEmpty()) {
          info.setContent(content);
          log.debug("成功提取文档内容: {} ({}字符)", doc.getFileName(), content.length());
        }
      } catch (Exception e) {
        log.warn("提取文档内容失败: {} - {}", doc.getFileName(), e.getMessage());
      }
    }

    return info;
  }

  // ========== 常量映射方法 ==========
  // 已迁移到 MatterConstants，此处保留注释以便追溯

  /**
   * 获取客户角色名称。
   *
   * @param role 角色代码
   * @return 角色名称
   */
  private String getClientRoleName(final String role) {
    if (role == null) {
      return "委托人";
    }
    return switch (role) {
      case "PLAINTIFF" -> "原告";
      case "DEFENDANT" -> "被告";
      case "APPLICANT" -> "申请人";
      case "RESPONDENT" -> "被申请人";
      case "THIRD_PARTY" -> "第三人";
      case "GUARANTOR" -> "担保人";
      case "OTHER" -> "其他";
      default -> role;
    };
  }

  /**
   * 获取参与人角色名称。
   *
   * @param role 角色代码
   * @return 角色名称
   */
  private String getParticipantRoleName(final String role) {
    if (role == null) {
      return "律师";
    }
    return switch (role) {
      case "LEAD" -> "主办律师";
      case "ASSOCIATE" -> "协办律师";
      case "ASSISTANT" -> "律师助理";
      case "PARALEGAL" -> "法务助理";
      case "ORIGINATOR" -> "案源人";
      default -> role;
    };
  }
}
