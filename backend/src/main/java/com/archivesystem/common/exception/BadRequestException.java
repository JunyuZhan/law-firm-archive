package com.archivesystem.common.exception;

/**
 * 非法请求异常.
 * @author junyuzhan
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
