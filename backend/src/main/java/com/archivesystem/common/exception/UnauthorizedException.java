package com.archivesystem.common.exception;

/**
 * 未授权异常.
 * @author junyuzhan
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super("401", message);
    }
}
