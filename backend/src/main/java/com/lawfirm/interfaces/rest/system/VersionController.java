package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本检查控制器
 * 提供版本检查和升级提示功能
 */
@Slf4j
@RestController
@RequestMapping("/system/version")
@RequiredArgsConstructor
@Tag(name = "版本管理", description = "版本检查和升级提示")
public class VersionController {

    private final JdbcTemplate jdbcTemplate;

    // 版本检查配置
    @Value("${app.version:1.0.0}")
    private String currentVersion;

    @Value("${app.version-check.url:}")
    private String versionCheckUrl;

    @Value("${app.version-check.github-repo:}")
    private String githubRepo;

    /**
     * 获取当前版本信息
     */
    @Operation(summary = "获取当前版本", description = "获取当前系统版本信息")
    @GetMapping
    public Result<Map<String, Object>> getVersion() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", currentVersion);
        result.put("name", "律所管理系统");
        result.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return Result.success(result);
    }

    /**
     * 检查新版本
     */
    @Operation(summary = "检查新版本", description = "检查是否有新版本可用")
    @GetMapping("/check")
    public Result<Map<String, Object>> checkVersion() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentVersion", currentVersion);
        result.put("hasUpdate", false);

        // 检查是否已忽略某个版本
        String ignoredVersion = getIgnoredVersion();

        try {
            Map<String, Object> latestInfo = fetchLatestVersion();
            if (latestInfo != null) {
                String latestVersion = (String) latestInfo.get("version");
                result.put("latestVersion", latestVersion);
                result.put("releaseNotes", latestInfo.get("releaseNotes"));
                result.put("releaseUrl", latestInfo.get("releaseUrl"));
                result.put("publishedAt", latestInfo.get("publishedAt"));

                // 比较版本号（排除已忽略的版本）
                if (latestVersion != null && compareVersions(latestVersion, currentVersion) > 0) {
                    if (ignoredVersion == null || !latestVersion.equals(ignoredVersion)) {
                        result.put("hasUpdate", true);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检查版本失败: {}", e.getMessage());
            result.put("error", "无法获取最新版本信息");
        }

        return Result.success(result);
    }

    /**
     * 获取升级指南
     */
    @Operation(summary = "获取升级指南", description = "返回系统升级的命令和步骤")
    @GetMapping("/upgrade/guide")
    public Result<Map<String, Object>> getUpgradeGuide() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentVersion", currentVersion);
        
        // 升级步骤
        List<Map<String, String>> steps = new ArrayList<>();
        
        Map<String, String> step1 = new LinkedHashMap<>();
        step1.put("title", "1. 备份数据");
        step1.put("command", "./scripts/ops/backup.sh");
        step1.put("description", "完整备份数据库和文件存储");
        steps.add(step1);
        
        Map<String, String> step2 = new LinkedHashMap<>();
        step2.put("title", "2. 拉取最新代码");
        step2.put("command", "git pull origin main");
        step2.put("description", "从 GitHub 获取最新版本");
        steps.add(step2);
        
        Map<String, String> step3 = new LinkedHashMap<>();
        step3.put("title", "3. 重新部署");
        step3.put("command", "./scripts/deploy/deploy.sh --quick --no-cache");
        step3.put("description", "重新构建并启动服务");
        steps.add(step3);
        
        result.put("steps", steps);
        
        // 一键升级命令
        String quickCmd = "./scripts/ops/backup.sh && git pull origin main "
            + "&& ./scripts/deploy/deploy.sh --quick --no-cache";
        result.put("quickCommand", quickCmd);
        
        return Result.success(result);
    }

    /**
     * 忽略版本更新
     */
    @Operation(summary = "忽略版本更新", description = "忽略指定版本的更新提示")
    @PostMapping("/ignore")
    public Result<Void> ignoreVersion(@RequestParam String version) {
        try {
            String configKey = "system.ignored-version";
            Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key = ?", Long.class, configKey);

            if (exists != null && exists > 0) {
                jdbcTemplate.update(
                    "UPDATE sys_config SET config_value = ?, update_time = CURRENT_TIMESTAMP WHERE config_key = ?",
                    version, configKey);
            } else {
                String sql = "INSERT INTO sys_config "
                    + "(config_key, config_value, config_name, config_type, remark, "
                    + "create_time, update_time) "
                    + "VALUES (?, ?, '已忽略的版本号', 'Y', '版本更新忽略配置', "
                    + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                jdbcTemplate.update(sql, configKey, version);
            }

            log.info("已忽略版本更新: {}", version);
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存忽略版本失败", e);
            return Result.error("操作失败");
        }
    }

    /**
     * 获取已忽略的版本
     */
    private String getIgnoredVersion() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT config_value FROM sys_config WHERE config_key = 'system.ignored-version'",
                String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从远程获取最新版本信息
     */
    private Map<String, Object> fetchLatestVersion() {
        // 优先使用自定义 URL
        if (versionCheckUrl != null && !versionCheckUrl.isEmpty()) {
            return fetchFromCustomUrl(versionCheckUrl);
        }

        // 使用 GitHub Release API
        if (githubRepo != null && !githubRepo.isEmpty()) {
            return fetchFromGitHub(githubRepo);
        }

        return null;
    }

    /**
     * 从自定义 URL 获取版本信息
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchFromCustomUrl(String url) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("从自定义 URL 获取版本失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 GitHub Release 获取最新版本
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchFromGitHub(String repo) {
        try {
            String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> release = restTemplate.getForObject(apiUrl, Map.class);
            if (release == null) {
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            String tagName = (String) release.get("tag_name");
            result.put("version", tagName != null ? tagName.replaceFirst("^v", "") : null);
            result.put("releaseNotes", release.get("body"));
            result.put("releaseUrl", release.get("html_url"));
            result.put("publishedAt", release.get("published_at"));

            return result;
        } catch (Exception e) {
            log.warn("从 GitHub 获取版本失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 比较两个版本号
     */
    private int compareVersions(String v1, String v2) {
        v1 = v1.replaceFirst("^v", "");
        v2 = v2.replaceFirst("^v", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    /**
     * 解析版本号部分
     */
    private int parseVersionPart(String part) {
        try {
            Pattern pattern = Pattern.compile("^(\\d+)");
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
