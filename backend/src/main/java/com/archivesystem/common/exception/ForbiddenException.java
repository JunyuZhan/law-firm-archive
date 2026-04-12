package com.archivesystem.common.exception;

/**
 * 禁止访问异常.
 * @author junyuzhan
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super("403", message);
    }
}
