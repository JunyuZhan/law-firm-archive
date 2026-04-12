package com.archivesystem.common.exception;

/**
 * 资源不存在异常.
 * @author junyuzhan
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super("404", message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException(String.format("%s不存在: %s", resource, id));
    }
}
