package com.archivesystem.service;

import com.archivesystem.entity.RetentionPeriod;

import java.util.List;

/**
 * 保管期限服务接口.
 * @author junyuzhan
 */
public interface RetentionPeriodService {

    /**
     * 获取全部保管期限，按排序号升序.
     */
    List<RetentionPeriod> listAll();
}
