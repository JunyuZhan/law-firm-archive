package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.ArchiveLocation;
import com.archivesystem.repository.ArchiveLocationMapper;
import com.archivesystem.service.LocationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 存放位置服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final ArchiveLocationMapper locationMapper;

    @Override
    @Transactional
    public ArchiveLocation create(ArchiveLocation location) {
        // 检查编码是否已存在
        if (locationMapper.selectByCode(location.getLocationCode()) != null) {
            throw new BusinessException("位置编码已存在: " + location.getLocationCode());
        }

        // 设置默认值
        if (location.getTotalCapacity() == null) {
            location.setTotalCapacity(0);
        }
        if (location.getUsedCapacity() == null) {
            location.setUsedCapacity(0);
        }
        if (!StringUtils.hasText(location.getStatus())) {
            location.setStatus(ArchiveLocation.STATUS_AVAILABLE);
        }

        locationMapper.insert(location);
        log.info("创建存放位置: id={}, code={}", location.getId(), location.getLocationCode());
        return location;
    }

    @Override
    @Transactional
    public ArchiveLocation update(Long id, ArchiveLocation location) {
        ArchiveLocation existing = getById(id);

        // 如果修改了编码，检查新编码是否已存在
        if (StringUtils.hasText(location.getLocationCode()) 
            && !location.getLocationCode().equals(existing.getLocationCode())) {
            if (locationMapper.selectByCode(location.getLocationCode()) != null) {
                throw new BusinessException("位置编码已存在: " + location.getLocationCode());
            }
            existing.setLocationCode(location.getLocationCode());
        }

        // 更新字段
        if (StringUtils.hasText(location.getLocationName())) {
            existing.setLocationName(location.getLocationName());
        }
        if (location.getRoomName() != null) {
            existing.setRoomName(location.getRoomName());
        }
        if (location.getArea() != null) {
            existing.setArea(location.getArea());
        }
        if (location.getShelfNo() != null) {
            existing.setShelfNo(location.getShelfNo());
        }
        if (location.getLayerNo() != null) {
            existing.setLayerNo(location.getLayerNo());
        }
        if (location.getTotalCapacity() != null) {
            existing.setTotalCapacity(location.getTotalCapacity());
        }
        if (location.getStatus() != null) {
            existing.setStatus(location.getStatus());
        }
        if (location.getRemarks() != null) {
            existing.setRemarks(location.getRemarks());
        }

        locationMapper.updateById(existing);
        log.info("更新存放位置: id={}", id);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ArchiveLocation location = getById(id);

        // 检查是否有档案使用此位置
        if (location.getUsedCapacity() != null && location.getUsedCapacity() > 0) {
            throw new BusinessException("该位置已有档案存放，无法删除");
        }

        location.setDeleted(true);
        locationMapper.updateById(location);
        log.info("删除存放位置: id={}", id);
    }

    @Override
    public ArchiveLocation getById(Long id) {
        ArchiveLocation location = locationMapper.selectById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            throw NotFoundException.of("存放位置", id);
        }
        return location;
    }

    @Override
    public ArchiveLocation getByCode(String code) {
        return locationMapper.selectByCode(code);
    }

    @Override
    public PageResult<ArchiveLocation> getList(String roomName, String status, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ArchiveLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArchiveLocation::getDeleted, false);

        if (StringUtils.hasText(roomName)) {
            wrapper.eq(ArchiveLocation::getRoomName, roomName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ArchiveLocation::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ArchiveLocation::getLocationCode, keyword)
                    .or().like(ArchiveLocation::getLocationName, keyword)
                    .or().like(ArchiveLocation::getArea, keyword)
                    .or().like(ArchiveLocation::getShelfNo, keyword));
        }

        wrapper.orderByAsc(ArchiveLocation::getLocationCode);

        Page<ArchiveLocation> page = new Page<>(pageNum, pageSize);
        Page<ArchiveLocation> result = locationMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public List<ArchiveLocation> getAll() {
        return locationMapper.selectList(
                new LambdaQueryWrapper<ArchiveLocation>()
                        .eq(ArchiveLocation::getDeleted, false)
                        .orderByAsc(ArchiveLocation::getLocationCode));
    }

    @Override
    public List<ArchiveLocation> getAvailable() {
        return locationMapper.selectAvailable();
    }

    @Override
    public List<ArchiveLocation> getByRoom(String roomName) {
        return locationMapper.selectByRoom(roomName);
    }

    @Override
    public List<String> getRoomNames() {
        return locationMapper.selectRoomNames();
    }

    @Override
    @Transactional
    public void updateUsage(Long id, int delta) {
        ArchiveLocation location = getById(id);
        int newUsed = (location.getUsedCapacity() != null ? location.getUsedCapacity() : 0) + delta;
        
        if (newUsed < 0) {
            newUsed = 0;
        }
        
        location.setUsedCapacity(newUsed);

        // 更新状态
        if (location.getTotalCapacity() != null && location.getTotalCapacity() > 0) {
            if (newUsed >= location.getTotalCapacity()) {
                location.setStatus(ArchiveLocation.STATUS_FULL);
            } else if (ArchiveLocation.STATUS_FULL.equals(location.getStatus())) {
                location.setStatus(ArchiveLocation.STATUS_AVAILABLE);
            }
        }

        locationMapper.updateById(location);
    }
}
