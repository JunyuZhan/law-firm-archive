package com.lawfirm.common.base;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO基类 - 所有DTO继承此类
 */
@Data
public abstract class BaseDTO implements Serializable {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

