package com.archivesystem.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复执行请求.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreExecuteRequest {

    @NotNull
    @Positive(message = "targetId 必须为正数")
    private Long targetId;

    @NotBlank
    @Size(max = 128, message = "backupSetName 长度不能超过128")
    private String backupSetName;

    @Builder.Default
    private Boolean restoreDatabase = true;

    @Builder.Default
    private Boolean restoreFiles = true;

    @Builder.Default
    private Boolean restoreConfig = true;

    @Builder.Default
    private Boolean rebuildIndex = true;

    @Builder.Default
    private Boolean exitMaintenanceAfterSuccess = true;

    @NotBlank
    @Size(max = 32, message = "confirmationText 长度不能超过32")
    private String confirmationText;
}
