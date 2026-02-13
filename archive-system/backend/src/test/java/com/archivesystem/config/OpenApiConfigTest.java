package com.archivesystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void testCustomOpenAPI() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        assertNotNull(openAPI);
    }

    @Test
    void testOpenAPIInfo() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("档案管理系统 API", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertNotNull(info.getDescription());
        assertTrue(info.getDescription().contains("档案管理系统"));
    }

    @Test
    void testOpenAPIContact() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("技术支持", openAPI.getInfo().getContact().getName());
        assertEquals("support@archive-system.com", openAPI.getInfo().getContact().getEmail());
    }

    @Test
    void testOpenAPILicense() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        assertNotNull(openAPI.getInfo().getLicense());
        assertEquals("私有", openAPI.getInfo().getLicense().getName());
    }

    @Test
    void testOpenAPIServers() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        assertNotNull(openAPI.getServers());
        assertFalse(openAPI.getServers().isEmpty());
        assertEquals("/api", openAPI.getServers().get(0).getUrl());
        assertEquals("档案管理系统API", openAPI.getServers().get(0).getDescription());
    }

    @Test
    void testDescriptionContainsFeatures() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        String description = openAPI.getInfo().getDescription();

        assertTrue(description.contains("接收律所系统归档档案"));
        assertTrue(description.contains("档案入库管理"));
        assertTrue(description.contains("档案借阅管理"));
        assertTrue(description.contains("档案检索查询"));
        assertTrue(description.contains("多来源档案收集"));
    }

    @Test
    void testDescriptionContainsApiEndpoint() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        String description = openAPI.getInfo().getDescription();

        assertTrue(description.contains("/api/open/law-firm/archive/receive"));
    }
}
