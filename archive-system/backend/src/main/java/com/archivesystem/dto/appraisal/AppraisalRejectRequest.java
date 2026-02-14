package com.archivesystem.dto.appraisal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 鉴定拒绝请求DTO.
 */
@Data
@Schema(description = "鉴定拒绝请求")
public class AppraisalRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    @Size(min = 2, max = 500, message = "拒绝原因长度应在2-500字之间")
    @Schema(description = "拒绝原因", example = "鉴定理由不充分", required = true)
    private String comment;
}
