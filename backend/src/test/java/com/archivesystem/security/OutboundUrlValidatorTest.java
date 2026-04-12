package com.archivesystem.security;

import com.archivesystem.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
/**
 * @author junyuzhan
 */

class OutboundUrlValidatorTest {

    private final OutboundUrlValidator validator = new OutboundUrlValidator();

    @Test
    void testValidate_AllowsPublicHttpsUrl() {
        assertDoesNotThrow(() -> validator.validate("https://8.8.8.8/callback", "回调地址"));
    }

    @Test
    void testValidate_RejectsLoopbackHost() {
        assertThrows(BusinessException.class,
                () -> validator.validate("http://127.0.0.1/internal", "下载地址"));
    }

    @Test
    void testValidate_RejectsLinkLocalHost() {
        assertThrows(BusinessException.class,
                () -> validator.validate("http://169.254.169.254/latest/meta-data", "下载地址"));
    }

    @Test
    void testValidate_RejectsUnsupportedScheme() {
        assertThrows(BusinessException.class,
                () -> validator.validate("file:///etc/passwd", "下载地址"));
    }
}
