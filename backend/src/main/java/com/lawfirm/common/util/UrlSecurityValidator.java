package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * URL安全验证器 用于防止SSRF（服务器端请求伪造）攻击
 *
 * <p>验证项目： 1. 协议白名单（只允许 http/https） 2. 禁止访问内网IP 3. 禁止访问云元数据端点
 */
@Slf4j
@Component
public class UrlSecurityValidator {

  /** 只允许的协议 */
  private static final Set<String> ALLOWED_PROTOCOLS = Set.of("http", "https");

  /** 字节掩码. */
  private static final int BYTE_MASK = 0xFF;

  /** 链路本地地址第一部分. */
  private static final int LINK_LOCAL_PART1 = 169;

  /** 链路本地地址第二部分. */
  private static final int LINK_LOCAL_PART2 = 254;

  /**
   * 验证URL是否安全（防止SSRF攻击）
   *
   * @param urlString 待验证的URL
   * @throws BusinessException 如果URL不安全
   */
  public void validateUrl(final String urlString) {
    if (urlString == null || urlString.trim().isEmpty()) {
      throw new BusinessException("URL不能为空");
    }

    URL url;
    try {
      url = new URI(urlString).toURL();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new BusinessException("无效的URL格式");
    }

    // 1. 验证协议
    String protocol = url.getProtocol().toLowerCase();
    if (!ALLOWED_PROTOCOLS.contains(protocol)) {
      log.warn("SSRF防护: 拒绝不安全的协议 - url={}, protocol={}", urlString, protocol);
      throw new BusinessException("只支持HTTP/HTTPS协议");
    }

    // 2. 解析并验证IP地址
    String host = url.getHost();
    try {
      InetAddress address = InetAddress.getByName(host);
      String ip = address.getHostAddress();

      if (isInternalIp(address)) {
        log.warn("SSRF防护: 拒绝内网地址 - url={}, ip={}", urlString, ip);
        throw new BusinessException("禁止访问内网地址");
      }
    } catch (UnknownHostException e) {
      throw new BusinessException("无法解析域名: " + host);
    }

    log.debug("URL安全检查通过: {}", urlString);
  }

  /**
   * 检查是否为内网IP
   *
   * @param address IP地址
   * @return 是否为内网IP
   */
  private boolean isInternalIp(final InetAddress address) {
    // 回环地址 (127.0.0.0/8)
    if (address.isLoopbackAddress()) {
      return true;
    }

    // 链路本地地址 (169.254.0.0/16) - 包括云主机元数据地址
    if (address.isLinkLocalAddress()) {
      return true;
    }

    // 站点本地地址 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
    if (address.isSiteLocalAddress()) {
      return true;
    }

    // 任播地址
    if (address.isAnyLocalAddress()) {
      return true;
    }

    // 特殊检查：AWS/阿里云等元数据地址 169.254.169.254
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int b0 = bytes[0] & BYTE_MASK;
      int b1 = bytes[1] & BYTE_MASK;

      // 169.254.x.x 链路本地
      if (b0 == LINK_LOCAL_PART1 && b1 == LINK_LOCAL_PART2) {
        return true;
      }

      // 0.0.0.0/8 特殊地址
      if (b0 == 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * 验证是否为图片URL（可选的额外验证）
   *
   * @param urlString URL字符串
   */
  public void validateImageUrl(final String urlString) {
    // 先进行基本安全检查
    validateUrl(urlString);

    // 检查URL后缀（简单验证）
    String lowerUrl = urlString.toLowerCase();
    if (!lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg)(\\?.*)?$")) {
      log.warn("URL可能不是图片: {}", urlString);
      // 只记录警告，不阻止（因为实际Content-Type需要请求后才知道）
    }
  }
}
