package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateDictItemCommand;
import com.lawfirm.application.system.command.CreateDictTypeCommand;
import com.lawfirm.application.system.dto.DictItemDTO;
import com.lawfirm.application.system.dto.DictTypeDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.DictItem;
import com.lawfirm.domain.system.entity.DictType;
import com.lawfirm.domain.system.repository.DictItemRepository;
import com.lawfirm.domain.system.repository.DictTypeRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
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
    private final BusinessCacheService businessCacheService;

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
     * 问题498修复：添加缓存
     */
    public List<DictItemDTO> getDictItemsByCode(String code) {
        return businessCacheService.getDictItems(code, () -> {
            return dictItemMapper.selectByTypeCode(code).stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 创建字典类型
     * 问题497修复：添加编码格式验证
     */
    @Transactional
    public DictTypeDTO createDictType(CreateDictTypeCommand command) {
        if (!StringUtils.hasText(command.getCode())) {
            throw new BusinessException("字典编码不能为空");
        }

        // 问题497修复：验证编码格式
        String code = command.getCode().trim();
        if (!code.matches("^[a-zA-Z][a-zA-Z0-9_]{0,49}$")) {
            throw new BusinessException("字典编码格式错误：只能包含字母、数字和下划线，以字母开头，长度1-50");
        }

        if (dictTypeMapper.selectByCode(code) != null) {
            throw new BusinessException("字典编码已存在");
        }

        DictType type = DictType.builder()
                .name(command.getName())
                .code(code)
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
     * 问题502修复：检查是否正在使用（如果修改编码）
     */
    @Transactional
    public DictTypeDTO updateDictType(Long id, CreateDictTypeCommand command) {
        DictType type = dictTypeRepository.getByIdOrThrow(id, "字典类型不存在");
        
        if (type.getIsSystem()) {
            throw new BusinessException("系统内置字典不能修改");
        }

        // 问题502修复：如果修改编码，检查是否有字典项在使用
        if (StringUtils.hasText(command.getCode()) && !command.getCode().equals(type.getCode())) {
            long itemCount = dictItemMapper.countByTypeId(id);
            if (itemCount > 0) {
                throw new BusinessException("该字典类型下有" + itemCount + "个字典项，无法修改编码");
            }
            // 清除旧编码的缓存
            businessCacheService.evictDictItems(type.getCode());
            type.setCode(command.getCode().trim());
        }

        if (StringUtils.hasText(command.getName())) {
            type.setName(command.getName());
        }
        if (command.getDescription() != null) {
            type.setDescription(command.getDescription());
        }

        dictTypeRepository.updateById(type);
        
        // 如果修改了编码，清除新编码的缓存
        if (StringUtils.hasText(command.getCode()) && !command.getCode().equals(type.getCode())) {
            businessCacheService.evictDictItems(command.getCode().trim());
        }
        
        log.info("字典类型更新成功: {}", type.getCode());
        return toTypeDTO(type);
    }

    /**
     * 删除字典类型
     * 问题489修复：检查字典项关联
     */
    @Transactional
    public void deleteDictType(Long id) {
        DictType type = dictTypeRepository.getByIdOrThrow(id, "字典类型不存在");
        
        if (type.getIsSystem()) {
            throw new BusinessException("系统内置字典不能删除");
        }

        // 问题489修复：检查是否有字典项
        long itemCount = dictItemMapper.countByTypeId(id);
        if (itemCount > 0) {
            throw new BusinessException("该字典类型下有" + itemCount + "个字典项，无法删除");
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
     * 问题498修复：清除缓存
     */
    @Transactional
    public DictItemDTO createDictItem(CreateDictItemCommand command) {
        DictType type = dictTypeRepository.getByIdOrThrow(command.getDictTypeId(), "字典类型不存在");

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
        
        // 问题498修复：清除缓存
        businessCacheService.evictDictItems(type.getCode());
        
        log.info("字典项创建成功: {}", item.getLabel());
        return toItemDTO(item);
    }

    /**
     * 更新字典项
     * 问题498修复：清除缓存
     */
    @Transactional
    public DictItemDTO updateDictItem(Long id, CreateDictItemCommand command) {
        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");
        DictType type = dictTypeRepository.getById(item.getDictTypeId());

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
        
        // 问题498修复：清除缓存
        if (type != null) {
            businessCacheService.evictDictItems(type.getCode());
        }
        
        log.info("字典项更新成功: {}", item.getLabel());
        return toItemDTO(item);
    }

    /**
     * 删除字典项
     * 问题490修复：使用软删除（禁用）替代物理删除
     * 问题498修复：清除缓存
     */
    @Transactional
    public void deleteDictItem(Long id) {
        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");
        DictType type = dictTypeRepository.getById(item.getDictTypeId());

        // 问题490修复：使用软删除（禁用）
        item.setStatus(DictItem.STATUS_DISABLED);
        dictItemRepository.updateById(item);

        // 问题498修复：清除缓存
        if (type != null) {
            businessCacheService.evictDictItems(type.getCode());
        }

        log.info("字典项已禁用: {}", item.getLabel());
    }

    /**
     * 启用/禁用字典项
     * 问题491修复：添加权限验证
     * 问题498修复：清除缓存
     */
    @Transactional
    public void toggleDictItemStatus(Long id) {
        // 问题491修复：验证权限
        if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("SYSTEM_MANAGER")) {
            throw new BusinessException("权限不足：只有管理员才能修改字典状态");
        }

        DictItem item = dictItemRepository.getByIdOrThrow(id, "字典项不存在");
        DictType type = dictTypeRepository.getById(item.getDictTypeId());
        String oldStatus = item.getStatus();
        item.setStatus(DictItem.STATUS_ENABLED.equals(item.getStatus()) 
                ? DictItem.STATUS_DISABLED : DictItem.STATUS_ENABLED);
        dictItemRepository.updateById(item);

        // 问题498修复：清除缓存
        if (type != null) {
            businessCacheService.evictDictItems(type.getCode());
        }

        log.info("字典项状态变更: id={}, {} -> {}", id, oldStatus, item.getStatus());
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
