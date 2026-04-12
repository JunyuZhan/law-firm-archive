package com.archivesystem.config;

import com.archivesystem.security.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器.
 * @author junyuzhan
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "uploadAt", LocalDateTime.class, now);
        
        // 从SecurityContext获取当前用户ID（可能为空，如系统自动任务）
        Long userId = getCurrentUserIdSafely();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
            this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        // 从SecurityContext获取当前用户ID
        Long userId = getCurrentUserIdSafely();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
        }
    }
    
    /**
     * 安全获取当前用户ID，在未认证时返回null而非抛出异常
     */
    private Long getCurrentUserIdSafely() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // 系统任务或匿名访问时可能没有登录用户
            log.trace("无法获取当前用户ID，可能是系统任务: {}", e.getMessage());
            return null;
        }
    }
}
