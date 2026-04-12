package com.archivesystem.dto.appraisal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 鉴定申请请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "鉴定申请请求")
public class AppraisalCreateRequest {

    @NotNull(message = "档案ID不能为空")
    @Schema(description = "档案ID", example = "1", required = true)
    private Long archiveId;

    @NotBlank(message = "鉴定类型不能为空")
    @Schema(description = "鉴定类型：RETENTION(保管期限)、SECURITY(密级)、DESTRUCTION(销毁)", example = "RETENTION", required = true)
    private String appraisalType;

    @Schema(description = "原值", example = "Y10")
    private String originalValue;

    @Schema(description = "新值", example = "Y30")
    private String newValue;

    @NotBlank(message = "鉴定原因不能为空")
    @Size(max = 1000, message = "鉴定原因不能超过1000字")
    @Schema(description = "鉴定原因", example = "该档案具有重要历史价值，建议延长保管期限", required = true)
    private String appraisalReason;
}
