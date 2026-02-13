package com.lawfirm.common.exception;

import lombok.Getter;

/** 业务异常 */
@Getter
public class BusinessException extends RuntimeException {

  /** 错误代码 */
  private final String code;

  /**
   * 构造函数
   *
   * @param message 异常消息
   */
  public BusinessException(final String message) {
    super(message);
    this.code = "500";
  }

  /**
   * 构造函数
   *
   * @param code 错误代码
   * @param message 异常消息
   */
  public BusinessException(final String code, final String message) {
    super(message);
    this.code = code;
  }

  /**
   * 构造函数
   *
   * @param message 异常消息
   * @param cause 原因
   */
  public BusinessException(final String message, final Throwable cause) {
    super(message, cause);
    this.code = "500";
  }

  public String getCode() {
    return code;
  }
}
