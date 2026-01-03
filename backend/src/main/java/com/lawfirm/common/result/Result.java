package com.lawfirm.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class Result<T> implements Serializable {
    
    private boolean success;
    private String code;
    private String message;
    private T data;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "200", "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, "200", message, data);
    }

    public static <T> Result<T> error(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return error("500", message);
    }

    public static <T> Result<T> badRequest(String message) {
        return error("400", message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return error("401", message);
    }

    public static <T> Result<T> forbidden(String message) {
        return error("403", message);
    }

    public static <T> Result<T> notFound(String message) {
        return error("404", message);
    }
}
