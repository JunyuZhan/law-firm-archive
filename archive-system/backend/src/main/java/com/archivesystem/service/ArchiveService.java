package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.archive.*;

/**
 * 档案服务接口.
 */
public interface ArchiveService {

    /**
     * 接收外部系统推送的档案.
     */
    ArchiveReceiveResponse receive(ArchiveReceiveRequest request);

    /**
     * 手动创建档案.
     */
    ArchiveDTO create(ArchiveCreateRequest request);

    /**
     * 更新档案.
     */
    ArchiveDTO update(Long id, ArchiveCreateRequest request);

    /**
     * 获取档案详情.
     */
    ArchiveDTO getById(Long id);

    /**
     * 根据档案号获取档案详情.
     */
    ArchiveDTO getByArchiveNo(String archiveNo);

    /**
     * 分页查询档案列表.
     */
    PageResult<ArchiveDTO> query(ArchiveQueryRequest request);

    /**
     * 删除档案（逻辑删除）.
     */
    void delete(Long id);

    /**
     * 更新档案状态.
     */
    void updateStatus(Long id, String status);

    /**
     * 生成档案号.
     */
    String generateArchiveNo(String archiveType);
}
