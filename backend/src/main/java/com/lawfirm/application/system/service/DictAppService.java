package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateDictItemCommand;
import com.lawfirm.application.system.command.CreateDictTypeCommand;
import com.lawfirm.application.system.dto.DictItemDTO;
import com.lawfirm.application.system.dto.DictTypeDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.DictItem;
import com.lawfirm.domain.system.entity.DictType;
import com.lawfirm.domain.system.repository.DictItemRepository;
import com.lawfirm.domain.system.repository.DictTypeRepository;
import com.lawfirm.infrastructure.persistence.mapper.DictItemMapper;
import com.lawfirm.infrastructure.persistence.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictAppService {

    private final DictTypeRepository dictTypeRepository;
    private final DictTypeMapper dictTypeMapper;
    private final DictItemRepository dictItemRepository;
    private final DictItemMapper dictItemMapper;

    // ==================== 字典类型 ====================

    /**
     * 获取所有字典类型
     */
    public List<DictTypeDTO> listDictTypes() {
        return dictTypeMapper.selectEnabledTypes().stream()
                .map(this::toTypeDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取字典类型详情（含字典项）
     */
    public DictTypeDTO getDictTypeWithItems(Long id) {
        DictType type = dictTypeRepository.getByIdOrThrow(id, "字典类型不存在");
        DictTypeDTO dto = toTypeDTO(type);
        dto.setItems(getDictItemsByTypeId(id));
        return dto;
    }

    /**
     * 根据编码获取字典项
     */
    public List<DictItemDTO> getDictItemsByCode(String code) {
        return dictItemMapper.selectByTypeCode(code).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建字典类型
     */
    @Transactional
    public DictTypeDTO createDictType(CreateDictTypeCommand command) {
        if (!StringUtils.hasText(command.getCode())) {
            throw new BusinessException("字典编码不能为空");
        }
        if (dictTypeMapper.selectByCode(command.getCode()) != null) {
            throw new BusinessException("字典编码已存在");
        }

        DictType type = DictType.builder()
                .name(command.getName())
                .code(command.getCode())
                .description(command.getDescription())
                .status(DictType.STATUS_ENABLED)
                .isSystem(false)
                .build();

        dictTypeRepository.save(type);
        log.info("字典类型创建成功: {}", type.getCode());
        return toTypeDTO(type);
    }

    /**
     * 更新字典类型
     */
    @Transactional
    public DictTypeDTO updateDictType(Long id, CreateDictTypeCommand command) {
        DictType type = dictTypeRepository.getByIdOrThrow(id, "字典类型不存在");
        
        if (type.getIsSystem()) {
            throw new BusinessException("系统内置字典不能修改");
        }

        if (StringUtils.hasText(command.getName())) {
            type.setName(command.getName());
        }
        if (command.getDescription() != null) {
            type.setDescription(command.getDescription());
        }

        dictTypeRepository.updateById(type);
        log.info("字典类型更新成功: {}", type.getCode());
        return toTypeDTO(type);
    }

    /**
     * 删除字典类型
     */
    @Transactional
    public void deleteDictType(Long id) {
        DictType type = dictTypeRepository.getByIdOrThrow(id, "字典类型不存在");
        
        if (type.getIsSystem()) {
            throw new BusinessException("系统内置字典不能删除");
        }

        dictTypeMapper.deleteById(id);
        log.info("字典类型删除成功: {}", type.getCode());
    }

    // ==================== 字典项 ====================

    /**
     * 获取字典项列表
     */
    public List<DictItemDTO> getDictItemsByTypeId(Long typeId) {
        return dictItemMapper.selectByTypeId(typeId).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建字典项
     */
    @Transactional
    public DictItemDTO createDictItem(CreateDictItemCommand command) {
        dictTypeRepository.getByIdOrThrow(command.getDictTypeId(), "字典类型不存在");

        DictItem item = DictItem.builder()
                .dictTypeId(command.getDictTypeId())
                .label(command.getLabel())
                .value(command.getValue())
                .description(command.getDescription())
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .status(DictItem.STATUS_ENABLED)
                .cssClass(command.getCssClass())
                .build();

        dictItemRepository.save(item);
        log.info("字典项创建成功: {}", item.getLabel());
        return toItemDTO(item);
    }

    /**
     * 更新字典项
     */
    @Transactional
    public DictItemDTO updateDictItem(Long id, CreateDictItemCommand command) {
        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");

        if (StringUtils.hasText(command.getLabel())) {
            item.setLabel(command.getLabel());
        }
        if (command.getDescription() != null) {
            item.setDescription(command.getDescription());
        }
        if (command.getSortOrder() != null) {
            item.setSortOrder(command.getSortOrder());
        }
        if (command.getCssClass() != null) {
            item.setCssClass(command.getCssClass());
        }

        dictItemRepository.updateById(item);
        log.info("字典项更新成功: {}", item.getLabel());
        return toItemDTO(item);
    }

    /**
     * 删除字典项
     */
    @Transactional
    public void deleteDictItem(Long id) {
        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");
        dictItemMapper.deleteById(id);
        log.info("字典项删除成功: {}", item.getLabel());
    }

    /**
     * 启用/禁用字典项
     */
    @Transactional
    public void toggleDictItemStatus(Long id) {
        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");
        item.setStatus(DictItem.STATUS_ENABLED.equals(item.getStatus()) 
                ? DictItem.STATUS_DISABLED : DictItem.STATUS_ENABLED);
        dictItemRepository.updateById(item);
    }

    private DictTypeDTO toTypeDTO(DictType type) {
        DictTypeDTO dto = new DictTypeDTO();
        dto.setId(type.getId());
        dto.setName(type.getName());
        dto.setCode(type.getCode());
        dto.setDescription(type.getDescription());
        dto.setStatus(type.getStatus());
        dto.setIsSystem(type.getIsSystem());
        return dto;
    }

    private DictItemDTO toItemDTO(DictItem item) {
        DictItemDTO dto = new DictItemDTO();
        dto.setId(item.getId());
        dto.setDictTypeId(item.getDictTypeId());
        dto.setLabel(item.getLabel());
        dto.setValue(item.getValue());
        dto.setDescription(item.getDescription());
        dto.setSortOrder(item.getSortOrder());
        dto.setStatus(item.getStatus());
        dto.setCssClass(item.getCssClass());
        return dto;
    }
}
