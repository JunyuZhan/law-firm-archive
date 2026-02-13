package com.lawfirm.common.result;

import java.io.Serializable;
import lombok.Data;

/**
 * 统一响应结果
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

  /** 是否成功. */
  private boolean success;

  /** 响应码. */
  private String code;

  /** 响应消息. */
  private String message;

  /** 响应数据. */
  private T data;

  /** 时间戳. */
  private long timestamp;

  /** 默认构造函数 */
  public Result() {
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * 构造函数
   *
   * @param success 是否成功
   * @param code 响应码
   * @param message 响应消息
   * @param data 响应数据
   */
  public Result(final boolean success, final String code, final String message, final T data) {
    this.success = success;
    this.code = code;
    this.message = message;
    this.data = data;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * 返回成功结果（无数据）
   *
   * @param <T> 数据类型
   * @return 成功结果
   */
  public static <T> Result<T> success() {
    return success(null);
  }

  /**
   * 返回成功结果（带数据）
   *
   * @param <T> 数据类型
   * @param data 响应数据
   * @return 成功结果
   */
  public static <T> Result<T> success(final T data) {
    return new Result<>(true, "200", "操作成功", data);
  }

  /**
   * 返回成功结果（带消息和数据）
   *
   * @param <T> 数据类型
   * @param message 响应消息
   * @param data 响应数据
   * @return 成功结果
   */
  public static <T> Result<T> success(final String message, final T data) {
    return new Result<>(true, "200", message, data);
  }

  /**
   * 返回错误结果
   *
   * @param <T> 数据类型
   * @param code 错误码
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> error(final String code, final String message) {
    return new Result<>(false, code, message, null);
  }

  /**
   * 返回错误结果（带数据）
   *
   * @param <T> 数据类型
   * @param code 错误码
   * @param message 错误消息
   * @param data 响应数据
   * @return 错误结果
   */
  public static <T> Result<T> error(final String code, final String message, final T data) {
    return new Result<>(false, code, message, data);
  }

  /**
   * 返回错误结果（默认错误码 500）
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> error(final String message) {
    return error("500", message);
  }

  /**
   * 返回 400 错误结果
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> badRequest(final String message) {
    return error("400", message);
  }

  /**
   * 返回 401 错误结果
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> unauthorized(final String message) {
    return error("401", message);
  }

  /**
   * 返回 403 错误结果
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> forbidden(final String message) {
    return error("403", message);
  }

  /**
   * 返回 404 错误结果
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> notFound(final String message) {
    return error("404", message);
  }

  /**
   * 返回失败结果（fail 是 error 的别名）
   *
   * @param <T> 数据类型
   * @param message 错误消息
   * @return 错误结果
   */
  public static <T> Result<T> fail(final String message) {
    return error("500", message);
  }
}
