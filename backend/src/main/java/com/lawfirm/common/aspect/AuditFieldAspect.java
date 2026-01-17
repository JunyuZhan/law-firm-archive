package com.lawfirm.common.aspect;

import com.lawfirm.common.annotation.AuditField;
import com.lawfirm.common.util.FieldChangeUtils;
import com.lawfirm.common.util.FieldChangeUtils.FieldChange;
import com.lawfirm.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字段审计切面
 * 
 * 自动记录字段变更日志
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditFieldAspect {

    @Around("@annotation(com.lawfirm.common.annotation.AuditField)")
    public Object auditFieldChanges(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditField annotation = method.getAnnotation(AuditField.class);

        // 2. 获取方法参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }

        // 3. 获取命令对象（通常是第一个参数）
        Object command = args[0];
        if (command == null) {
            return joinPoint.proceed();
        }

        // 4. 尝试获取实体ID
        Long entityId = extractEntityId(command, annotation.idParam());
        
        // 5. 记录变更前的值（简化实现：从command中提取）
        Object oldValues = null;
        if (entityId != null) {
            // 这里可以扩展为从数据库查询原始数据
            // 简化实现：记录command中的值作为新值
            log.debug("审计字段变更: entityId={}", entityId);
        }

        // 6. 执行目标方法
        Object result = joinPoint.proceed();

        // 7. 记录审计日志
        try {
            recordAuditLog(annotation, command, entityId, signature);
        } catch (Exception e) {
            log.warn("记录审计日志失败", e);
        }

        return result;
    }

    /**
     * 从命令对象中提取实体ID
     */
    private Long extractEntityId(Object command, String idParam) {
        try {
            Field idField = findField(command.getClass(), idParam);
            if (idField != null) {
                idField.setAccessible(true);
                Object value = idField.get(command);
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
        } catch (Exception e) {
            log.debug("无法提取实体ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 记录审计日志
     */
    private void recordAuditLog(AuditField annotation, Object command, Long entityId, MethodSignature signature) {
        String module = annotation.module();
        if (module.isEmpty()) {
            module = signature.getDeclaringType().getSimpleName();
        }

        String action = annotation.action();
        String[] fields = annotation.fields();

        // 获取当前用户
        String username = "anonymous";
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
            username = SecurityUtils.getUsername();
        } catch (Exception ignored) {
        }

        // 构建变更描述
        StringBuilder changes = new StringBuilder();
        if (fields.length > 0) {
            Set<String> fieldSet = new HashSet<>(Arrays.asList(fields));
            List<FieldChange> fieldChanges = extractFieldValues(command, fieldSet);
            changes.append(FieldChangeUtils.formatChanges(fieldChanges));
        } else {
            changes.append("更新实体 ID=").append(entityId);
        }

        // 记录日志
        log.info("字段审计 | 模块:{} | 操作:{} | 用户:{}({}) | 实体ID:{} | 变更:{}",
                module, action, username, userId, entityId, changes);
    }

    /**
     * 提取字段值（用于记录新值）
     */
    private List<FieldChange> extractFieldValues(Object obj, Set<String> fieldNames) {
        // 使用 FieldChangeUtils 对比 null 和当前对象，获取所有字段值
        return FieldChangeUtils.compare(null, obj, fieldNames);
    }
}
