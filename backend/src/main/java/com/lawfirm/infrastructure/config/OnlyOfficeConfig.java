package com.lawfirm.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** OnlyOffice 文档服务配置 */
@Data
@Configuration
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeConfig {

  /** OnlyOffice Document Server 地址 */
  private String documentServerUrl = "http://localhost:8088";

  /** 后端回调地址（OnlyOffice 保存文档时调用） */
  private String callbackUrl = "http://host.docker.internal:8080/api";

  /** 文件服务地址（OnlyOffice 获取文件时使用） */
  private String fileServerUrl = "http://host.docker.internal:9000";

  /**
   * 外部访问地址（用于生成 OnlyOffice 可访问的文件 URL） 如果配置了此地址，OnlyOffice 将通过此地址访问文件（通过 Nginx 代理）
   * 格式：http://你的域名或IP（不包含 /api 路径）
   */
  private String externalAccessUrl;

  /** JWT 密钥 */
  private String jwtSecret;

  /** 是否启用 JWT */
  private boolean jwtEnabled = false;

  /**
   * 获取 OnlyOffice API JS 地址.
   *
   * @return OnlyOffice API JS 地址
   */
  public String getApiJsUrl() {
    return documentServerUrl + "/web-apps/apps/api/documents/api.js";
  }
}
