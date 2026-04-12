package com.archivesystem.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统交付信息.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemDeliveryInfoDTO {

    private String deliveryMode;
    private String artifactType;
    private String recommendedDeployment;
    private String deploymentDirectory;
    private String sourceDirectory;
    private String upgradeOwner;
    private Boolean sourceCodeIncluded;
    private List<String> notes;
    private List<DeliveryDocumentDTO> documents;
}
