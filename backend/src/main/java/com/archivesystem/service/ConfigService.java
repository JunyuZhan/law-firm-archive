package com.archivesystem.service;

import com.archivesystem.entity.SysConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * @author junyuzhan
 */
public interface ConfigService {

    /**
     * 获取所有配置（按分组）
     */
    Map<String, List<SysConfig>> getAllGrouped();

    /**
     * 获取配置列表
     */
    List<SysConfig> getAll();

    /**
     * 根据分组获取配置
     */
    List<SysConfig> getByGroup(String group);

    /**
     * 根据配置键获取配置
     */
    SysConfig getByKey(String key);

    /**
     * 获取配置值
     */
    String getValue(String key);

    /**
     * 获取配置值（带默认值）
     */
    String getValue(String key, String defaultValue);

    /**
     * 获取整数配置值
     */
    Integer getIntValue(String key, Integer defaultValue);

    /**
     * 获取布尔配置值
     */
    Boolean getBooleanValue(String key, Boolean defaultValue);

    /**
     * 更新配置
     */
    void updateConfig(String key, String value);

    /**
     * 批量更新配置
     */
    void batchUpdateConfigs(Map<String, String> configs);

    /**
     * 更新或创建配置.
     */
    void saveConfig(String key, String value, String group, String description, String type, Boolean editable, Integer sortOrder);

    /**
     * 创建配置
     */
    SysConfig createConfig(SysConfig config);

    /**
     * 删除配置
     */
    void deleteConfig(String key);

    /**
     * 刷新配置缓存
     */
    void refreshCache();

    /**
     * 获取档案号前缀
     */
    String getArchiveNoPrefix(String archiveType);
}
