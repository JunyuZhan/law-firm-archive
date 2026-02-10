package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
     * 执行一键升级
     */
    @Operation(summary = "执行一键升级", description = "在服务器上执行升级脚本")
    @PostMapping("/upgrade/execute")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('system:config:edit')")
    public Result<Map<String, Object>> executeUpgrade(
            @RequestParam(defaultValue = "true") boolean backup) {
        Map<String, Object> result = new LinkedHashMap<>();
        String upgradeId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            // 检查升级脚本是否存在
            String workDir = System.getProperty("user.dir");
            // Docker 容器内的工作目录是 /app，需要找到项目根目录
            Path projectRoot = findProjectRoot(workDir);
            if (projectRoot == null) {
                return Result.error("无法找到项目根目录，请确保在正确的目录下运行");
            }
            
            Path upgradeScript = projectRoot.resolve("scripts/ops/upgrade.sh");
            if (!Files.exists(upgradeScript)) {
                return Result.error("升级脚本不存在: " + upgradeScript);
            }
            
            // 创建升级状态文件（写到 /tmp 目录，避免只读文件系统问题）
            Path statusFile = Paths.get("/tmp/.upgrade-status-" + upgradeId + ".json");
            writeUpgradeStatus(statusFile, "started", "升级已启动", 0);
            
            result.put("upgradeId", upgradeId);
            result.put("status", "started");
            result.put("message", "升级任务已启动，请等待...");
            
            // 异步执行升级
            String projectRootStr = projectRoot.toString();
            CompletableFuture.runAsync(() -> {
                executeUpgradeScript(projectRootStr, upgradeId, backup);
            });
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("启动升级失败", e);
            return Result.error("启动升级失败，请查看服务器日志了解详情");
        }
    }

    /**
     * 查询升级状态
     */
    @Operation(summary = "查询升级状态", description = "查询升级任务的执行状态")
    @GetMapping("/upgrade/status")
    public Result<Map<String, Object>> getUpgradeStatus(@RequestParam String upgradeId) {
        // 安全检查：防止路径遍历攻击
        if (upgradeId == null || upgradeId.isBlank()) {
            return Result.error("upgradeId 不能为空");
        }
        // upgradeId 应该只包含字母数字和短横线（UUID 格式）
        if (!upgradeId.matches("^[a-zA-Z0-9\\-]+$")) {
            log.warn("检测到非法 upgradeId: {}", upgradeId);
            return Result.error("非法的 upgradeId 格式");
        }
        
        try {
            // 状态文件在 /tmp 目录
            Path statusFile = Paths.get("/tmp/.upgrade-status-" + upgradeId + ".json");
            if (!Files.exists(statusFile)) {
                // 状态文件不存在，可能升级已完成并清理
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "completed");
                result.put("message", "升级已完成");
                result.put("progress", 100);
                return Result.success(result);
            }
            
            String content = Files.readString(statusFile, StandardCharsets.UTF_8);
            // 简单解析 JSON
            Map<String, Object> result = parseSimpleJson(content);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询升级状态失败", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "unknown");
            result.put("message", "无法获取升级状态");
            return Result.success(result);
        }
    }

    /**
     * 查找项目根目录
     */
    private Path findProjectRoot(String startDir) {
        Path path = Paths.get(startDir);
        
        // 如果在 Docker 容器内（/app），尝试找到挂载的项目目录
        if (startDir.equals("/app")) {
            // 检查常见的挂载点
            Path[] possibleRoots = {
                Paths.get("/opt/law-firm"),
                Paths.get("/app"),
                path
            };
            for (Path root : possibleRoots) {
                if (Files.exists(root.resolve("scripts/ops/upgrade.sh"))) {
                    return root;
                }
            }
        }
        
        // 向上查找包含 scripts/ops/upgrade.sh 的目录
        while (path != null) {
            if (Files.exists(path.resolve("scripts/ops/upgrade.sh"))) {
                return path;
            }
            path = path.getParent();
        }
        
        return null;
    }

    /**
     * 获取当前Git分支
     */
    private String getCurrentBranch(String projectRoot) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pb.directory(new File(projectRoot));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String branch = reader.readLine();
                if (branch != null && !branch.trim().isEmpty()) {
                    return branch.trim();
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            log.warn("获取当前分支失败，使用默认分支main: {}", e.getMessage());
        }
        return "main"; // 默认分支
    }

    /**
     * 执行升级（调用upgrade.sh脚本）
     */
    private void executeUpgradeScript(String projectRoot, String upgradeId, boolean backup) {
        Path statusFile = Paths.get("/tmp/.upgrade-status-" + upgradeId + ".json");
        
        try {
            // 获取当前分支
            String currentBranch = getCurrentBranch(projectRoot);
            log.info("当前分支: {}", currentBranch);
            
            // 构建升级脚本命令
            Path upgradeScript = Paths.get(projectRoot, "scripts/ops/upgrade.sh");
            if (!Files.exists(upgradeScript)) {
                writeUpgradeStatus(statusFile, "failed", "升级脚本不存在: " + upgradeScript, -1);
                return;
            }
            
            // 确保脚本有执行权限
            upgradeScript.toFile().setExecutable(true);
            
            // 构建命令参数
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add(upgradeScript.toString());
            
            if (backup) {
                command.add("--quick"); // 快速模式，自动备份
            } else {
                command.add("--no-backup"); // 跳过备份
            }
            
            writeUpgradeStatus(statusFile, "running", "正在执行升级脚本...", 10);
            
            // 执行升级脚本
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(projectRoot));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 读取输出并更新进度
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[升级脚本] {}", line);
                    
                    // 根据输出内容更新进度（简单匹配）
                    if (line.contains("拉取最新代码") || line.contains("pull")) {
                        writeUpgradeStatus(statusFile, "running", "正在拉取最新代码...", 30);
                    } else if (line.contains("构建") || line.contains("build")) {
                        writeUpgradeStatus(statusFile, "running", "正在构建镜像...", 60);
                    } else if (line.contains("部署") || line.contains("deploy")) {
                        writeUpgradeStatus(statusFile, "running", "正在部署服务...", 80);
                    } else if (line.contains("完成") || line.contains("success")) {
                        writeUpgradeStatus(statusFile, "running", "升级进行中...", 90);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                writeUpgradeStatus(statusFile, "completed", "升级成功！服务正在重启...", 100);
                log.info("升级脚本执行成功，服务即将重启");
            } else {
                writeUpgradeStatus(statusFile, "failed", "升级脚本执行失败，退出码: " + exitCode, -1);
                log.error("升级脚本执行失败，退出码: {}", exitCode);
            }
            
        } catch (Exception e) {
            log.error("升级执行异常", e);
            try {
                writeUpgradeStatus(statusFile, "failed", "升级执行异常，请查看服务器日志了解详情", -1);
            } catch (Exception writeEx) {
                log.warn("写入升级状态文件失败: {}", writeEx.getMessage());
            }
        }
    }

    /**
     * 写入升级状态
     */
    private void writeUpgradeStatus(Path statusFile, String status, String message, int progress) 
            throws Exception {
        String json = String.format(
            "{\"status\":\"%s\",\"message\":\"%s\",\"progress\":%d,\"timestamp\":\"%s\"}",
            status, message, progress, 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Files.writeString(statusFile, json, StandardCharsets.UTF_8);
    }

    /**
     * 简单解析 JSON
     */
    private Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 提取 status
        java.util.regex.Matcher m = Pattern.compile("\"status\":\"([^\"]+)\"").matcher(json);
        if (m.find()) result.put("status", m.group(1));
        // 提取 message
        m = Pattern.compile("\"message\":\"([^\"]+)\"").matcher(json);
        if (m.find()) result.put("message", m.group(1));
        // 提取 progress
        m = Pattern.compile("\"progress\":(\\d+)").matcher(json);
        if (m.find()) result.put("progress", Integer.parseInt(m.group(1)));
        // 提取 timestamp
        m = Pattern.compile("\"timestamp\":\"([^\"]+)\"").matcher(json);
        if (m.find()) result.put("timestamp", m.group(1));
        return result;
    }

    /**
     * 忽略版本更新
     */
    @Operation(summary = "忽略版本更新", description = "忽略指定版本的更新提示")
    @PostMapping("/ignore")
    @PreAuthorize("hasAuthority('sys:config:edit')")
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
            log.debug("获取已忽略版本配置失败: {}", e.getMessage());
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
