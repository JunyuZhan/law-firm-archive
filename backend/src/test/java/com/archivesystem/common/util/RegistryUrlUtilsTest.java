package com.archivesystem.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryUrlUtilsTest {

    @Test
    void stripsCatalogSuffix() {
        assertEquals("https://hub.albertzhan.top",
                RegistryUrlUtils.normalizeRegistryBaseUrl("https://hub.albertzhan.top/v2/_catalog"));
    }

    @Test
    void stripsV2Suffix() {
        assertEquals("https://example.com",
                RegistryUrlUtils.normalizeRegistryBaseUrl("https://example.com/v2/"));
    }

    @Test
    void keepsPlainBase() {
        assertEquals("https://registry.example.org:5000",
                RegistryUrlUtils.normalizeRegistryBaseUrl("https://registry.example.org:5000"));
    }
}
