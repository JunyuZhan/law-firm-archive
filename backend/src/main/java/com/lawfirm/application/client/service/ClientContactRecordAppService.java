package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.CreateContactRecordCommand;
import com.lawfirm.application.client.command.UpdateContactRecordCommand;
import com.lawfirm.application.client.dto.ClientContactRecordDTO;
import com.lawfirm.application.client.dto.ContactRecordQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ClientContactRecord;
import com.lawfirm.domain.client.repository.ClientContactRecordRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户联系记录应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContactRecordAppService {

    private final ClientContactRecordRepository contactRecordRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    /**
     * 分页查询联系记录
     */
    public PageResult<ClientContactRecordDTO> listContactRecords(ContactRecordQueryDTO query) {
        // 如果指定了clientId，直接查询该客户的联系记录
        if (query.getClientId() != null) {
            IPage<ClientContactRecord> page = contactRecordRepository.findByClientId(
                    new Page<>(query.getPageNum(), query.getPageSize()),
                    query.getClientId()
            );
            return PageResult.of(
                    page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()),
                    page.getTotal(),
                    query.getPageNum(),
                    query.getPageSize()
            );
        }

        // TODO: 实现更复杂的查询逻辑（按联系方式、时间范围等筛选）
        // 目前简化处理，返回空结果
        return PageResult.of(List.of(), 0L, query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取客户的联系记录列表
     */
    public List<ClientContactRecordDTO> getContactRecordsByClientId(Long clientId) {
        clientRepository.getByIdOrThrow(clientId, "客户不存在");
        List<ClientContactRecord> records = contactRecordRepository.findAllByClientId(clientId);
        return records.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建联系记录
     */
    @Transactional
    public ClientContactRecordDTO createContactRecord(CreateContactRecordCommand command) {
        // 验证客户存在
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        ClientContactRecord record = ClientContactRecord.builder()
                .clientId(command.getClientId())
                .contactId(command.getContactId())
                .contactPerson(command.getContactPerson())
                .contactMethod(command.getContactMethod())
                .contactDate(command.getContactDate())
                .contactDuration(command.getContactDuration())
                .contactLocation(command.getContactLocation())
                .contactContent(command.getContactContent())
                .contactResult(command.getContactResult())
                .nextFollowUpDate(command.getNextFollowUpDate())
                .followUpReminder(command.getFollowUpReminder() != null ? command.getFollowUpReminder() : false)
                .createdBy(SecurityUtils.getUserId())
                .build();

        contactRecordRepository.save(record);
        log.info("联系记录创建成功: clientId={}, contactMethod={}", command.getClientId(), command.getContactMethod());
        return toDTO(record);
    }

    /**
     * 更新联系记录
     */
    @Transactional
    public ClientContactRecordDTO updateContactRecord(UpdateContactRecordCommand command) {
        ClientContactRecord record = contactRecordRepository.getByIdOrThrow(command.getId(), "联系记录不存在");

        if (command.getContactId() != null) {
            record.setContactId(command.getContactId());
        }
        if (command.getContactPerson() != null) {
            record.setContactPerson(command.getContactPerson());
        }
        if (command.getContactMethod() != null) {
            record.setContactMethod(command.getContactMethod());
        }
        if (command.getContactDate() != null) {
            record.setContactDate(command.getContactDate());
        }
        if (command.getContactDuration() != null) {
            record.setContactDuration(command.getContactDuration());
        }
        if (command.getContactLocation() != null) {
            record.setContactLocation(command.getContactLocation());
        }
        if (command.getContactContent() != null) {
            record.setContactContent(command.getContactContent());
        }
        if (command.getContactResult() != null) {
            record.setContactResult(command.getContactResult());
        }
        if (command.getNextFollowUpDate() != null) {
            record.setNextFollowUpDate(command.getNextFollowUpDate());
        }
        if (command.getFollowUpReminder() != null) {
            record.setFollowUpReminder(command.getFollowUpReminder());
        }

        contactRecordRepository.updateById(record);
        log.info("联系记录更新成功: id={}", command.getId());
        return toDTO(record);
    }

    /**
     * 删除联系记录
     */
    @Transactional
    public void deleteContactRecord(Long id) {
        ClientContactRecord record = contactRecordRepository.getByIdOrThrow(id, "联系记录不存在");
        contactRecordRepository.getBaseMapper().deleteById(id);
        log.info("联系记录删除成功: id={}, clientId={}", id, record.getClientId());
    }

    /**
     * 查询需要跟进的联系记录
     */
    public List<ClientContactRecordDTO> getFollowUpRecords(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        List<ClientContactRecord> records = contactRecordRepository.findFollowUpRecords(date);
        return records.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private ClientContactRecordDTO toDTO(ClientContactRecord record) {
        ClientContactRecordDTO dto = new ClientContactRecordDTO();
        dto.setId(record.getId());
        dto.setClientId(record.getClientId());
        
        // 加载客户名称
        if (record.getClientId() != null) {
            try {
                Client client = clientRepository.findById(record.getClientId());
                if (client != null) {
                    dto.setClientName(client.getName());
                }
            } catch (Exception e) {
                // 忽略错误，不阻断流程
            }
        }
        
        dto.setContactId(record.getContactId());
        dto.setContactPerson(record.getContactPerson());
        dto.setContactMethod(record.getContactMethod());
        dto.setContactMethodName(getContactMethodName(record.getContactMethod()));
        dto.setContactDate(record.getContactDate());
        dto.setContactDuration(record.getContactDuration());
        dto.setContactLocation(record.getContactLocation());
        dto.setContactContent(record.getContactContent());
        dto.setContactResult(record.getContactResult());
        dto.setNextFollowUpDate(record.getNextFollowUpDate());
        dto.setFollowUpReminder(record.getFollowUpReminder());
        dto.setCreatedBy(record.getCreatedBy());
        
        // 加载记录人名称
        if (record.getCreatedBy() != null) {
            try {
                User user = userRepository.findById(record.getCreatedBy());
                if (user != null) {
                    dto.setCreatedByName(user.getRealName());
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
        
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    /**
     * 获取联系方式名称
     */
    private String getContactMethodName(String method) {
        if (method == null) return null;
        return switch (method) {
            case "PHONE" -> "电话";
            case "EMAIL" -> "邮件";
            case "MEETING" -> "会面";
            case "VISIT" -> "拜访";
            case "OTHER" -> "其他";
            default -> method;
        };
    }
}

