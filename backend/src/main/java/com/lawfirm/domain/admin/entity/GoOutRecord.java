package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 外出登记实体（M8-005）
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("go_out_record")
public class GoOutRecord extends BaseEntity {

    /**
     * 登记编号
     */
    private String recordNo;

    /**
     * 外出人ID
     */
    private Long userId;

    /**
     * 外出时间
     */
    private LocalDateTime outTime;

    /**
     * 预计返回时间
     */
    private LocalDateTime expectedReturnTime;

    /**
     * 实际返回时间
     */
    private LocalDateTime actualReturnTime;

    /**
     * 外出地点
     */
    private String location;

    /**
     * 外出事由
     */
    private String reason;

    /**
     * 同行人员
     */
    private String companions;

    /**
     * 状态：OUT-外出中, RETURNED-已返回
     */
    private String status;

    public static final String STATUS_OUT = "OUT";
    public static final String STATUS_RETURNED = "RETURNED";
}

