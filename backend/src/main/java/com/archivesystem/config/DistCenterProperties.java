package com.archivesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 与 Dist Center 分发站联动（versions/latest.json）。
 */
@Data
@ConfigurationProperties(prefix = "app.dist-center")
public class DistCenterProperties {

    /**
     * 例如 https://install.example.com/projects/law-firm-archive/versions/latest.json
     * 可被「规则与运行参数」中 system.upgrade.dist_center_latest_json_url 覆盖。
     */
    private String latestJsonUrl = "";
}
