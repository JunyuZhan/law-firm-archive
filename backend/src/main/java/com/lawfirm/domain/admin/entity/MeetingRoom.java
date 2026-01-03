package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 会议室实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_room")
public class MeetingRoom extends BaseEntity {

    /** 会议室名称 */
    private String name;

    /** 会议室编码 */
    private String code;

    /** 位置 */
    private String location;

    /** 容纳人数 */
    private Integer capacity;

    /** 设备 */
    private String equipment;

    /** 描述 */
    private String description;

    /** 状态: AVAILABLE可用/OCCUPIED占用/MAINTENANCE维护中 */
    private String status;

    /** 是否启用 */
    private Boolean enabled;

    /** 排序 */
    private Integer sortOrder;

    // 状态常量
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_OCCUPIED = "OCCUPIED";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";
}
