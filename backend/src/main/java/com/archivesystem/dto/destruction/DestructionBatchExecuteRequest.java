package com.archivesystem.dto.destruction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量执行销毁请求DTO.
 */
@Data
@Schema(description = "批量执行销毁请求")
public class DestructionBatchExecuteRequest {

    @NotEmpty(message = "销毁记录ID列表不能为空")
    @Schema(description = "销毁记录ID列表", required = true)
    private List<Long> ids;

    @Size(max = 500, message = "备注不能超过500字")
    @Schema(description = "执行备注")
    private String remarks;
}
