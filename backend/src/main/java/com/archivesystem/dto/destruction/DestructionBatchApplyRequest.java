package com.archivesystem.dto.destruction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量销毁申请请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "批量销毁申请请求")
public class DestructionBatchApplyRequest {

    @NotEmpty(message = "档案ID列表不能为空")
    @Schema(description = "档案ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> archiveIds;

    @NotBlank(message = "销毁原因不能为空")
    @Size(max = 1000, message = "销毁原因不能超过1000字")
    @Schema(description = "销毁原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destructionReason;

    @NotBlank(message = "销毁方式不能为空")
    @Schema(description = "销毁方式：LOGICAL(逻辑删除)、PHYSICAL(物理销毁)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destructionMethod;
}
