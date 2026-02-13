package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 全宗实体类.
 * 对应数据库表: arc_fonds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("arc_fonds")
public class Fonds extends BaseEntity {

    /** 全宗号 */
    private String fondsNo;
    
    /** 全宗名称 */
    private String fondsName;
    
    /** 全宗类型 */
    private String fondsType;
    
    /** 说明 */
    private String description;
    
    /** 联系人 */
    private String contactPerson;
    
    /** 联系电话 */
    private String contactPhone;
    
    /** 状态 */
    @Builder.Default
    private String status = STATUS_ACTIVE;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String TYPE_INTERNAL = "INTERNAL";
    public static final String TYPE_EXTERNAL = "EXTERNAL";
}
