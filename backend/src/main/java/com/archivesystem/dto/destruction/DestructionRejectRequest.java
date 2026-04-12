package com.archivesystem.dto.destruction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 销毁拒绝请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "销毁拒绝请求")
public class DestructionRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    @Size(min = 2, max = 500, message = "拒绝原因长度应在2-500字之间")
    @Schema(description = "拒绝原因", example = "档案仍具有保存价值", required = true)
    private String comment;
}
