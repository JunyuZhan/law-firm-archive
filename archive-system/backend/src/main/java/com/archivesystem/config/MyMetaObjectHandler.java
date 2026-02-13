package com.archivesystem.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器.
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "uploadAt", LocalDateTime.class, now);
        
        // TODO: 从SecurityContext获取当前用户ID
        // Long userId = SecurityUtils.getCurrentUserId();
        // this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        // this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        // TODO: 从SecurityContext获取当前用户ID
        // Long userId = SecurityUtils.getCurrentUserId();
        // this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
    }
}
