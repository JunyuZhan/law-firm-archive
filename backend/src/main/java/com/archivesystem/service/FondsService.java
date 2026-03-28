package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.Fonds;

import java.util.List;

/**
 * 全宗服务接口.
 */
public interface FondsService {

    /**
     * 创建全宗.
     */
    Fonds create(Fonds fonds);

    /**
     * 更新全宗.
     */
    Fonds update(Long id, Fonds fonds);

    /**
     * 获取全宗详情.
     */
    Fonds getById(Long id);

    /**
     * 根据全宗号获取.
     */
    Fonds getByFondsNo(String fondsNo);

    /**
     * 获取全宗列表.
     */
    List<Fonds> list();

    /**
     * 分页查询.
     */
    PageResult<Fonds> query(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 删除全宗.
     */
    void delete(Long id);

    /**
     * 统计全宗下的档案数量.
     */
    long countArchives(Long fondsId);
}
