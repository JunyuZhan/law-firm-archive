package com.lawfirm.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.lawfirm.common.util.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
                // 从SecurityContext获取当前用户ID
                try {
                    Long userId = SecurityUtils.getUserId();
                    if (userId != null && metaObject.hasSetter("createdBy")) {
                        this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                    }
                } catch (Exception e) {
                    // 忽略异常，允许在没有认证上下文的情况下创建记录
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                // 从SecurityContext获取当前用户ID
                try {
                    Long userId = SecurityUtils.getUserId();
                    if (userId != null && metaObject.hasSetter("updatedBy")) {
                        this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
                    }
                } catch (Exception e) {
                    // 忽略异常，允许在没有认证上下文的情况下更新记录
                }
            }
        };
    }
}

