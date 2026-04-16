package com.archivesystem.config;

import com.archivesystem.common.util.ClientIpUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 客户端 IP 解析配置.
 */
@Configuration
public class ClientIpConfig {

    @Value("${security.trusted-proxies:127.0.0.1,::1}")
    private String trustedProxies;

    @PostConstruct
    public void configureTrustedProxies() {
        List<String> proxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        ClientIpUtils.configureTrustedProxies(proxies);
    }
}
