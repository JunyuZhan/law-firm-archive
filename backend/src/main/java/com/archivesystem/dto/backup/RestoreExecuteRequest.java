package com.archivesystem.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Long targetId;

    @NotBlank
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
    private String confirmationText;
}
