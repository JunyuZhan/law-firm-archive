package com.archivesystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI配置.
 * 仅在非生产环境启用，生产环境禁用 Swagger 文档
 * @author junyuzhan
 */
@Configuration
@Profile("!prod")
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
                                - 律所系统通过 `/api/open/archive/receive` 接口推送归档档案
                                - 除公开借阅访问与健康检查外，其余 `/api/open/**` 请求都需要在请求头中携带 `X-API-Key`
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
