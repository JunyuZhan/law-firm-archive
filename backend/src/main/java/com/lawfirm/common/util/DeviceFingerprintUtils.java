package com.lawfirm.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备指纹工具类
 * 
 * 用于生成设备指纹、识别设备类型、提取浏览器和操作系统信息
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public class DeviceFingerprintUtils {

    private DeviceFingerprintUtils() {
        // 工具类，禁止实例化
    }

    // 浏览器正则
    private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/([\\d.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/([\\d.]+)");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("Version/([\\d.]+).*Safari");
    private static final Pattern EDGE_PATTERN = Pattern.compile("Edg/([\\d.]+)");
    private static final Pattern IE_PATTERN = Pattern.compile("MSIE ([\\d.]+)|Trident.*rv:([\\d.]+)");

    // 操作系统正则
    private static final Pattern WINDOWS_PATTERN = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MAC_PATTERN = Pattern.compile("Mac OS X ([\\d_]+)");
    private static final Pattern LINUX_PATTERN = Pattern.compile("Linux");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android ([\\d.]+)");
    private static final Pattern IOS_PATTERN = Pattern.compile("iPhone OS ([\\d_]+)|iPad.*OS ([\\d_]+)");

    /**
     * 生成设备指纹
     * 基于User-Agent、Accept-Language、Accept-Encoding等信息生成
     */
    public static String generateFingerprint(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getHeader(request, "User-Agent"));
        sb.append("|");
        sb.append(getHeader(request, "Accept-Language"));
        sb.append("|");
        sb.append(getHeader(request, "Accept-Encoding"));
        sb.append("|");
        sb.append(getHeader(request, "Accept"));

        return md5(sb.toString());
    }

    /**
     * 解析设备信息
     */
    public static DeviceInfo parseDeviceInfo(HttpServletRequest request) {
        String userAgent = getHeader(request, "User-Agent");
        return parseDeviceInfo(userAgent);
    }

    /**
     * 解析设备信息
     */
    public static DeviceInfo parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return DeviceInfo.builder()
                    .deviceType(DeviceType.UNKNOWN)
                    .browser("Unknown")
                    .browserVersion("")
                    .os("Unknown")
                    .osVersion("")
                    .build();
        }

        return DeviceInfo.builder()
                .deviceType(detectDeviceType(userAgent))
                .browser(detectBrowser(userAgent))
                .browserVersion(detectBrowserVersion(userAgent))
                .os(detectOS(userAgent))
                .osVersion(detectOSVersion(userAgent))
                .build();
    }

    /**
     * 检测设备类型
     */
    public static DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") && !ua.contains("tablet")) {
            return DeviceType.MOBILE;
        }
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return DeviceType.TABLET;
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return DeviceType.DESKTOP;
        }

        return DeviceType.UNKNOWN;
    }

    /**
     * 检测浏览器
     */
    public static String detectBrowser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        // 顺序很重要，Edge包含Chrome，需要先判断
        if (EDGE_PATTERN.matcher(userAgent).find()) {
            return "Edge";
        }
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        }
        if (IE_PATTERN.matcher(userAgent).find()) {
            return "IE";
        }

        return "Unknown";
    }

    /**
     * 检测浏览器版本
     */
    public static String detectBrowserVersion(String userAgent) {
        if (userAgent == null) {
            return "";
        }

        Matcher matcher;

        matcher = EDGE_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = CHROME_PATTERN.matcher(userAgent);
        if (matcher.find() && !userAgent.contains("Edg")) {
            return matcher.group(1);
        }

        matcher = FIREFOX_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = SAFARI_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = IE_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }

        return "";
    }

    /**
     * 检测操作系统
     */
    public static String detectOS(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        if (ANDROID_PATTERN.matcher(userAgent).find()) {
            return "Android";
        }
        if (IOS_PATTERN.matcher(userAgent).find()) {
            return "iOS";
        }
        if (WINDOWS_PATTERN.matcher(userAgent).find()) {
            return "Windows";
        }
        if (MAC_PATTERN.matcher(userAgent).find()) {
            return "macOS";
        }
        if (LINUX_PATTERN.matcher(userAgent).find()) {
            return "Linux";
        }

        return "Unknown";
    }

    /**
     * 检测操作系统版本
     */
    public static String detectOSVersion(String userAgent) {
        if (userAgent == null) {
            return "";
        }

        Matcher matcher;

        matcher = ANDROID_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = IOS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            String version = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return version != null ? version.replace("_", ".") : "";
        }

        matcher = WINDOWS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return mapWindowsVersion(matcher.group(1));
        }

        matcher = MAC_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            return matcher.group(1).replace("_", ".");
        }

        return "";
    }

    /**
     * 映射Windows NT版本号到Windows版本名
     */
    private static String mapWindowsVersion(String ntVersion) {
        switch (ntVersion) {
            case "10.0": return "10/11";
            case "6.3": return "8.1";
            case "6.2": return "8";
            case "6.1": return "7";
            case "6.0": return "Vista";
            case "5.1": return "XP";
            default: return ntVersion;
        }
    }

    private static String getHeader(HttpServletRequest request, String name) {
        if (request == null) {
            return "";
        }
        String value = request.getHeader(name);
        return value != null ? value : "";
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            return input.hashCode() + "";
        }
    }

    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        DESKTOP("桌面端"),
        MOBILE("移动端"),
        TABLET("平板"),
        UNKNOWN("未知");

        private final String description;

        DeviceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 设备信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        private DeviceType deviceType;
        private String browser;
        private String browserVersion;
        private String os;
        private String osVersion;

        public String getFullBrowser() {
            return browser + (browserVersion.isEmpty() ? "" : " " + browserVersion);
        }

        public String getFullOS() {
            return os + (osVersion.isEmpty() ? "" : " " + osVersion);
        }
    }
}
