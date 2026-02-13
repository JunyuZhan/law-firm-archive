package com.archivesystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("档案管理系统 API")
                        .version("1.0.0")
                        .description("""
                                档案管理系统API文档
                                
                                ## 功能概述
                                - 接收律所系统归档档案
                                - 档案入库管理
                                - 档案借阅管理
                                - 档案检索查询
                                - 多来源档案收集
                                
                                ## 对接说明
                                - 律所系统通过 `/api/open/law-firm/archive/receive` 接口推送归档档案
                                - 需要在请求头中携带 API 密钥进行认证
                                """)
                        .contact(new Contact()
                                .name("技术支持")
                                .email("support@archive-system.com"))
                        .license(new License()
                                .name("私有")
                                .url("")))
                .servers(List.of(
                        new Server().url("/api").description("档案管理系统API")
                ));
    }
}
