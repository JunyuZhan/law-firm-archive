package com.lawfirm.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI 配置.
 *
 * <p>仅在开发/测试环境启用，生产环境应禁用
 *
 * @author system
 * @since 2026-01-17
 */
@Configuration
@Profile({"dev", "test"})
public class OpenApiConfig {

  /**
   * 创建自定义 OpenAPI 配置.
   *
   * @return OpenAPI 配置对象
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("智慧律所管理系统 API")
                .description("Law Firm Management System RESTful API Documentation")
                .version("1.0.0")
                .contact(new Contact().name("技术支持").email("support@lawfirm.com"))
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT认证，格式：Bearer {token}")));
  }
}
