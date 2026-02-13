package com.archivesystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 是否成功 */
    private Boolean success;

    /** 状态码 */
    private String code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /**
     * 成功响应.
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "200", "success", null);
    }

    /**
     * 成功响应（带数据）.
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "200", "success", data);
    }

    /**
     * 成功响应（带消息和数据）.
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, "200", message, data);
    }

    /**
     * 失败响应.
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, "500", message, null);
    }

    /**
     * 失败响应（带状态码）.
     */
    public static <T> Result<T> error(String code, String message) {
        return new Result<>(false, code, message, null);
    }
}
