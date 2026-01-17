package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UrlSecurityValidator 单元测试
 *
 * 测试URL安全验证功能（防止SSRF攻击）
 */
@DisplayName("UrlSecurityValidator URL安全验证测试")
class UrlSecurityValidatorTest {

    private final UrlSecurityValidator validator = new UrlSecurityValidator();

    // ========== validateUrl 基础测试 ==========

    @Test
    @DisplayName("null URL 应该抛出异常")
    void validateUrl_shouldThrowForNull() {
        assertThatThrownBy(() -> validator.validateUrl(null))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("URL不能为空");
    }

    @Test
    @DisplayName("空字符串 URL 应该抛出异常")
    void validateUrl_shouldThrowForEmpty() {
        assertThatThrownBy(() -> validator.validateUrl(""))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("URL不能为空");
    }

    @Test
    @DisplayName("空白字符串 URL 应该抛出异常")
    void validateUrl_shouldThrowForBlank() {
        assertThatThrownBy(() -> validator.validateUrl("   "))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("URL不能为空");
    }

    @Test
    @DisplayName("无效的URL格式应该抛出异常")
    void validateUrl_shouldThrowForInvalidFormat() {
        assertThatThrownBy(() -> validator.validateUrl("not a valid url"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("无效的URL格式");
    }

    // ========== 协议验证测试 ==========

    @Test
    @DisplayName("HTTP 协议应该通过验证")
    void validateUrl_shouldAllowHttp() {
        assertThatCode(() -> validator.validateUrl("http://www.baidu.com"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HTTPS 协议应该通过验证")
    void validateUrl_shouldAllowHttps() {
        assertThatCode(() -> validator.validateUrl("https://www.baidu.com"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FTP 协议应该被拒绝")
    void validateUrl_shouldRejectFtp() {
        assertThatThrownBy(() -> validator.validateUrl("ftp://www.baidu.com"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只支持HTTP/HTTPS协议");
    }

    @Test
    @DisplayName("file:// 协议应该被拒绝")
    void validateUrl_shouldRejectFileProtocol() {
        assertThatThrownBy(() -> validator.validateUrl("file:///etc/passwd"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("只支持HTTP/HTTPS协议");
    }

    // ========== SSRF 防护测试 - 内网地址 ==========

    @Test
    @DisplayName("应该拒绝访问回环地址 127.0.0.1")
    void validateUrl_shouldRejectLoopbackAddress() {
        assertThatThrownBy(() -> validator.validateUrl("http://127.0.0.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    @DisplayName("应该拒绝访问 localhost")
    void validateUrl_shouldRejectLocalhost() {
        assertThatThrownBy(() -> validator.validateUrl("http://localhost"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    @DisplayName("应该拒绝访问 0.0.0.0")
    void validateUrl_shouldRejectZeroAddress() {
        assertThatThrownBy(() -> validator.validateUrl("http://0.0.0.0"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    // ========== SSRF 防护测试 - 私有地址 ==========

    @Test
    @DisplayName("应该拒绝访问 10.0.0.0/8 网段")
    void validateUrl_shouldReject10PrivateNetwork() {
        assertThatThrownBy(() -> validator.validateUrl("http://10.0.0.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    @DisplayName("应该拒绝访问 172.16.0.0/12 网段")
    void validateUrl_shouldReject172PrivateNetwork() {
        assertThatThrownBy(() -> validator.validateUrl("http://172.16.0.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    @DisplayName("应该拒绝访问 192.168.0.0/16 网段")
    void validateUrl_shouldReject192PrivateNetwork() {
        assertThatThrownBy(() -> validator.validateUrl("http://192.168.1.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    // ========== SSRF 防护测试 - 链路本地地址 ==========

    @Test
    @DisplayName("应该拒绝访问 169.254.0.0/16 网段（云元数据地址）")
    void validateUrl_shouldRejectLinkLocalAddress() {
        assertThatThrownBy(() -> validator.validateUrl("http://169.254.169.254"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    // ========== 合法公网地址测试 ==========

    @Test
    @DisplayName("应该允许访问公网IP地址")
    void validateUrl_shouldAllowPublicIp() {
        assertThatCode(() -> validator.validateUrl("http://8.8.8.8"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该允许访问公网域名")
    void validateUrl_shouldAllowPublicDomain() {
        assertThatCode(() -> validator.validateUrl("https://www.baidu.com"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该允许访问带端口的公网地址")
    void validateUrl_shouldAllowPublicDomainWithPort() {
        assertThatCode(() -> validator.validateUrl("https://www.baidu.com:443"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("应该允许访问带路径的公网地址")
    void validateUrl_shouldAllowPublicDomainWithPath() {
        assertThatCode(() -> validator.validateUrl("https://www.baidu.com/s?wd=test"))
            .doesNotThrowAnyException();
    }

    // ========== validateImageUrl 测试 ==========

    @Test
    @DisplayName("validateImageUrl: null 应该抛出异常")
    void validateImageUrl_shouldThrowForNull() {
        assertThatThrownBy(() -> validator.validateImageUrl(null))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("validateImageUrl: 应该允许合法的图片URL")
    void validateImageUrl_shouldAllowValidImageUrl() {
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.jpg"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateImageUrl: 应该允许各种图片格式")
    void validateImageUrl_shouldAllowVariousImageFormats() {
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.jpeg"))
            .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.png"))
            .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.gif"))
            .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.webp"))
            .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.svg"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateImageUrl: 应该拒绝内网图片URL")
    void validateImageUrl_shouldRejectInternalImageUrl() {
        assertThatThrownBy(() -> validator.validateImageUrl("http://localhost/image.jpg"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("禁止访问内网地址");
    }

    @Test
    @DisplayName("validateImageUrl: 应该允许带查询参数的图片URL")
    void validateImageUrl_shouldAllowImageUrlWithQuery() {
        assertThatCode(() -> validator.validateImageUrl("https://www.baidu.com/image.jpg?t=123"))
            .doesNotThrowAnyException();
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("应该正确处理IPv6格式的localhost")
    void validateUrl_shouldHandleIPv6Localhost() {
        assertThatThrownBy(() -> validator.validateUrl("http://[::1]"))
            .isInstanceOf(BusinessException.class);
    }
}
