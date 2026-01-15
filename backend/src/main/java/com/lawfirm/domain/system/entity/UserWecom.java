package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户企业微信绑定实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_wecom")
public class UserWecom implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 系统用户ID */
    private Long userId;

    /** 企业微信UserId（用于@） */
    private String wecomUserid;

    /** 企业微信绑定手机号（备用） */
    private String wecomMobile;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
