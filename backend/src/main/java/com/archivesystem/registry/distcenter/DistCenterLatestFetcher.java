package com.archivesystem.registry.distcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 拉取 Dist Center 静态站上的 {@code versions/latest.json}。
 */
@Slf4j
@Component
public class DistCenterLatestFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public DistCenterLatestOutcome fetchDescriptor(String url) {
        if (!StringUtils.hasText(url)) {
            return DistCenterLatestOutcome.notConfigured();
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return DistCenterLatestOutcome.fail("分发中心地址必须以 http:// 或 https:// 开头");
        }
        try {
            URI.create(trimmed);
        } catch (Exception e) {
            return DistCenterLatestOutcome.fail("分发中心地址无效：" + e.getMessage());
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimmed))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return DistCenterLatestOutcome.fail("HTTP " + response.statusCode() + "：无法读取分发中心版本描述");
            }
            return parseBody(response.body());
        } catch (Exception e) {
            log.warn("Dist Center latest.json 请求失败: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.length() > 160) {
                msg = msg.substring(0, 160) + "…";
            }
            return DistCenterLatestOutcome.fail("拉取分发中心版本描述失败：" + msg);
        }
    }

    private static DistCenterLatestOutcome parseBody(String body) {
        if (!StringUtils.hasText(body)) {
            return DistCenterLatestOutcome.fail("分发中心返回空内容");
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            if (!root.isObject()) {
                return DistCenterLatestOutcome.fail("分发中心 JSON 格式异常");
            }
            String snapshotVersion = textOrEmpty(root, "version");
            String channelAppVersion = textOrEmpty(root, "app_version");
            JsonNode env = root.get("env");
            String fromEnv = "";
            if (env != null && env.isObject() && env.has("APP_VERSION")) {
                fromEnv = textOrEmpty(env, "APP_VERSION");
            }
            String publishedImageTag = StringUtils.hasText(fromEnv) ? fromEnv : channelAppVersion;
            DistCenterDescriptor descriptor = new DistCenterDescriptor(
                    publishedImageTag,
                    snapshotVersion,
                    channelAppVersion
            );
            return DistCenterLatestOutcome.ok(descriptor);
        } catch (Exception e) {
            return DistCenterLatestOutcome.fail("解析分发中心 JSON 失败：" + e.getMessage());
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            return "";
        }
        return v.asText("").trim();
    }
}
