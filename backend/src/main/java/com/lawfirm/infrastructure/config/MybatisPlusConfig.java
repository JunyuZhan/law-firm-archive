package com.lawfirm.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.infrastructure.persistence.interceptor.DataScopeInterceptor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置.
 *
 * @author system
 * @since 2026-01-17
 */
@Configuration
@RequiredArgsConstructor
public class MybatisPlusConfig {

  /** 数据权限拦截器 */
  private final DataScopeInterceptor dataScopeInterceptor;

  /**
   * MyBatis-Plus 拦截器配置.
   *
   * <p>注意：拦截器顺序很重要 1. 数据权限拦截器（先执行，修改SQL） 2. 乐观锁拦截器（处理并发控制） 3. 分页拦截器（后执行，处理分页）
   *
   * @return MybatisPlusInterceptor实例
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 数据权限拦截器（需要在分页之前执行）
    interceptor.addInnerInterceptor(dataScopeInterceptor);
    // 乐观锁拦截器（防止并发数据覆盖）
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    // 分页插件
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
    return interceptor;
  }

  /**
   * 自动填充处理器.
   *
   * @return MetaObjectHandler实例
   */
  @Bean
  public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {
      /**
       * 插入时自动填充.
       *
       * @param metaObject 元对象
       */
      @Override
      public void insertFill(final MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Boolean.class, false);
        // 乐观锁版本号初始值
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
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

      /**
       * 更新时自动填充.
       *
       * @param metaObject 元对象
       */
      @Override
      public void updateFill(final MetaObject metaObject) {
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
