package com.archivesystem.dto.borrow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 借阅申请请求DTO.
 * @author junyuzhan
 */
@Data
@Schema(description = "借阅申请请求")
public class BorrowApplyRequest {

    @NotNull(message = "档案ID不能为空")
    @Schema(description = "档案ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long archiveId;

    @NotBlank(message = "借阅目的不能为空")
    @Size(max = 500, message = "借阅目的不能超过500字")
    @Schema(description = "借阅目的", example = "案件审理需要", requiredMode = Schema.RequiredMode.REQUIRED)
    private String borrowPurpose;

    @NotNull(message = "预计归还日期不能为空")
    @Future(message = "预计归还日期必须是将来的日期")
    @Schema(description = "预计归还日期", example = "2026-03-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate expectedReturnDate;

    @NotBlank(message = "借阅方式不能为空")
    @Pattern(regexp = "ONLINE|DOWNLOAD|COPY", message = "借阅方式不合法")
    @Schema(description = "借阅方式", example = "ONLINE", allowableValues = {"ONLINE", "DOWNLOAD", "COPY"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String borrowType;

    @Size(max = 500, message = "备注不能超过500字")
    @Schema(description = "备注", example = "请尽快审批")
    private String remarks;

    /**
     * 兼容前端传入的 purpose 字段.
     */
    public void setPurpose(String purpose) {
        if (this.borrowPurpose == null || this.borrowPurpose.isEmpty()) {
            this.borrowPurpose = purpose;
        }
    }
}
