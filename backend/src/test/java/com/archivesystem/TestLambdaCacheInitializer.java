package com.archivesystem;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.HashSet;
import java.util.Set;

/**
 * 测试用 MyBatis-Plus Lambda 缓存初始化工具.
 * 解决单元测试中 "can not find lambda cache for this entity" 错误.
 */
public class TestLambdaCacheInitializer {

    private static final Set<Class<?>> initializedClasses = new HashSet<>();
    private static MybatisConfiguration configuration;
    private static MapperBuilderAssistant assistant;

    public static synchronized void initTableInfo(Class<?>... entityClasses) {
        if (configuration == null) {
            configuration = new MybatisConfiguration();
            assistant = new MapperBuilderAssistant(configuration, "");
        }
        
        for (Class<?> entityClass : entityClasses) {
            if (!initializedClasses.contains(entityClass)) {
                TableInfoHelper.initTableInfo(assistant, entityClass);
                initializedClasses.add(entityClass);
            }
        }
    }
}
