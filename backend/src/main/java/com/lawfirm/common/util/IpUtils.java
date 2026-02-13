package com.lawfirm.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;

/**
 * IP地址工具类
 *
 * <p>功能： - 获取请求真实IP地址（支持多级代理） - 判断内网/公网IP - IP地址验证
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public final class IpUtils {

  /** 未知IP标识. */
  private static final String UNKNOWN = "unknown";

  /** 本地回环地址 IPv4. */
  private static final String LOCALHOST_IPV4 = "127.0.0.1";

  /** 本地回环地址 IPv6. */
  private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

  /** IP分隔符. */
  private static final String SEPARATOR = ",";

  /** IPv4 最大段值. */
  private static final int MAX_IPV4_PART = 255;

  /** A类私有地址前缀. */
  private static final int IP_A_CLASS = 10;

  /** B类私有地址前缀. */
  private static final int IP_B_CLASS_START = 172;

  /** B类私有地址范围起始. */
  private static final int IP_B_CLASS_RANGE_START = 16;

  /** B类私有地址范围结束. */
  private static final int IP_B_CLASS_RANGE_END = 31;

  /** C类私有地址前缀1. */
  private static final int IP_C_CLASS_START = 192;

  /** C类私有地址前缀2. */
  private static final int IP_C_CLASS_SEC = 168;

  /** 回环地址前缀. */
  private static final int IP_LOOPBACK = 127;

  /** 位移 24. */
  private static final int SHIFT_24 = 24;

  /** 位移 16. */
  private static final int SHIFT_16 = 16;

  /** 位移 8. */
  private static final int SHIFT_8 = 8;

  /** 掩码 0xFF. */
  private static final int MASK_FF = 0xFF;

  /** 代理服务器可能使用的Header，按优先级排序 */
  private static final String[] HEADERS = {
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_CLIENT_IP",
    "HTTP_X_FORWARDED_FOR",
    "X-Real-IP"
  };

  private IpUtils() {
    // 工具类，禁止实例化
  }

  /**
   * 获取请求的真实IP地址
   *
   * @param request HTTP请求
   * @return 真实IP地址
   */
  public static String getIpAddr(final HttpServletRequest request) {
    if (request == null) {
      return UNKNOWN;
    }

    String ip = null;

    // 依次检查各个代理Header
    for (String header : HEADERS) {
      ip = request.getHeader(header);
      if (isNotEmpty(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
        break;
      }
    }

    // 如果所有Header都没有，使用RemoteAddr
    if (isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    if (ip == null || ip.isEmpty()) {
      return UNKNOWN;
    }

    // 对于通过多个代理的情况，第一个IP为客户端真实IP
    // 格式: clientIP, proxy1IP, proxy2IP
    String result = ip;
    if (result.contains(SEPARATOR)) {
      String[] ips = result.split(SEPARATOR);
      for (String ipAddr : ips) {
        if (isNotEmpty(ipAddr) && !UNKNOWN.equalsIgnoreCase(ipAddr.trim())) {
          result = ipAddr.trim();
          break;
        }
      }
    }

    // 处理IPv6本地地址
    if (LOCALHOST_IPV6.equals(result)) {
      result = LOCALHOST_IPV4;
    }

    // 如果是本地地址，尝试获取本机IP
    if (LOCALHOST_IPV4.equals(result)) {
      try {
        result = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e) {
        log.warn("获取本机IP地址失败", e);
      }
    }

    return result;
  }

  /**
   * 获取本机IP地址
   *
   * @return 本机IP地址
   */
  public static String getLocalIP() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostAddress();
    } catch (UnknownHostException e) {
      log.error("获取本机IP地址失败", e);
      return UNKNOWN;
    }
  }

  /**
   * 检查IP地址格式是否合法（IPv4）
   *
   * @param ip IP地址
   * @return true-合法，false-不合法
   */
  public static boolean isValidIPv4(final String ip) {
    if (isEmpty(ip)) {
      return false;
    }
    String[] parts = ip.split("\\.");
    if (parts.length != 4) {
      return false;
    }
    for (String part : parts) {
      try {
        int value = Integer.parseInt(part);
        if (value < 0 || value > MAX_IPV4_PART) {
          return false;
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return true;
  }

  /**
   * 判断是否是内网IP
   *
   * <p>内网IP范围： - 10.0.0.0 - 10.255.255.255 (A类) - 172.16.0.0 - 172.31.255.255 (B类) - 192.168.0.0 -
   * 192.168.255.255 (C类) - 127.0.0.0 - 127.255.255.255 (回环地址)
   *
   * @param ip IP地址
   * @return true-内网IP，false-公网IP
   */
  public static boolean isInternalIP(final String ip) {
    if (!isValidIPv4(ip)) {
      return false;
    }

    String[] parts = ip.split("\\.");
    int firstPart = Integer.parseInt(parts[0]);
    int secondPart = Integer.parseInt(parts[1]);

    // 10.0.0.0 - 10.255.255.255 (A类私有地址)
    if (firstPart == IP_A_CLASS) {
      return true;
    }

    // 172.16.0.0 - 172.31.255.255 (B类私有地址)
    if (firstPart == IP_B_CLASS_START
        && (secondPart >= IP_B_CLASS_RANGE_START && secondPart <= IP_B_CLASS_RANGE_END)) {
      return true;
    }

    // 192.168.0.0 - 192.168.255.255 (C类私有地址)
    if (firstPart == IP_C_CLASS_START && secondPart == IP_C_CLASS_SEC) {
      return true;
    }

    // 127.0.0.0 - 127.255.255.255 (回环地址)
    if (firstPart == IP_LOOPBACK) {
      return true;
    }

    return false;
  }

  /**
   * 判断是否是公网IP
   *
   * @param ip IP地址
   * @return true-公网IP，false-内网IP或无效IP
   */
  public static boolean isPublicIP(final String ip) {
    return isValidIPv4(ip) && !isInternalIP(ip);
  }

  /**
   * 判断是否是本地回环地址
   *
   * @param ip IP地址
   * @return true-本地地址，false-非本地地址
   */
  public static boolean isLocalhost(final String ip) {
    return LOCALHOST_IPV4.equals(ip)
        || LOCALHOST_IPV6.equals(ip)
        || "localhost".equalsIgnoreCase(ip);
  }

  /**
   * 获取IP地址描述
   *
   * @param ip IP地址
   * @return 描述信息
   */
  public static String getIpDescription(final String ip) {
    if (isEmpty(ip) || UNKNOWN.equals(ip)) {
      return "未知";
    }
    if (isLocalhost(ip)) {
      return "本地";
    }
    if (isInternalIP(ip)) {
      return "内网";
    }
    if (isPublicIP(ip)) {
      return "公网";
    }
    return "未知";
  }

  /**
   * 将IP地址转换为长整型（用于IP范围比较）
   *
   * @param ip IP地址
   * @return 长整型值，无效IP返回-1
   */
  public static long ipToLong(final String ip) {
    if (!isValidIPv4(ip)) {
      return -1;
    }
    String[] parts = ip.split("\\.");
    long result = 0;
    for (int i = 0; i < 4; i++) {
      result = result << 8 | Integer.parseInt(parts[i]);
    }
    return result;
  }

  /**
   * 将长整型转换为IP地址
   *
   * @param ipLong IP的长整型表示
   * @return IP地址字符串
   */
  public static String longToIp(final long ipLong) {
    return ((ipLong >> SHIFT_24) & MASK_FF)
        + "."
        + ((ipLong >> SHIFT_16) & MASK_FF)
        + "."
        + ((ipLong >> SHIFT_8) & MASK_FF)
        + "."
        + (ipLong & MASK_FF);
  }

  /**
   * 检查IP是否在指定范围内
   *
   * @param ip 要检查的IP
   * @param startIp 起始IP
   * @param endIp 结束IP
   * @return true-在范围内，false-不在范围内
   */
  public static boolean isInRange(final String ip, final String startIp, final String endIp) {
    long ipValue = ipToLong(ip);
    long startValue = ipToLong(startIp);
    long endValue = ipToLong(endIp);

    if (ipValue < 0 || startValue < 0 || endValue < 0) {
      return false;
    }

    return ipValue >= startValue && ipValue <= endValue;
  }

  private static boolean isEmpty(final String str) {
    return str == null || str.trim().isEmpty();
  }

  private static boolean isNotEmpty(final String str) {
    return !isEmpty(str);
  }
}
