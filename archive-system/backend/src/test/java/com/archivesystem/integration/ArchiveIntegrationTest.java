package com.archivesystem.integration;

import com.archivesystem.dto.archive.ArchiveCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 档案模块集成测试.
 */
@DisplayName("档案模块集成测试")
@TestMethodOrder(OrderAnnotation.class)
class ArchiveIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private static Long createdArchiveId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = getAdminToken();
    }

    @Test
    @Order(1)
    @DisplayName("创建档案 - 完整信息")
    @Disabled("需要完整的Mock配置来支持创建操作")
    void createArchive_WithValidData_ShouldSucceed() throws Exception {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("集成测试档案-合同纠纷案");
        request.setArchiveType("DOCUMENT");
        request.setFondsId(1L);
        request.setCategoryId(1L);
        request.setRetentionPeriod("Y10");
        request.setSecurityLevel("INTERNAL");
        request.setArchiveDate(LocalDate.now());

        String response = mockMvc.perform(post("/archives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.archiveNo").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("集成测试档案-合同纠纷案"))
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andReturn().getResponse().getContentAsString();

        // 保存创建的档案ID
        int start = response.indexOf("\"id\":") + 5;
        int end = response.indexOf(",", start);
        createdArchiveId = Long.parseLong(response.substring(start, end));
    }

    @Test
    @Order(2)
    @DisplayName("创建档案 - 缺少必填字段应返回400")
    void createArchive_WithMissingRequiredFields_ShouldFail() throws Exception {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setTitle("测试档案");
        // 缺少 retentionPeriod 和 archiveType

        mockMvc.perform(post("/archives")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("获取档案详情 - 存在的档案")
    @Disabled("需要完整的Mock配置，档案详情查询包含多个关联服务调用")
    void getArchiveById_WithExistingId_ShouldReturnArchive() throws Exception {
        // 使用测试数据中已存在的档案ID
        mockMvc.perform(get("/archives/{id}", 1L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @Order(4)
    @DisplayName("获取档案详情 - 不存在的档案应返回404")
    void getArchiveById_WithNonExistingId_ShouldReturnError() throws Exception {
        mockMvc.perform(get("/archives/{id}", 99999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("查询档案列表 - 分页查询")
    void queryArchives_WithPagination_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/archives")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("查询档案列表 - 按标题搜索")
    void queryArchives_WithTitleFilter_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/archives")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("title", "合同"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(7)
    @DisplayName("查询档案列表 - 按状态筛选")
    void queryArchives_WithStatusFilter_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/archives")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("更新档案 - 修改标题")
    @Disabled("需要完整的Mock配置来支持更新操作")
    void updateArchive_WithValidData_ShouldSucceed() throws Exception {
        String updateJson = "{\"title\": \"更新后的档案标题\", \"archiveType\": \"DOCUMENT\", \"retentionPeriod\": \"Y10\"}";

        mockMvc.perform(put("/archives/{id}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(9)
    @DisplayName("获取档案统计信息")
    void getArchiveStatistics_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/statistics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(10)
    @DisplayName("未认证访问档案接口 - 应返回403")
    void accessArchives_WithoutAuth_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/archives"))
                .andExpect(status().isForbidden());
    }
}
