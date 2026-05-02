package com.archivesystem.controller;

import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.archivesystem.security.ApiKeyAuthFilter;
import com.archivesystem.security.OutboundUrlValidator;
import com.archivesystem.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SourceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExternalSourceMapper externalSourceMapper;

    @Mock
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @Mock
    private OutboundUrlValidator outboundUrlValidator;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SourceController sourceController;

    private ObjectMapper objectMapper;
    private ExternalSource testSource;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sourceController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testSource = new ExternalSource();
        testSource.setId(1L);
        testSource.setSourceCode("LAW_FIRM");
        testSource.setSourceName("律所管理系统");
        testSource.setApiUrl("http://lawfirm.example.com");
        testSource.setApiKey("api-key-123");
        testSource.setExtraConfig(Map.of("callbackUrl", "http://callback.example.com"));
        testSource.setEnabled(true);
        testSource.setDeleted(false);
        testSource.setCreatedAt(LocalDateTime.now());
        testSource.setCreatedBy(7L);
        testSource.setUpdatedBy(8L);
        
        when(apiKeyAuthFilter.generateApiKey()).thenReturn("new-api-key-123");
        doNothing().when(apiKeyAuthFilter).clearApiKeyCache(anyString());
    }

    @Test
    void testList() throws Exception {
        when(externalSourceMapper.selectList(ArgumentMatchers.<LambdaQueryWrapper<ExternalSource>>any()))
                .thenReturn(Arrays.asList(testSource));

        mockMvc.perform(get("/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].sourceCode").value("LAW_FIRM"))
                .andExpect(jsonPath("$.data[0].description").doesNotExist())
                .andExpect(jsonPath("$.data[0].apiKey").doesNotExist())
                .andExpect(jsonPath("$.data[0].extraConfig").doesNotExist())
                .andExpect(jsonPath("$.data[0].lastSyncMessage").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedBy").doesNotExist())
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());
    }

    @Test
    void testGetById_Found() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);

        mockMvc.perform(get("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.sourceCode").value("LAW_FIRM"))
                .andExpect(jsonPath("$.data.sourceName").value("律所管理系统"))
                .andExpect(jsonPath("$.data.apiKey").doesNotExist())
                .andExpect(jsonPath("$.data.extraConfig.callbackUrl").value("http://callback.example.com"))
                .andExpect(jsonPath("$.data.lastSyncMessage").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/sources/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testGetById_Deleted() throws Exception {
        testSource.setDeleted(true);
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);

        mockMvc.perform(get("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testCreate() throws Exception {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setSourceType("COURT");
        newSource.setApiUrl("http://court.example.com");

        when(apiKeyAuthFilter.hashApiKeyPublic("new-api-key-123")).thenReturn("hashed-new-api-key");
        when(externalSourceMapper.insert(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("创建成功，请保存 API Key"))
                .andExpect(jsonPath("$.data.sourceCode").value("COURT"))
                .andExpect(jsonPath("$.data.sourceName").value("法院系统"))
                .andExpect(jsonPath("$.data.apiKey").value("new-api-key-123"))
                .andExpect(jsonPath("$.data.apiUrl").doesNotExist())
                .andExpect(jsonPath("$.data.authType").doesNotExist())
                .andExpect(jsonPath("$.data.extraConfig").doesNotExist())
                .andExpect(jsonPath("$.data.lastSyncAt").doesNotExist())
                .andExpect(jsonPath("$.data.lastSyncStatus").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist());

        verify(externalSourceMapper).insert(any(ExternalSource.class));
        verify(apiKeyAuthFilter).clearApiKeyCache("hashed-new-api-key");
    }

    @Test
    void testCreate_DuplicateApiKeyShouldBeRejected() throws Exception {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setSourceType("COURT");
        newSource.setApiKey("plain-api-key");

        ExternalSource duplicate = new ExternalSource();
        duplicate.setId(2L);
        duplicate.setApiKey("hashed-api-key");

        when(apiKeyAuthFilter.hashApiKeyPublic("plain-api-key")).thenReturn("hashed-api-key");
        when(externalSourceMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<ExternalSource>>any()))
                .thenReturn(null)
                .thenReturn(duplicate);

        mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("API Key 已被其他来源使用"));

        verify(externalSourceMapper, never()).insert(any(ExternalSource.class));
    }

    @Test
    void testCreate_UnsupportedAuthTypeShouldBeRejected() throws Exception {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setSourceType("COURT");
        newSource.setAuthType("OAUTH2");

        assertThrows(BusinessException.class, () -> sourceController.create(newSource));
        verify(externalSourceMapper, never()).insert(any(ExternalSource.class));
    }

    @Test
    void testCreate_UnsupportedSourceTypeShouldBeRejected() throws Exception {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("CUSTOM");
        newSource.setSourceName("自定义系统");
        newSource.setSourceType("CUSTOM");

        assertThrows(BusinessException.class, () -> sourceController.create(newSource));
        verify(externalSourceMapper, never()).insert(any(ExternalSource.class));
    }

    @Test
    void testCreate_InvalidCallbackUrlShouldBeRejected() {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setSourceType("COURT");
        newSource.setExtraConfig(Map.of("callbackUrl", "javascript:alert(1)"));

        doThrow(new BusinessException("回调地址格式不安全")).when(outboundUrlValidator)
                .validate("javascript:alert(1)", "回调地址");

        assertThrows(BusinessException.class, () -> sourceController.create(newSource));
        verify(externalSourceMapper, never()).insert(any(ExternalSource.class));
    }

    @Test
    void testCreate_InvalidApiUrlShouldBeRejected() {
        ExternalSource newSource = new ExternalSource();
        newSource.setSourceCode("COURT");
        newSource.setSourceName("法院系统");
        newSource.setSourceType("COURT");
        newSource.setApiUrl("javascript:alert(1)");

        doThrow(new BusinessException("API地址格式不安全")).when(outboundUrlValidator)
                .validate("javascript:alert(1)", "API地址");

        assertThrows(BusinessException.class, () -> sourceController.create(newSource));
        verify(externalSourceMapper, never()).insert(any(ExternalSource.class));
    }

    @Test
    void testRegenerateApiKey_ShouldClearNewInvalidCacheMarker() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(apiKeyAuthFilter.hashApiKeyPublic("new-api-key-123")).thenReturn("hashed-new-api-key");
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(post("/sources/1/regenerate-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.apiKey").value("new-api-key-123"));

        verify(apiKeyAuthFilter).clearApiKeyCache("api-key-123");
        verify(apiKeyAuthFilter).clearApiKeyCache("hashed-new-api-key");
        verify(externalSourceMapper).updateById(argThat(source ->
                "hashed-new-api-key".equals(source.getApiKey())));
    }

    @Test
    void testUpdate() throws Exception {
        ExternalSource updateSource = new ExternalSource();
        updateSource.setSourceCode("LAW_FIRM");
        updateSource.setSourceName("更新后的名称");
        updateSource.setSourceType("LAW_FIRM");
        updateSource.setApiUrl("http://new-url.example.com");

        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("更新成功"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getId().equals(1L)));
    }

    @Test
    void testUpdate_DuplicateApiKeyShouldBeRejected() throws Exception {
        ExternalSource updateSource = new ExternalSource();
        updateSource.setSourceCode("LAW_FIRM");
        updateSource.setSourceName("更新后的名称");
        updateSource.setSourceType("LAW_FIRM");
        updateSource.setApiUrl("http://new-url.example.com");
        updateSource.setApiKey("plain-api-key");

        ExternalSource duplicate = new ExternalSource();
        duplicate.setId(2L);
        duplicate.setApiKey("hashed-api-key");

        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(apiKeyAuthFilter.hashApiKeyPublic("plain-api-key")).thenReturn("hashed-api-key");
        when(externalSourceMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<ExternalSource>>any()))
                .thenReturn(null)
                .thenReturn(duplicate);

        mockMvc.perform(put("/sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("API Key 已被其他来源使用"));

        verify(externalSourceMapper, never()).updateById(any(ExternalSource.class));
    }

    @Test
    void testUpdate_DuplicateSourceCode() throws Exception {
        ExternalSource updateSource = new ExternalSource();
        updateSource.setSourceCode("COURT");
        updateSource.setSourceName("更新后的名称");
        updateSource.setSourceType("LAW_FIRM");

        ExternalSource duplicate = new ExternalSource();
        duplicate.setId(2L);
        duplicate.setSourceCode("COURT");

        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.selectOne(ArgumentMatchers.<LambdaQueryWrapper<ExternalSource>>any())).thenReturn(duplicate);

        mockMvc.perform(put("/sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.message").value("来源编码已存在"));

        verify(externalSourceMapper, never()).updateById(any(ExternalSource.class));
    }

    @Test
    void testUpdate_NotFound() throws Exception {
        ExternalSource updateSource = new ExternalSource();
        updateSource.setSourceCode("LAW_FIRM");
        updateSource.setSourceName("更新后的名称");
        updateSource.setSourceType("LAW_FIRM");

        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(put("/sources/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testToggle_Enable() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已启用"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getEnabled()));
    }

    @Test
    void testToggle_Disable() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(put("/sources/1/toggle")
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("已禁用"));
    }

    @Test
    void testToggle_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(put("/sources/999/toggle")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testTestConnection() throws Exception {
        // 没有API URL时，应明确返回失败
        testSource.setApiUrl(null);
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(post("/sources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("请先配置API地址后再测试连接"));
    }

    @Test
    void testTestConnection_RemoteSuccess() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);
        when(restTemplate.exchange(eq("http://lawfirm.example.com"), eq(org.springframework.http.HttpMethod.HEAD),
                isNull(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        mockMvc.perform(post("/sources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("连接测试成功"));
    }

    @Test
    void testTestConnection_RemoteFailureShouldHideInternalErrorDetails() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);
        when(restTemplate.exchange(eq("http://lawfirm.example.com"), eq(org.springframework.http.HttpMethod.HEAD),
                isNull(), eq(String.class)))
                .thenThrow(new RuntimeException("connect timed out to http://lawfirm.example.com/internal/health"));

        mockMvc.perform(post("/sources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("502"))
                .andExpect(jsonPath("$.message").value("连接失败，请检查API地址、网络和服务状态"));

        verify(externalSourceMapper).updateById(argThat(source ->
                "FAILED".equals(source.getLastSyncStatus())
                        && "连接失败，请检查API地址、网络和服务状态".equals(source.getLastSyncMessage())));
    }

    @Test
    void testTestConnection_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(post("/sources/999/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("来源不存在"));
    }

    @Test
    void testDelete() throws Exception {
        when(externalSourceMapper.selectById(1L)).thenReturn(testSource);
        when(externalSourceMapper.updateById(any(ExternalSource.class))).thenReturn(1);

        mockMvc.perform(delete("/sources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(externalSourceMapper).updateById(argThat(source -> source.getDeleted()));
    }

    @Test
    void testDelete_NotFound() throws Exception {
        when(externalSourceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(delete("/sources/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(externalSourceMapper, never()).updateById(any());
    }
}
