package com.lawfirm.application.client.service;

import com.lawfirm.application.client.command.CreateContactCommand;
import com.lawfirm.application.client.command.UpdateContactCommand;
import com.lawfirm.application.client.dto.ContactDTO;
import com.lawfirm.domain.client.entity.Contact;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ContactRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 联系人应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactAppService {

  /** 联系人仓储. */
  private final ContactRepository contactRepository;

  /** 客户仓储. */
  private final ClientRepository clientRepository;

  /**
   * 获取客户的联系人列表
   *
   * @param clientId 客户ID
   * @return 联系人列表
   */
  public List<ContactDTO> listContacts(final Long clientId) {
    // 验证客户存在
    clientRepository.getByIdOrThrow(clientId, "客户不存在");

    List<Contact> contacts = contactRepository.findByClientId(clientId);
    return contacts.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取联系人详情
   *
   * @param id 联系人ID
   * @return 联系人DTO
   */
  public ContactDTO getContactById(final Long id) {
    Contact contact = contactRepository.getByIdOrThrow(id, "联系人不存在");
    return toDTO(contact);
  }

  /**
   * 创建联系人
   *
   * @param command 创建联系人命令
   * @return 联系人DTO
   */
  @Transactional
  public ContactDTO createContact(final CreateContactCommand command) {
    // 验证客户存在
    clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

    // 如果设置为主要联系人，先取消其他主要联系人
    if (Boolean.TRUE.equals(command.getIsPrimary())) {
      contactRepository.clearPrimaryByClientId(command.getClientId());
    }

    Contact contact =
        Contact.builder()
            .clientId(command.getClientId())
            .contactName(command.getContactName())
            .position(command.getPosition())
            .department(command.getDepartment())
            .mobilePhone(command.getMobilePhone())
            .officePhone(command.getOfficePhone())
            .email(command.getEmail())
            .wechat(command.getWechat())
            .isPrimary(command.getIsPrimary() != null ? command.getIsPrimary() : false)
            .relationshipNote(command.getRelationshipNote())
            .build();

    contactRepository.save(contact);
    log.info("创建联系人: clientId={}, contactName={}", command.getClientId(), command.getContactName());
    return toDTO(contact);
  }

  /**
   * 更新联系人
   *
   * @param id 联系人ID
   * @param command 更新联系人命令
   * @return 联系人DTO
   */
  @Transactional
  public ContactDTO updateContact(final Long id, final UpdateContactCommand command) {
    Contact contact = contactRepository.getByIdOrThrow(id, "联系人不存在");

    // 如果设置为主要联系人，先取消该客户的其他主要联系人
    if (Boolean.TRUE.equals(command.getIsPrimary())
        && !Boolean.TRUE.equals(contact.getIsPrimary())) {
      contactRepository.clearPrimaryByClientId(contact.getClientId());
    }

    contact.setContactName(command.getContactName());
    contact.setPosition(command.getPosition());
    contact.setDepartment(command.getDepartment());
    contact.setMobilePhone(command.getMobilePhone());
    contact.setOfficePhone(command.getOfficePhone());
    contact.setEmail(command.getEmail());
    contact.setWechat(command.getWechat());
    if (command.getIsPrimary() != null) {
      contact.setIsPrimary(command.getIsPrimary());
    }
    contact.setRelationshipNote(command.getRelationshipNote());

    contactRepository.updateById(contact);
    log.info("更新联系人: id={}, contactName={}", id, command.getContactName());
    return toDTO(contact);
  }

  /**
   * 删除联系人
   *
   * @param id 联系人ID
   */
  @Transactional
  public void deleteContact(final Long id) {
    Contact contact = contactRepository.getByIdOrThrow(id, "联系人不存在");
    contactRepository.softDelete(id);
    log.info("删除联系人: id={}, contactName={}", id, contact.getContactName());
  }

  /**
   * 设置主要联系人
   *
   * @param id 联系人ID
   * @return 联系人DTO
   */
  @Transactional
  public ContactDTO setPrimaryContact(final Long id) {
    Contact contact = contactRepository.getByIdOrThrow(id, "联系人不存在");

    // 取消该客户的其他主要联系人
    contactRepository.clearPrimaryByClientId(contact.getClientId());

    // 设置当前联系人为主要联系人
    contact.setIsPrimary(true);
    contactRepository.updateById(contact);

    log.info("设置主要联系人: id={}, contactName={}", id, contact.getContactName());
    return toDTO(contact);
  }

  /**
   * 转换为DTO
   *
   * @param contact 联系人实体
   * @return 联系人DTO
   */
  private ContactDTO toDTO(final Contact contact) {
    ContactDTO dto = new ContactDTO();
    dto.setId(contact.getId());
    dto.setClientId(contact.getClientId());
    dto.setContactName(contact.getContactName());
    dto.setPosition(contact.getPosition());
    dto.setDepartment(contact.getDepartment());
    dto.setMobilePhone(contact.getMobilePhone());
    dto.setOfficePhone(contact.getOfficePhone());
    dto.setEmail(contact.getEmail());
    dto.setWechat(contact.getWechat());
    dto.setIsPrimary(contact.getIsPrimary());
    dto.setRelationshipNote(contact.getRelationshipNote());
    dto.setCreatedAt(contact.getCreatedAt());
    dto.setUpdatedAt(contact.getUpdatedAt());
    return dto;
  }
}
