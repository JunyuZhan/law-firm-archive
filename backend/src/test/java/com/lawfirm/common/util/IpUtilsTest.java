package com.lawfirm.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * IpUtils 单元测试
 *
 * <p>测试IP工具功能： - 获取真实IP - IP格式验证 - 内网/公网IP判断 - IP描述
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@DisplayName("IpUtils 单元测试")
class IpUtilsTest {

  @Nested
  @DisplayName("获取真实IP")
  class GetIpAddrTests {

    @Test
    @DisplayName("从 X-Forwarded-For 获取IP")
    void getIpAddr_FromXForwardedFor() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Forwarded-For", "192.168.1.100");

      // When
      String ip = IpUtils.getIpAddr(request);

      // Then
      assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("从 X-Forwarded-For 获取多级代理的第一个IP")
    void getIpAddr_FromXForwardedFor_MultipleIps() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.1");

      // When
      String ip = IpUtils.getIpAddr(request);

      // Then
      assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("从 Proxy-Client-IP 获取IP")
    void getIpAddr_FromProxyClientIp() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Proxy-Client-IP", "192.168.1.101");

      // When
      String ip = IpUtils.getIpAddr(request);

      // Then
      assertEquals("192.168.1.101", ip);
    }

    @Test
    @DisplayName("从 RemoteAddr 获取IP")
    void getIpAddr_FromRemoteAddr() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("192.168.1.102");

      // When
      String ip = IpUtils.getIpAddr(request);

      // Then
      assertEquals("192.168.1.102", ip);
    }

    @Test
    @DisplayName("忽略 unknown 值")
    void getIpAddr_IgnoreUnknown() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Forwarded-For", "unknown");
      request.addHeader("Proxy-Client-IP", "192.168.1.103");

      // When
      String ip = IpUtils.getIpAddr(request);

      // Then
      assertEquals("192.168.1.103", ip);
    }

    @Test
    @DisplayName("null 请求返回 unknown")
    void getIpAddr_NullRequest() {
      // When
      String ip = IpUtils.getIpAddr(null);

      // Then
      assertEquals("unknown", ip);
    }
  }

  @Nested
  @DisplayName("IP格式验证")
  class ValidateIpTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "192.168.1.1",
          "10.0.0.1",
          "172.16.0.1",
          "8.8.8.8",
          "0.0.0.0",
          "255.255.255.255"
        })
    @DisplayName("有效的IPv4地址")
    void isValidIPv4_ValidIps(String ip) {
      assertTrue(IpUtils.isValidIPv4(ip));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "256.1.1.1",
          "1.256.1.1",
          "1.1.256.1",
          "1.1.1.256",
          "-1.1.1.1",
          "1.1.1",
          "1.1.1.1.1"
        })
    @DisplayName("无效的IPv4地址")
    void isValidIPv4_InvalidIps(String ip) {
      assertFalse(IpUtils.isValidIPv4(ip));
    }

    @Test
    @DisplayName("null 地址")
    void isValidIPv4_Null() {
      assertFalse(IpUtils.isValidIPv4(null));
    }

    @Test
    @DisplayName("空字符串")
    void isValidIPv4_Empty() {
      assertFalse(IpUtils.isValidIPv4(""));
    }
  }

  @Nested
  @DisplayName("内网IP判断")
  class InternalIpTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "10.0.0.1",
          "10.255.255.255",
          "172.16.0.1",
          "172.31.255.255",
          "192.168.0.1",
          "192.168.255.255"
        })
    @DisplayName("内网IP地址")
    void isInternalIP_InternalIps(String ip) {
      assertTrue(IpUtils.isInternalIP(ip));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "114.114.114.114", "1.1.1.1", "223.5.5.5"})
    @DisplayName("公网IP地址")
    void isInternalIP_PublicIps(String ip) {
      assertFalse(IpUtils.isInternalIP(ip));
    }

    @Test
    @DisplayName("边界值 - 172.15.x.x 不是内网")
    void isInternalIP_Boundary172_15() {
      assertFalse(IpUtils.isInternalIP("172.15.255.255"));
    }

    @Test
    @DisplayName("边界值 - 172.32.x.x 不是内网")
    void isInternalIP_Boundary172_32() {
      assertFalse(IpUtils.isInternalIP("172.32.0.1"));
    }
  }

  @Nested
  @DisplayName("公网IP判断")
  class PublicIpTests {

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "114.114.114.114"})
    @DisplayName("公网IP")
    void isPublicIP_PublicIps(String ip) {
      assertTrue(IpUtils.isPublicIP(ip));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0.1", "192.168.1.1"})
    @DisplayName("内网IP不是公网IP")
    void isPublicIP_InternalIps(String ip) {
      assertFalse(IpUtils.isPublicIP(ip));
    }
  }

  @Nested
  @DisplayName("本地地址判断")
  class LocalhostTests {

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "0:0:0:0:0:0:0:1"})
    @DisplayName("本地地址")
    void isLocalhost_LocalAddresses(String ip) {
      assertTrue(IpUtils.isLocalhost(ip));
    }

    @Test
    @DisplayName("非本地地址")
    void isLocalhost_NotLocal() {
      assertFalse(IpUtils.isLocalhost("192.168.1.1"));
    }
  }

  @Nested
  @DisplayName("IP描述")
  class IpDescriptionTests {

    @Test
    @DisplayName("本地地址描述")
    void getIpDescription_Localhost() {
      String desc = IpUtils.getIpDescription("127.0.0.1");
      assertEquals("本地", desc);
    }

    @Test
    @DisplayName("内网地址描述")
    void getIpDescription_Internal() {
      String desc = IpUtils.getIpDescription("192.168.1.1");
      assertEquals("内网", desc);
    }

    @Test
    @DisplayName("公网地址描述")
    void getIpDescription_Public() {
      String desc = IpUtils.getIpDescription("8.8.8.8");
      assertEquals("公网", desc);
    }
  }

  @Nested
  @DisplayName("IP转换")
  class IpConversionTests {

    @ParameterizedTest
    @CsvSource({"0.0.0.0, 0", "0.0.0.1, 1", "0.0.1.0, 256", "192.168.1.1, 3232235777"})
    @DisplayName("IP转Long")
    void ipToLong(String ip, long expected) {
      assertEquals(expected, IpUtils.ipToLong(ip));
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0.0.0", "1, 0.0.0.1", "256, 0.0.1.0", "3232235777, 192.168.1.1"})
    @DisplayName("Long转IP")
    void longToIp(long ipLong, String expected) {
      assertEquals(expected, IpUtils.longToIp(ipLong));
    }
  }

  @Nested
  @DisplayName("IP范围判断")
  class IpRangeTests {

    @Test
    @DisplayName("IP在范围内")
    void isInRange_InRange() {
      assertTrue(IpUtils.isInRange("192.168.1.100", "192.168.1.1", "192.168.1.255"));
    }

    @Test
    @DisplayName("IP不在范围内")
    void isInRange_OutOfRange() {
      assertFalse(IpUtils.isInRange("192.168.2.1", "192.168.1.1", "192.168.1.255"));
    }

    @Test
    @DisplayName("IP等于起始值")
    void isInRange_EqualStart() {
      assertTrue(IpUtils.isInRange("192.168.1.1", "192.168.1.1", "192.168.1.255"));
    }

    @Test
    @DisplayName("IP等于结束值")
    void isInRange_EqualEnd() {
      assertTrue(IpUtils.isInRange("192.168.1.255", "192.168.1.1", "192.168.1.255"));
    }
  }
}
