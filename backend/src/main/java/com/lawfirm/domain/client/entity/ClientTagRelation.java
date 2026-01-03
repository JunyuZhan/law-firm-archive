package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户标签关联实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("crm_client_tag_relation")
public class ClientTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

