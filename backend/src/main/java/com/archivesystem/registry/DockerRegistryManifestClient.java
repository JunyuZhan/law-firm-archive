package com.archivesystem.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker / OCI Registry HTTP API V2：拉取 manifest 并解析 digest。
 */
@Slf4j
@Component
public class DockerRegistryManifestClient {

    private static final String ACCEPT_MANIFEST = String.join(", ",
            "application/vnd.docker.distribution.manifest.v2+json",
            "application/vnd.docker.distribution.manifest.list.v2+json",
            "application/vnd.oci.image.manifest.v1+json",
            "application/vnd.oci.image.index.v1+json");

    private static final Pattern REALM_PATTERN = Pattern.compile("realm=\"([^\"]+)\"");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("service=\"([^\"]*)\"");

    private static final Pattern SCOPE_PATTERN = Pattern.compile("scope=\"([^\"]*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<String> fetchManifestDigest(String registryBaseUrl, String repository, String tag,
                                                  Optional<String> username, Optional<String> password) {
        if (!StringUtils.hasText(registryBaseUrl) || !StringUtils.hasText(repository) || !StringUtils.hasText(tag)) {
            return Optional.empty();
        }
        String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8);
        String manifestUrl = registryBaseUrl + "/v2/" + repository + "/manifests/" + encodedTag;
        return requestDigest(manifestUrl, username, password, repository);
    }

    private Optional<String> requestDigest(String manifestUrl, Optional<String> username, Optional<String> password,
                                           String repository) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(manifestUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", ACCEPT_MANIFEST)
                    .GET();
            applyBasicAuth(builder, username, password);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                Optional<String> bearer = resolveBearerToken(response.headers().firstValue("WWW-Authenticate").orElse(""),
                        repository, username, password);
                if (bearer.isPresent()) {
                    HttpRequest.Builder retry = HttpRequest.newBuilder()
                            .uri(URI.create(manifestUrl))
                            .timeout(Duration.ofSeconds(20))
                            .header("Accept", ACCEPT_MANIFEST)
                            .header("Authorization", "Bearer " + bearer.get())
                            .GET();
                    response = httpClient.send(retry.build(), HttpResponse.BodyHandlers.ofString());
                }
            }
            if (response.statusCode() != 200) {
                log.warn("Registry manifest 请求失败: status={}, url={}", response.statusCode(), manifestUrl);
                return Optional.empty();
            }
            Optional<String> fromHeader = response.headers().firstValue("Docker-Content-Digest");
            if (fromHeader.isPresent() && StringUtils.hasText(fromHeader.get())) {
                return fromHeader;
            }
            return sha256DigestPrefix(response.body());
        } catch (Exception e) {
            log.warn("Registry manifest 请求异常: url={}, error={}", manifestUrl, e.getMessage());
            return Optional.empty();
        }
    }

    private void applyBasicAuth(HttpRequest.Builder builder, Optional<String> username, Optional<String> password) {
        if (username.isPresent() && StringUtils.hasText(username.get())
                && password.isPresent() && StringUtils.hasText(password.get())) {
            String raw = username.get() + ":" + password.get();
            String b64 = java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + b64);
        }
    }

    private Optional<String> resolveBearerToken(String wwwAuthenticate, String repository,
                                                  Optional<String> username, Optional<String> password) {
        if (!StringUtils.hasText(wwwAuthenticate) || !wwwAuthenticate.startsWith("Bearer ")) {
            return Optional.empty();
        }
        Matcher rm = REALM_PATTERN.matcher(wwwAuthenticate);
        if (!rm.find()) {
            return Optional.empty();
        }
        String realm = rm.group(1);
        String service = "";
        Matcher sm = SERVICE_PATTERN.matcher(wwwAuthenticate);
        if (sm.find()) {
            service = sm.group(1);
        }
        String scope = "repository:" + repository + ":pull";
        Matcher scm = SCOPE_PATTERN.matcher(wwwAuthenticate);
        if (scm.find() && StringUtils.hasText(scm.group(1))) {
            scope = scm.group(1);
        }
        StringBuilder tokenUrl = new StringBuilder(realm);
        tokenUrl.append(realm.contains("?") ? "&" : "?");
        tokenUrl.append("service=").append(URLEncoder.encode(service, StandardCharsets.UTF_8));
        tokenUrl.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        try {
            HttpRequest.Builder tb = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .GET();
            applyBasicAuth(tb, username, password);
            HttpResponse<String> tr = httpClient.send(tb.build(), HttpResponse.BodyHandlers.ofString());
            if (tr.statusCode() != 200) {
                log.warn("Registry token 请求失败: status={}", tr.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(tr.body());
            if (root.hasNonNull("token")) {
                return Optional.of(root.get("token").asText());
            }
            if (root.hasNonNull("access_token")) {
                return Optional.of(root.get("access_token").asText());
            }
        } catch (Exception e) {
            log.warn("Registry token 解析失败: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> sha256DigestPrefix(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(body.getBytes(StandardCharsets.UTF_8));
            return Optional.of("sha256:" + HexFormat.of().formatHex(hash));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
