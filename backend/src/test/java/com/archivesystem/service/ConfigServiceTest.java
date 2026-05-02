package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private SecretCryptoService secretCryptoService;

    @InjectMocks
    private ConfigServiceImpl configService;

    private SysConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new SysConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.key");
        testConfig.setConfigValue("test-value");
        testConfig.setConfigGroup("SYSTEM");
        testConfig.setDescription("测试配置");
        testConfig.setEditable(true);
    }

    @Test
    void testGetAllGrouped() {
        SysConfig config1 = new SysConfig();
        config1.setConfigKey("key1");
        config1.setConfigValue("value1");
        config1.setConfigGroup("GROUP_A");

        SysConfig config2 = new SysConfig();
        config2.setConfigKey("key2");
        config2.setConfigValue("value2");
        config2.setConfigGroup("GROUP_B");

        SysConfig config3 = new SysConfig();
        config3.setConfigKey("key3");
        config3.setConfigValue("value3");
        config3.setConfigGroup(null); // 无分组

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2, config3));

        Map<String, List<SysConfig>> result = configService.getAllGrouped();

        assertNotNull(result);
        assertTrue(result.containsKey("GROUP_A"));
        assertTrue(result.containsKey("GROUP_B"));
        assertTrue(result.containsKey("OTHER")); // null 分组归入 OTHER
    }

    @Test
    void testGetAll() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));

        List<SysConfig> result = configService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.key", result.get(0).getConfigKey());
    }

    @Test
    void testGetByGroup() {
        when(configMapper.selectByGroup("SYSTEM")).thenReturn(Arrays.asList(testConfig));

        List<SysConfig> result = configService.getByGroup("SYSTEM");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetByKey_Success() {
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        SysConfig result = configService.getByKey("test.key");

        assertNotNull(result);
        assertEquals("test-value", result.getConfigValue());
    }

    @Test
    void testGetByKey_SensitiveValueShouldBeRedacted() {
        SysConfig sensitiveConfig = new SysConfig();
        sensitiveConfig.setConfigKey("security.crypto.secret");
        sensitiveConfig.setConfigValue("super-secret-value");

        when(configMapper.selectByKey("security.crypto.secret")).thenReturn(sensitiveConfig);

        SysConfig result = configService.getByKey("security.crypto.secret");

        assertEquals("******", result.getConfigValue());
    }

    @Test
    void testGetByGroup_SensitiveValueShouldBeRedacted() {
        SysConfig sensitiveConfig = new SysConfig();
        sensitiveConfig.setConfigKey("system.site.logo.object");
        sensitiveConfig.setConfigValue("site/logo/internal-object.png");
        sensitiveConfig.setConfigGroup("SITE");

        when(configMapper.selectByGroup("SITE")).thenReturn(List.of(sensitiveConfig));

        List<SysConfig> result = configService.getByGroup("SITE");

        assertEquals(1, result.size());
        assertEquals("******", result.get(0).getConfigValue());
    }

    @Test
    void testGetByKey_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> configService.getByKey("not.exists"));
    }

    @Test
    void testGetValue() {
        // 首先刷新缓存
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
        configService.refreshCache();

        String result = configService.getValue("test.key");

        assertEquals("test-value", result);
    }

    @Test
    void testGetValue_NotInCache() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        String result = configService.getValue("not.in.cache");

        assertNull(result);
    }

    @Test
    void testGetValue_WithDefault() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        String result = configService.getValue("not.exists", "default-value");

        assertEquals("default-value", result);
    }

    @Test
    void testGetValue_WithDefault_ValueExists() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
        configService.refreshCache();

        String result = configService.getValue("test.key", "default-value");

        assertEquals("test-value", result);
    }

    @Test
    void testGetIntValue() {
        SysConfig intConfig = new SysConfig();
        intConfig.setConfigKey("int.key");
        intConfig.setConfigValue("42");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(intConfig));
        configService.refreshCache();

        Integer result = configService.getIntValue("int.key", 0);

        assertEquals(42, result);
    }

    @Test
    void testGetIntValue_Default() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        Integer result = configService.getIntValue("not.exists", 100);

        assertEquals(100, result);
    }

    @Test
    void testGetIntValue_InvalidFormat() {
        SysConfig invalidConfig = new SysConfig();
        invalidConfig.setConfigKey("invalid.int");
        invalidConfig.setConfigValue("not-a-number");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(invalidConfig));
        configService.refreshCache();

        Integer result = configService.getIntValue("invalid.int", 50);

        assertEquals(50, result);
    }

    @Test
    void testGetBooleanValue_True() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("true");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", false);

        assertTrue(result);
    }

    @Test
    void testGetBooleanValue_One() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("1");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", false);

        assertTrue(result);
    }

    @Test
    void testGetBooleanValue_False() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("false");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", true);

        assertFalse(result);
    }

    @Test
    void testGetBooleanValue_Default() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("not.exists", true);

        assertTrue(result);
    }

    @Test
    void testUpdateConfig_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(configMapper.selectByKey("test.key")).thenReturn(testConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
            when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
            configService.refreshCache();

            assertDoesNotThrow(() -> configService.updateConfig("test.key", "new-value"));

            verify(configMapper).updateById(argThat(config -> 
                "new-value".equals(config.getConfigValue())));
        }
    }

    @Test
    void testUpdateConfig_NullValueShouldClearCache() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(configMapper.selectByKey("test.key")).thenReturn(testConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
            when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
            configService.refreshCache();

            assertDoesNotThrow(() -> configService.updateConfig("test.key", null));

            verify(configMapper).updateById(argThat(config -> config.getConfigValue() == null));
            assertNull(configService.getValue("test.key"));
        }
    }

    @Test
    void testUpdateConfig_RedactedSensitivePlaceholderShouldKeepOriginalValue() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig sensitiveConfig = new SysConfig();
            sensitiveConfig.setId(2L);
            sensitiveConfig.setConfigKey("system.upgrade.registry_password");
            sensitiveConfig.setConfigValue("real-secret");
            sensitiveConfig.setEditable(true);

            when(configMapper.selectByKey("system.upgrade.registry_password")).thenReturn(sensitiveConfig);

            assertDoesNotThrow(() -> configService.updateConfig("system.upgrade.registry_password", "******"));

            verify(configMapper, never()).updateById(any(SysConfig.class));
        }
    }

    @Test
    void testUpdateConfig_NumberTypeShouldRejectInvalidValue() {
        SysConfig numberConfig = new SysConfig();
        numberConfig.setId(3L);
        numberConfig.setConfigKey("system.upload.max.size");
        numberConfig.setConfigType(SysConfig.TYPE_NUMBER);
        numberConfig.setConfigValue("104857600");
        numberConfig.setEditable(true);

        when(configMapper.selectByKey("system.upload.max.size")).thenReturn(numberConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.upload.max.size", "abc"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_NumberTypeShouldRejectBlankValue() {
        SysConfig numberConfig = new SysConfig();
        numberConfig.setId(3L);
        numberConfig.setConfigKey("system.search.page.size");
        numberConfig.setConfigType(SysConfig.TYPE_NUMBER);
        numberConfig.setConfigValue("20");
        numberConfig.setEditable(true);

        when(configMapper.selectByKey("system.search.page.size")).thenReturn(numberConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.search.page.size", "   "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_BooleanTypeShouldRejectInvalidValue() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setId(4L);
        boolConfig.setConfigKey("system.mail.smtp.ssl");
        boolConfig.setConfigType(SysConfig.TYPE_BOOLEAN);
        boolConfig.setConfigValue("true");
        boolConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.smtp.ssl")).thenReturn(boolConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.smtp.ssl", "maybe"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_AllowedUploadTypesShouldRejectBlankList() {
        SysConfig uploadTypesConfig = new SysConfig();
        uploadTypesConfig.setId(5L);
        uploadTypesConfig.setConfigKey("system.upload.allowed.types");
        uploadTypesConfig.setConfigType(SysConfig.TYPE_STRING);
        uploadTypesConfig.setConfigValue("pdf,docx");
        uploadTypesConfig.setEditable(true);

        when(configMapper.selectByKey("system.upload.allowed.types")).thenReturn(uploadTypesConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.upload.allowed.types", " , , "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_AllowedUploadTypesShouldRejectInvalidExtension() {
        SysConfig uploadTypesConfig = new SysConfig();
        uploadTypesConfig.setId(5L);
        uploadTypesConfig.setConfigKey("system.upload.allowed.types");
        uploadTypesConfig.setConfigType(SysConfig.TYPE_STRING);
        uploadTypesConfig.setConfigValue("pdf,docx");
        uploadTypesConfig.setEditable(true);

        when(configMapper.selectByKey("system.upload.allowed.types")).thenReturn(uploadTypesConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.upload.allowed.types", "pdf,.exe"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_AllowedUploadTypesShouldNormalizeValue() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig uploadTypesConfig = new SysConfig();
            uploadTypesConfig.setId(5L);
            uploadTypesConfig.setConfigKey("system.upload.allowed.types");
            uploadTypesConfig.setConfigType(SysConfig.TYPE_STRING);
            uploadTypesConfig.setConfigValue("pdf,docx");
            uploadTypesConfig.setEditable(true);

            when(configMapper.selectByKey("system.upload.allowed.types")).thenReturn(uploadTypesConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

            assertDoesNotThrow(() -> configService.updateConfig("system.upload.allowed.types", " PDF, docx ,pdf "));

            verify(configMapper).updateById(argThat(config ->
                    "pdf,docx".equals(config.getConfigValue())));
        }
    }

    @Test
    void testUpdateConfig_RegistryBaseUrlShouldRejectInvalidScheme() {
        SysConfig urlConfig = new SysConfig();
        urlConfig.setId(6L);
        urlConfig.setConfigKey("system.upgrade.registry_base_url");
        urlConfig.setConfigType(SysConfig.TYPE_STRING);
        urlConfig.setConfigValue("https://hub.example.com");
        urlConfig.setEditable(true);

        when(configMapper.selectByKey("system.upgrade.registry_base_url")).thenReturn(urlConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.upgrade.registry_base_url", "ftp://hub.example.com"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_DistCenterUrlShouldRejectMissingHost() {
        SysConfig urlConfig = new SysConfig();
        urlConfig.setId(7L);
        urlConfig.setConfigKey("system.upgrade.dist_center_latest_json_url");
        urlConfig.setConfigType(SysConfig.TYPE_STRING);
        urlConfig.setConfigValue("https://install.example/latest.json");
        urlConfig.setEditable(true);

        when(configMapper.selectByKey("system.upgrade.dist_center_latest_json_url")).thenReturn(urlConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.upgrade.dist_center_latest_json_url", "https:///latest.json"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_MailFromShouldRejectInvalidEmail() {
        SysConfig emailConfig = new SysConfig();
        emailConfig.setId(8L);
        emailConfig.setConfigKey("system.mail.from");
        emailConfig.setConfigType(SysConfig.TYPE_STRING);
        emailConfig.setConfigValue("archive@example.com");
        emailConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.from")).thenReturn(emailConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.from", "bad-address"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_AdminEmailsShouldNormalizeAndDeduplicate() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig emailConfig = new SysConfig();
            emailConfig.setId(9L);
            emailConfig.setConfigKey("system.notify.admin.emails");
            emailConfig.setConfigType(SysConfig.TYPE_STRING);
            emailConfig.setConfigValue("ops@example.com");
            emailConfig.setEditable(true);

            when(configMapper.selectByKey("system.notify.admin.emails")).thenReturn(emailConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

            assertDoesNotThrow(() -> configService.updateConfig(
                    "system.notify.admin.emails",
                    " ops@example.com,sec@example.com,ops@example.com "
            ));

            verify(configMapper).updateById(argThat(config ->
                    "ops@example.com,sec@example.com".equals(config.getConfigValue())));
        }
    }

    @Test
    void testUpdateConfig_SmtpHostShouldRejectProtocolPrefix() {
        SysConfig hostConfig = new SysConfig();
        hostConfig.setId(10L);
        hostConfig.setConfigKey("system.mail.smtp.host");
        hostConfig.setConfigType(SysConfig.TYPE_STRING);
        hostConfig.setConfigValue("smtp.example.com");
        hostConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.smtp.host")).thenReturn(hostConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.smtp.host", "https://smtp.example.com"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_SmtpHostShouldRejectPathSegment() {
        SysConfig hostConfig = new SysConfig();
        hostConfig.setId(10L);
        hostConfig.setConfigKey("system.mail.smtp.host");
        hostConfig.setConfigType(SysConfig.TYPE_STRING);
        hostConfig.setConfigValue("smtp.example.com");
        hostConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.smtp.host")).thenReturn(hostConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.smtp.host", "smtp.example.com/mail"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_SmtpPortShouldRejectNonPositiveValue() {
        SysConfig portConfig = new SysConfig();
        portConfig.setId(10L);
        portConfig.setConfigKey("system.mail.smtp.port");
        portConfig.setConfigType(SysConfig.TYPE_NUMBER);
        portConfig.setConfigValue("587");
        portConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.smtp.port")).thenReturn(portConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.smtp.port", "0"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_SmtpPortShouldRejectOutOfRangeValue() {
        SysConfig portConfig = new SysConfig();
        portConfig.setId(10L);
        portConfig.setConfigKey("system.mail.smtp.port");
        portConfig.setConfigType(SysConfig.TYPE_NUMBER);
        portConfig.setConfigValue("587");
        portConfig.setEditable(true);

        when(configMapper.selectByKey("system.mail.smtp.port")).thenReturn(portConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.mail.smtp.port", "70000"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_BackupCronShouldRejectInvalidExpression() {
        SysConfig cronConfig = new SysConfig();
        cronConfig.setId(11L);
        cronConfig.setConfigKey("system.backup.cron");
        cronConfig.setConfigType(SysConfig.TYPE_STRING);
        cronConfig.setConfigValue("0 0 2 * * ?");
        cronConfig.setEditable(true);

        when(configMapper.selectByKey("system.backup.cron")).thenReturn(cronConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.backup.cron", "invalid-cron"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_BackupCronShouldRejectBlankValue() {
        SysConfig cronConfig = new SysConfig();
        cronConfig.setId(11L);
        cronConfig.setConfigKey("system.backup.cron");
        cronConfig.setConfigType(SysConfig.TYPE_STRING);
        cronConfig.setConfigValue("0 0 2 * * ?");
        cronConfig.setEditable(true);

        when(configMapper.selectByKey("system.backup.cron")).thenReturn(cronConfig);

        assertThrows(BusinessException.class, () -> configService.updateConfig("system.backup.cron", "   "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_MinioProxyPrefixShouldRejectAbsoluteUrl() {
        SysConfig proxyConfig = new SysConfig();
        proxyConfig.setId(12L);
        proxyConfig.setConfigKey("system.storage.minio.proxy-prefix");
        proxyConfig.setConfigType(SysConfig.TYPE_STRING);
        proxyConfig.setConfigValue("/storage");
        proxyConfig.setEditable(true);

        when(configMapper.selectByKey("system.storage.minio.proxy-prefix")).thenReturn(proxyConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("system.storage.minio.proxy-prefix", "https://cdn.example.com/storage"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_MinioProxyPrefixShouldRejectBlankValue() {
        SysConfig proxyConfig = new SysConfig();
        proxyConfig.setId(12L);
        proxyConfig.setConfigKey("system.storage.minio.proxy-prefix");
        proxyConfig.setConfigType(SysConfig.TYPE_STRING);
        proxyConfig.setConfigValue("/storage");
        proxyConfig.setEditable(true);

        when(configMapper.selectByKey("system.storage.minio.proxy-prefix")).thenReturn(proxyConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("system.storage.minio.proxy-prefix", "   "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_MinioProxyPrefixShouldNormalizeTrailingSlash() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig proxyConfig = new SysConfig();
            proxyConfig.setId(13L);
            proxyConfig.setConfigKey("system.storage.minio.proxy-prefix");
            proxyConfig.setConfigType(SysConfig.TYPE_STRING);
            proxyConfig.setConfigValue("/storage");
            proxyConfig.setEditable(true);

            when(configMapper.selectByKey("system.storage.minio.proxy-prefix")).thenReturn(proxyConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);

            assertDoesNotThrow(() ->
                    configService.updateConfig("system.storage.minio.proxy-prefix", "/storage/"));

            verify(configMapper).updateById(argThat(config ->
                    "/storage".equals(config.getConfigValue())));
        }
    }

    @Test
    void testUpdateConfig_ArchiveNoDateFormatShouldRejectInvalidPattern() {
        SysConfig dateFormatConfig = new SysConfig();
        dateFormatConfig.setId(14L);
        dateFormatConfig.setConfigKey("archive.no.date.format");
        dateFormatConfig.setConfigType(SysConfig.TYPE_STRING);
        dateFormatConfig.setConfigValue("yyyyMMdd");
        dateFormatConfig.setEditable(true);

        when(configMapper.selectByKey("archive.no.date.format")).thenReturn(dateFormatConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("archive.no.date.format", "yyyy-MM-]"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_ArchiveNoDateFormatShouldRejectBlankValue() {
        SysConfig dateFormatConfig = new SysConfig();
        dateFormatConfig.setId(14L);
        dateFormatConfig.setConfigKey("archive.no.date.format");
        dateFormatConfig.setConfigType(SysConfig.TYPE_STRING);
        dateFormatConfig.setConfigValue("yyyyMMdd");
        dateFormatConfig.setEditable(true);

        when(configMapper.selectByKey("archive.no.date.format")).thenReturn(dateFormatConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("archive.no.date.format", "   "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_ArchiveNoSeqDigitsShouldRejectNonPositiveValue() {
        SysConfig seqConfig = new SysConfig();
        seqConfig.setId(15L);
        seqConfig.setConfigKey("archive.no.seq.digits");
        seqConfig.setConfigType(SysConfig.TYPE_NUMBER);
        seqConfig.setConfigValue("4");
        seqConfig.setEditable(true);

        when(configMapper.selectByKey("archive.no.seq.digits")).thenReturn(seqConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("archive.no.seq.digits", "0"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_UploadMaxSizeShouldRejectNonPositiveValue() {
        SysConfig uploadSizeConfig = new SysConfig();
        uploadSizeConfig.setId(16L);
        uploadSizeConfig.setConfigKey("system.upload.max.size");
        uploadSizeConfig.setConfigType(SysConfig.TYPE_NUMBER);
        uploadSizeConfig.setConfigValue("104857600");
        uploadSizeConfig.setEditable(true);

        when(configMapper.selectByKey("system.upload.max.size")).thenReturn(uploadSizeConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("system.upload.max.size", "0"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_BackupKeepCountShouldRejectNonPositiveValue() {
        SysConfig keepCountConfig = new SysConfig();
        keepCountConfig.setId(17L);
        keepCountConfig.setConfigKey("system.backup.keep.count");
        keepCountConfig.setConfigType(SysConfig.TYPE_NUMBER);
        keepCountConfig.setConfigValue("7");
        keepCountConfig.setEditable(true);

        when(configMapper.selectByKey("system.backup.keep.count")).thenReturn(keepCountConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("system.backup.keep.count", "0"));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_ArchiveNoPrefixShouldRejectBlankValue() {
        SysConfig prefixConfig = new SysConfig();
        prefixConfig.setId(18L);
        prefixConfig.setConfigKey("archive.no.prefix.DOCUMENT");
        prefixConfig.setConfigType(SysConfig.TYPE_STRING);
        prefixConfig.setConfigValue("DOC");
        prefixConfig.setEditable(true);

        when(configMapper.selectByKey("archive.no.prefix.DOCUMENT")).thenReturn(prefixConfig);

        assertThrows(BusinessException.class, () ->
                configService.updateConfig("archive.no.prefix.DOCUMENT", "   "));
        verify(configMapper, never()).updateById(any(SysConfig.class));
    }

    @Test
    void testUpdateConfig_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            configService.updateConfig("not.exists", "value"));
    }

    @Test
    void testUpdateConfig_NotEditable() {
        testConfig.setEditable(false);
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> 
            configService.updateConfig("test.key", "new-value"));
    }

    @Test
    void testBatchUpdateConfigs_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig config1 = new SysConfig();
            config1.setConfigKey("key1");
            config1.setConfigValue("value1");
            config1.setEditable(true);

            SysConfig config2 = new SysConfig();
            config2.setConfigKey("key2");
            config2.setConfigValue("value2");
            config2.setEditable(true);

            when(configMapper.selectByKey("key1")).thenReturn(config1);
            when(configMapper.selectByKey("key2")).thenReturn(config2);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
            when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2));
            configService.refreshCache();

            Map<String, String> updates = Map.of("key1", "newValue1", "key2", "newValue2");
            assertDoesNotThrow(() -> configService.batchUpdateConfigs(updates));

            verify(configMapper, times(2)).updateById(any(SysConfig.class));
        }
    }

    @Test
    void testCreateConfig_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey("new.key");
            newConfig.setConfigValue("new-value");

            when(configMapper.selectByKey("new.key")).thenReturn(null);
            when(configMapper.insert(any(SysConfig.class))).thenReturn(1);

            SysConfig result = configService.createConfig(newConfig);

            assertNotNull(result);
            verify(configMapper).insert(any(SysConfig.class));
        }
    }

    @Test
    void testCreateConfig_SmtpPasswordShouldBeEncrypted() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey(ConfigServiceImpl.MAIL_SMTP_PASSWORD_KEY);
            newConfig.setConfigValue("plain-secret");

            when(configMapper.selectByKey(ConfigServiceImpl.MAIL_SMTP_PASSWORD_KEY)).thenReturn(null);
            when(secretCryptoService.encrypt("plain-secret")).thenReturn("enc:plain-secret");
            when(configMapper.insert(any(SysConfig.class))).thenReturn(1);

            SysConfig result = configService.createConfig(newConfig);

            assertNotNull(result);
            assertEquals("******", result.getConfigValue());
            verify(secretCryptoService).encrypt("plain-secret");
            verify(configMapper).insert(argThat(config ->
                    "enc:plain-secret".equals(config.getConfigValue())));
        }
    }

    @Test
    void testSaveConfig_SmtpPasswordShouldBeEncrypted() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(configMapper.selectByKey(ConfigServiceImpl.MAIL_SMTP_PASSWORD_KEY)).thenReturn(null);
            when(secretCryptoService.encrypt("plain-secret")).thenReturn("enc:plain-secret");
            when(configMapper.insert(any(SysConfig.class))).thenReturn(1);

            assertDoesNotThrow(() -> configService.saveConfig(
                    ConfigServiceImpl.MAIL_SMTP_PASSWORD_KEY,
                    "plain-secret",
                    SysConfig.GROUP_SYSTEM,
                    "SMTP 密码",
                    SysConfig.TYPE_STRING,
                    true,
                    1
            ));

            verify(secretCryptoService).encrypt("plain-secret");
            verify(configMapper).insert(argThat(config ->
                    "enc:plain-secret".equals(config.getConfigValue())));
        }
    }

    @Test
    void testSaveConfig_ProtectedRuntimeSecretShouldBeRejected() {
        assertThrows(BusinessException.class, () -> configService.saveConfig(
                RuntimeSecretProvider.KEY_JWT_SECRET,
                "manual-secret",
                SysConfig.GROUP_SYSTEM,
                "JWT 密钥",
                SysConfig.TYPE_STRING,
                false,
                90
        ));
        verify(configMapper, never()).selectByKey(RuntimeSecretProvider.KEY_JWT_SECRET);
    }

    @Test
    void testCreateConfig_KeyExists() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey("test.key");
        newConfig.setConfigValue("new-value");

        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> configService.createConfig(newConfig));
    }

    @Test
    void testCreateConfig_ProtectedRuntimeSecretShouldBeRejected() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey(RuntimeSecretProvider.KEY_JWT_SECRET);
        newConfig.setConfigValue("manual-secret");

        assertThrows(BusinessException.class, () -> configService.createConfig(newConfig));
        verify(configMapper, never()).insert(any(SysConfig.class));
    }

    @Test
    void testDeleteConfig_Success() {
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);
        when(configMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> configService.deleteConfig("test.key"));

        verify(configMapper).deleteById(1L);
    }

    @Test
    void testDeleteConfig_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> configService.deleteConfig("not.exists"));
    }

    @Test
    void testDeleteConfig_NotEditable() {
        testConfig.setEditable(false);
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> configService.deleteConfig("test.key"));
    }

    @Test
    void testUpdateConfig_ProtectedRuntimeSecretShouldBeRejected() {
        assertThrows(BusinessException.class, () ->
                configService.updateConfig(RuntimeSecretProvider.KEY_JWT_SECRET, "new-secret"));
        verify(configMapper, never()).selectByKey(RuntimeSecretProvider.KEY_JWT_SECRET);
    }

    @Test
    void testDeleteConfig_ProtectedRuntimeSecretShouldBeRejected() {
        assertThrows(BusinessException.class, () ->
                configService.deleteConfig(RuntimeSecretProvider.KEY_JWT_SECRET));
        verify(configMapper, never()).selectByKey(RuntimeSecretProvider.KEY_JWT_SECRET);
    }

    @Test
    void testRefreshCache() {
        SysConfig config1 = new SysConfig();
        config1.setConfigKey("key1");
        config1.setConfigValue("value1");

        SysConfig config2 = new SysConfig();
        config2.setConfigKey("key2");
        config2.setConfigValue(null); // null value

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2));

        configService.refreshCache();

        assertEquals("value1", configService.getValue("key1"));
        assertNull(configService.getValue("key2"));
    }

    @Test
    void testGetArchiveNoPrefix_Specific() {
        SysConfig prefixConfig = new SysConfig();
        prefixConfig.setConfigKey("archive.no.prefix.DOCUMENT");
        prefixConfig.setConfigValue("DOC");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(prefixConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("DOCUMENT");

        assertEquals("DOC", result);
    }

    @Test
    void testGetArchiveNoPrefix_Default() {
        SysConfig defaultConfig = new SysConfig();
        defaultConfig.setConfigKey("archive.no.prefix.DEFAULT");
        defaultConfig.setConfigValue("ARC");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(defaultConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("UNKNOWN_TYPE");

        assertEquals("ARC", result);
    }

    @Test
    void testGetArchiveNoPrefix_BlankSpecificShouldFallbackToDefault() {
        SysConfig prefixConfig = new SysConfig();
        prefixConfig.setConfigKey("archive.no.prefix.DOCUMENT");
        prefixConfig.setConfigValue("   ");

        SysConfig defaultConfig = new SysConfig();
        defaultConfig.setConfigKey("archive.no.prefix.DEFAULT");
        defaultConfig.setConfigValue("ARC");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(prefixConfig, defaultConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("DOCUMENT");

        assertEquals("ARC", result);
    }

    @Test
    void testGetArchiveNoPrefix_NoConfig() {
        // 当没有配置时，使用默认的 DEFAULT 前缀
        SysConfig defaultConfig = new SysConfig();
        defaultConfig.setConfigKey("archive.no.prefix.DEFAULT");
        defaultConfig.setConfigValue("ARC");
        
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(defaultConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("UNKNOWN_TYPE");

        // 当没有特定类型配置时，使用默认前缀
        assertEquals("ARC", result);
    }
}
