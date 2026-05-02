package com.archivesystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RegistryUpgradeProperties.class, DistCenterProperties.class})
public class RegistryUpgradeConfiguration {
}
