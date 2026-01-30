package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.application.client.command.AssignClientTagsCommand;
import com.lawfirm.application.client.command.CreateClientTagCommand;
import com.lawfirm.application.client.command.UpdateClientTagCommand;
import com.lawfirm.application.client.dto.ClientTagDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.ClientTag;
import com.lawfirm.domain.client.entity.ClientTagRelation;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ClientTagRepository;
import com.lawfirm.infrastructure.persistence.mapper.ClientTagRelationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 客户标签应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientTagAppService {

  /** 客户标签仓储. */
  private final ClientTagRepository clientTagRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /** 客户标签关系Mapper. */
  private final ClientTagRelationMapper clientTagRelationMapper;

  /**
   * 查询所有标签
   *
   * @return 标签列表
   */
  public List<ClientTagDTO> listTags() {
    List<ClientTag> tags =
        clientTagRepository.list(
            new LambdaQueryWrapper<ClientTag>()
                .eq(ClientTag::getDeleted, false)
                .orderByAsc(ClientTag::getSortOrder)
                .orderByAsc(ClientTag::getId));
    return tags.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 根据ID查询标签
   *
   * @param id 标签ID
   * @return 标签DTO
   */
  public ClientTagDTO getTagById(final Long id) {
    ClientTag tag = clientTagRepository.getByIdOrThrow(id, "标签不存在");
    return toDTO(tag);
  }

  /**
   * 创建标签
   *
   * @param command 创建标签命令
   * @return 标签DTO
   */
  @Transactional
  public ClientTagDTO createTag(final CreateClientTagCommand command) {
    // 检查标签名称是否已存在
    if (clientTagRepository.existsByTagName(command.getTagName())) {
      throw new BusinessException("标签名称已存在");
    }

    ClientTag tag =
        ClientTag.builder()
            .tagName(command.getTagName())
            .tagColor(
                StringUtils.hasText(command.getTagColor()) ? command.getTagColor() : "#1890ff")
            .description(command.getDescription())
            .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
            .build();

    clientTagRepository.save(tag);
    log.info("创建客户标签成功: {}", tag.getTagName());
    return toDTO(tag);
  }

  /**
   * 更新标签
   *
   * @param command 更新标签命令
   * @return 标签DTO
   */
  @Transactional
  public ClientTagDTO updateTag(final UpdateClientTagCommand command) {
    ClientTag tag = clientTagRepository.getByIdOrThrow(command.getId(), "标签不存在");

    // 如果更新标签名称，检查是否重复
    if (StringUtils.hasText(command.getTagName())
        && !command.getTagName().equals(tag.getTagName())) {
      if (clientTagRepository.existsByTagName(command.getTagName())) {
        throw new BusinessException("标签名称已存在");
      }
      tag.setTagName(command.getTagName());
    }

    if (StringUtils.hasText(command.getTagColor())) {
      tag.setTagColor(command.getTagColor());
    }
    if (command.getDescription() != null) {
      tag.setDescription(command.getDescription());
    }
    if (command.getSortOrder() != null) {
      tag.setSortOrder(command.getSortOrder());
    }

    clientTagRepository.updateById(tag);
    log.info("更新客户标签成功: {}", tag.getId());
    return toDTO(tag);
  }

  /**
   * 删除标签
   *
   * @param id 标签ID
   */
  @Transactional
  public void deleteTag(final Long id) {
    ClientTag tag = clientTagRepository.getByIdOrThrow(id, "标签不存在");
    clientTagRepository.softDelete(id);

    // 删除所有关联关系
    clientTagRelationMapper.deleteByTagId(id);

    log.info("删除客户标签成功: {}", tag.getTagName());
  }

  /**
   * 查询客户的标签列表
   *
   * @param clientId 客户ID
   * @return 标签列表
   */
  public List<ClientTagDTO> getClientTags(final Long clientId) {
    List<Long> tagIds = clientTagRelationMapper.selectTagIdsByClientId(clientId);
    if (tagIds.isEmpty()) {
      return List.of();
    }

    List<ClientTag> tags =
        clientTagRepository.list(
            new LambdaQueryWrapper<ClientTag>()
                .in(ClientTag::getId, tagIds)
                .eq(ClientTag::getDeleted, false));
    return tags.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 为客户分配标签
   *
   * @param command 分配标签命令
   */
  @Transactional
  public void assignTags(final AssignClientTagsCommand command) {
    // 验证客户存在
    clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

    // 删除原有标签关联
    clientTagRelationMapper.deleteByClientId(command.getClientId());

    // 添加新标签关联
    if (command.getTagIds() != null && !command.getTagIds().isEmpty()) {
      Long userId = SecurityUtils.getUserId();
      LocalDateTime now = LocalDateTime.now();

      for (Long tagId : command.getTagIds()) {
        // 验证标签存在
        clientTagRepository.getByIdOrThrow(tagId, "标签不存在");

        ClientTagRelation relation =
            ClientTagRelation.builder()
                .clientId(command.getClientId())
                .tagId(tagId)
                .createdBy(userId)
                .createdAt(now)
                .build();

        clientTagRelationMapper.insert(relation);
      }
    }

    log.info(
        "为客户分配标签成功: clientId={}, tagCount={}",
        command.getClientId(),
        command.getTagIds() != null ? command.getTagIds().size() : 0);
  }

  /**
   * 移除客户的标签
   *
   * @param clientId 客户ID
   * @param tagId 标签ID
   */
  @Transactional
  public void removeClientTag(final Long clientId, final Long tagId) {
    clientTagRelationMapper.delete(
        new LambdaQueryWrapper<ClientTagRelation>()
            .eq(ClientTagRelation::getClientId, clientId)
            .eq(ClientTagRelation::getTagId, tagId));
    log.info("移除客户标签成功: clientId={}, tagId={}", clientId, tagId);
  }

  /**
   * Entity转DTO
   *
   * @param tag 标签实体
   * @return 标签DTO
   */
  private ClientTagDTO toDTO(final ClientTag tag) {
    ClientTagDTO dto = new ClientTagDTO();
    dto.setId(tag.getId());
    dto.setTagName(tag.getTagName());
    dto.setTagColor(tag.getTagColor());
    dto.setDescription(tag.getDescription());
    dto.setSortOrder(tag.getSortOrder());
    dto.setCreatedAt(tag.getCreatedAt());
    dto.setUpdatedAt(tag.getUpdatedAt());
    return dto;
  }
}
