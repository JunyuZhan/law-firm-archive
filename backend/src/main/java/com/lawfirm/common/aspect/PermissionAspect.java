package com.lawfirm.common.aspect;

import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(com.lawfirm.common.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 1. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            return;
        }

        // 2. 检查用户是否已登录（如果未登录，SecurityUtils会抛出401异常）
        try {
            // 管理员跳过权限校验
            if (SecurityUtils.isAdmin()) {
                return;
            }

            // 3. 获取当前用户权限
            Set<String> userPermissions = SecurityUtils.getPermissions();

        // 4. 校验权限
        String[] requiredPermissions = annotation.value();
        RequirePermission.Logical logical = annotation.logical();

        boolean hasPermission;
        if (logical == RequirePermission.Logical.AND) {
            // AND逻辑：需要拥有所有权限
            hasPermission = true;
            for (String permission : requiredPermissions) {
                if (!userPermissions.contains(permission)) {
                    hasPermission = false;
                    break;
                }
            }
        } else {
            // OR逻辑：只需拥有任一权限
            hasPermission = false;
            for (String permission : requiredPermissions) {
                if (userPermissions.contains(permission)) {
                    hasPermission = true;
                    break;
                }
            }
        }

        if (!hasPermission) {
            log.warn("权限不足: 用户[{}]缺少权限{}", SecurityUtils.getUsername(), String.join(",", requiredPermissions));
            throw new BusinessException("403", "权限不足");
        }
        } catch (BusinessException e) {
            // 重新抛出业务异常（401或403），让GlobalExceptionHandler处理
            throw e;
        } catch (Exception e) {
            // 其他异常（如空指针等）记录日志并抛出
            log.error("权限检查时发生异常", e);
            throw new BusinessException("500", "权限检查失败: " + e.getMessage());
        }
    }
}

