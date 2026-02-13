package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.ArchiveLocation;

import java.util.List;

/**
 * 存放位置服务接口.
 */
public interface LocationService {

    /**
     * 创建位置.
     */
    ArchiveLocation create(ArchiveLocation location);

    /**
     * 更新位置.
     */
    ArchiveLocation update(Long id, ArchiveLocation location);

    /**
     * 删除位置.
     */
    void delete(Long id);

    /**
     * 获取位置详情.
     */
    ArchiveLocation getById(Long id);

    /**
     * 根据编码获取位置.
     */
    ArchiveLocation getByCode(String code);

    /**
     * 获取位置列表（分页）.
     */
    PageResult<ArchiveLocation> getList(String roomName, String status, Integer pageNum, Integer pageSize);

    /**
     * 获取所有位置.
     */
    List<ArchiveLocation> getAll();

    /**
     * 获取可用位置.
     */
    List<ArchiveLocation> getAvailable();

    /**
     * 根据库房获取位置.
     */
    List<ArchiveLocation> getByRoom(String roomName);

    /**
     * 获取所有库房名称.
     */
    List<String> getRoomNames();

    /**
     * 更新位置使用量.
     */
    void updateUsage(Long id, int delta);
}
