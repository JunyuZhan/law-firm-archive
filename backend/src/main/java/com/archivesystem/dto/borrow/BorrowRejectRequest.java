package com.archivesystem.dto.borrow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 借阅拒绝请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "借阅拒绝请求")
public class BorrowRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    @Size(min = 2, max = 500, message = "拒绝原因长度应在2-500字之间")
    @Schema(description = "拒绝原因", example = "档案正在整理中，暂不外借", required = true)
    private String reason;
}
