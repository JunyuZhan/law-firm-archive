package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 档案存放位置实体类.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("archive_location")
public class ArchiveLocation extends BaseEntity {

    /** 位置编码 */
    @NotBlank(message = "位置编码不能为空")
    @Size(max = 50, message = "位置编码长度不能超过50个字符")
    private String locationCode;

    /** 位置名称 */
    @NotBlank(message = "位置名称不能为空")
    @Size(max = 100, message = "位置名称长度不能超过100个字符")
    private String locationName;

    /** 库房名称 */
    private String roomName;

    /** 区域 */
    private String area;

    /** 架号 */
    private String shelfNo;

    /** 层号 */
    private String layerNo;

    /** 总容量 */
    private Integer totalCapacity;

    /** 已用容量 */
    private Integer usedCapacity;

    /** 状态：AVAILABLE-可用, FULL-已满, DISABLED-停用 */
    private String status;

    /** 备注 */
    private String remarks;

    // ===== 状态常量 =====
    
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_FULL = "FULL";
    public static final String STATUS_DISABLED = "DISABLED";
}
