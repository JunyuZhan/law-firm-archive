package com.archivesystem.dto.destruction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 销毁申请请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "销毁申请请求")
public class DestructionApplyRequest {

    @NotNull(message = "档案ID不能为空")
    @Schema(description = "档案ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long archiveId;

    @NotBlank(message = "销毁原因不能为空")
    @Size(max = 1000, message = "销毁原因不能超过1000字")
    @Schema(description = "销毁原因", example = "保管期限已到，经鉴定无保存价值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destructionReason;

    @NotBlank(message = "销毁方式不能为空")
    @Schema(description = "销毁方式：LOGICAL(逻辑删除)、PHYSICAL(物理销毁)", example = "LOGICAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destructionMethod;
}
