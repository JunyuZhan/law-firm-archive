package com.lawfirm.common.util;

import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/** 版本信息工具类 用于获取应用版本号、构建时间等信息 */
@Slf4j
public final class VersionUtils {

  /** 配置文件名. */
  private static final String VERSION_PROPERTIES = "version.properties";

  /** 默认版本号. */
  private static final String DEFAULT_VERSION = "1.0.0";

  /** 未知标识. */
  private static final String UNKNOWN = "unknown";

  /** 应用版本号. */
  private static String version = null;

  /** 构建时间. */
  private static String buildTime = null;

  /** Git 提交 ID. */
  private static String gitCommit = null;

  static {
    loadVersionInfo();
  }

  private VersionUtils() {
    // 工具类，禁止实例化
  }

  /** 加载版本信息 优先级：数据库配置 > version.properties > 默认值 */
  private static void loadVersionInfo() {
    try {
      // 1. 先尝试从 version.properties 读取（构建时生成）
      ClassPathResource versionResource = new ClassPathResource(VERSION_PROPERTIES);
      if (versionResource.exists()) {
        try (InputStream inputStream = versionResource.getInputStream()) {
          Properties props = new Properties();
          props.load(inputStream);
          version = props.getProperty("version", DEFAULT_VERSION);
          buildTime = props.getProperty("build.time", UNKNOWN);
          gitCommit = props.getProperty("git.commit.id.abbrev", UNKNOWN);
        }
      }

      // 2. 如果 Git 提交信息未找到，尝试从 git.properties 读取（由 git-commit-id-plugin 生成）
      if (UNKNOWN.equals(gitCommit)) {
        ClassPathResource gitResource = new ClassPathResource("git.properties");
        if (gitResource.exists()) {
          try (InputStream inputStream = gitResource.getInputStream()) {
            Properties gitProps = new Properties();
            gitProps.load(inputStream);
            String commitId = gitProps.getProperty("git.commit.id.abbrev");
            if (commitId != null && !commitId.isEmpty()) {
              gitCommit = commitId;
            }
          }
        }
      }

      // 3. 如果版本信息文件不存在，使用默认值
      if (version == null) {
        version = DEFAULT_VERSION;
        buildTime = UNKNOWN;
        if (UNKNOWN.equals(gitCommit)) {
          gitCommit = UNKNOWN;
        }
        log.warn("未找到版本信息文件: {}，使用默认值", VERSION_PROPERTIES);
      }
    } catch (Exception e) {
      log.warn("加载版本信息失败，使用默认值", e);
      version = DEFAULT_VERSION;
      buildTime = UNKNOWN;
      gitCommit = UNKNOWN;
    }
  }

  /**
   * 从数据库获取系统版本号（如果数据库中有配置）.
   *
   * <p>注意：此方法需要 Spring 上下文，在静态初始化时无法使用 建议在需要时通过 SysConfigAppService 获取
   *
   * @return 版本号
   */
  public static String getVersionFromDatabase() {
    // 此方法保留用于未来扩展
    // 当前版本号主要从构建时生成的 version.properties 读取
    return null;
  }

  /**
   * 获取应用版本号.
   *
   * @return 应用版本号
   */
  public static String getVersion() {
    return version != null ? version : DEFAULT_VERSION;
  }

  /**
   * 获取构建时间.
   *
   * @return 构建时间
   */
  public static String getBuildTime() {
    return buildTime != null ? buildTime : UNKNOWN;
  }

  /**
   * 获取 Git 提交 ID.
   *
   * @return Git 提交 ID
   */
  public static String getGitCommit() {
    return gitCommit != null ? gitCommit : UNKNOWN;
  }

  /**
   * 获取完整版本信息.
   *
   * @return 完整版本信息
   */
  public static VersionInfo getVersionInfo() {
    return VersionInfo.builder()
        .version(getVersion())
        .buildTime(getBuildTime())
        .gitCommit(getGitCommit())
        .build();
  }

  /** 版本信息. */
  @lombok.Data
  @lombok.Builder
  public static class VersionInfo {
    /** 版本号. */
    private String version;

    /** 构建时间. */
    private String buildTime;

    /** Git 提交 ID. */
    private String gitCommit;
  }
}
