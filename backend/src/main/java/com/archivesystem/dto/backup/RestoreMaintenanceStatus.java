package com.archivesystem.dto.backup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复维护模式状态.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreMaintenanceStatus {

    private Boolean enabled;
    private Boolean restoreRequiresMaintenance;
    private String message;
}
