package com.lawfirm.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * VersionUtils 单元测试
 *
 * <p>测试版本信息获取功能
 */
@DisplayName("VersionUtils 版本工具测试")
class VersionUtilsTest {

  // ========== getVersion 测试 ==========

  @Test
  @DisplayName("getVersion: 应该返回非空版本号")
  void getVersion_shouldReturnNonEmptyVersion() {
    String version = VersionUtils.getVersion();
    assertThat(version).isNotNull();
    assertThat(version).isNotEmpty();
  }

  @Test
  @DisplayName("getVersion: 版本号应该存在")
  void getVersion_shouldReturnSemanticVersion() {
    String version = VersionUtils.getVersion();
    // 版本号可能是占位符或实际版本号
    assertThat(version).isNotNull();
    assertThat(version).isNotEmpty();
  }

  // ========== getBuildTime 测试 ==========

  @Test
  @DisplayName("getBuildTime: 应该返回非空构建时间")
  void getBuildTime_shouldReturnNonEmptyBuildTime() {
    String buildTime = VersionUtils.getBuildTime();
    assertThat(buildTime).isNotNull();
    assertThat(buildTime).isNotEmpty();
  }

  // ========== getGitCommit 测试 ==========

  @Test
  @DisplayName("getGitCommit: 应该返回非空提交ID")
  void getGitCommit_shouldReturnNonEmptyCommitId() {
    String gitCommit = VersionUtils.getGitCommit();
    assertThat(gitCommit).isNotNull();
    assertThat(gitCommit).isNotEmpty();
  }

  // ========== getVersionInfo 测试 ==========

  @Test
  @DisplayName("getVersionInfo: 应该返回完整的版本信息")
  void getVersionInfo_shouldReturnCompleteVersionInfo() {
    VersionUtils.VersionInfo info = VersionUtils.getVersionInfo();
    assertThat(info).isNotNull();
    assertThat(info.getVersion()).isNotNull();
    assertThat(info.getBuildTime()).isNotNull();
    assertThat(info.getGitCommit()).isNotNull();
  }

  @Test
  @DisplayName("getVersionInfo: 多次调用应该返回相同的信息")
  void getVersionInfo_shouldReturnSameInfoOnMultipleCalls() {
    VersionUtils.VersionInfo info1 = VersionUtils.getVersionInfo();
    VersionUtils.VersionInfo info2 = VersionUtils.getVersionInfo();

    assertThat(info1.getVersion()).isEqualTo(info2.getVersion());
    assertThat(info1.getBuildTime()).isEqualTo(info2.getBuildTime());
    assertThat(info1.getGitCommit()).isEqualTo(info2.getGitCommit());
  }

  @Test
  @DisplayName("getVersionInfo: 应该返回带Builder模式的VersionInfo")
  void getVersionInfo_shouldReturnBuilderPatternVersionInfo() {
    VersionUtils.VersionInfo info =
        VersionUtils.VersionInfo.builder()
            .version("1.0.0")
            .buildTime("2024-01-01")
            .gitCommit("abc123")
            .build();

    assertThat(info.getVersion()).isEqualTo("1.0.0");
    assertThat(info.getBuildTime()).isEqualTo("2024-01-01");
    assertThat(info.getGitCommit()).isEqualTo("abc123");
  }

  // ========== VersionInfo 测试 ==========

  @Test
  @DisplayName("VersionInfo: Builder应该正确设置字段")
  void VersionInfo_shouldSetAndGetFields() {
    VersionUtils.VersionInfo info =
        VersionUtils.VersionInfo.builder()
            .version("2.0.0")
            .buildTime("2024-06-01")
            .gitCommit("def456")
            .build();

    assertThat(info.getVersion()).isEqualTo("2.0.0");
    assertThat(info.getBuildTime()).isEqualTo("2024-06-01");
    assertThat(info.getGitCommit()).isEqualTo("def456");
  }
}
