package com.lawfirm.application.system.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * User-Agent解析工具
 */
@Slf4j
@Component
public class UserAgentParser {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?i)(mobile|android|iphone|ipod|blackberry|opera mini|opera mobi|skyfire|maemo|windows phone|palm|iemobile|symbian|symbianos|fennec)");
    private static final Pattern TABLET_PATTERN = Pattern.compile("(?i)(tablet|ipad|playbook|silk)");
    private static final Pattern CHROME_PATTERN = Pattern.compile("(?i)chrome/([0-9.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("(?i)firefox/([0-9.]+)");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("(?i)version/([0-9.]+).*safari");
    private static final Pattern EDGE_PATTERN = Pattern.compile("(?i)edg([ea])?/([0-9.]+)");
    private static final Pattern OPERA_PATTERN = Pattern.compile("(?i)opr/([0-9.]+)|opera/([0-9.]+)");
    private static final Pattern WINDOWS_PATTERN = Pattern.compile("(?i)windows (nt|phone|ce) ([0-9.]+)");
    private static final Pattern MAC_PATTERN = Pattern.compile("(?i)mac os x ([0-9_]+)");
    private static final Pattern LINUX_PATTERN = Pattern.compile("(?i)linux");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("(?i)android ([0-9.]+)");
    private static final Pattern IOS_PATTERN = Pattern.compile("(?i)(iphone|ipad|ipod) os ([0-9_]+)");

    /**
     * 解析设备类型
     */
    public String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "PC";
        }

        if (TABLET_PATTERN.matcher(userAgent).find()) {
            return "TABLET";
        }
        if (MOBILE_PATTERN.matcher(userAgent).find()) {
            return "MOBILE";
        }
        return "PC";
    }

    /**
     * 解析浏览器
     */
    public String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        if (CHROME_PATTERN.matcher(userAgent).find()) {
            return "Chrome";
        }
        if (FIREFOX_PATTERN.matcher(userAgent).find()) {
            return "Firefox";
        }
        if (EDGE_PATTERN.matcher(userAgent).find()) {
            return "Edge";
        }
        if (OPERA_PATTERN.matcher(userAgent).find()) {
            return "Opera";
        }
        if (SAFARI_PATTERN.matcher(userAgent).find()) {
            return "Safari";
        }
        return "Unknown";
    }

    /**
     * 解析操作系统
     */
    public String parseOS(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        if (WINDOWS_PATTERN.matcher(userAgent).find()) {
            return "Windows";
        }
        if (MAC_PATTERN.matcher(userAgent).find()) {
            return "macOS";
        }
        if (ANDROID_PATTERN.matcher(userAgent).find()) {
            return "Android";
        }
        if (IOS_PATTERN.matcher(userAgent).find()) {
            return "iOS";
        }
        if (LINUX_PATTERN.matcher(userAgent).find()) {
            return "Linux";
        }
        return "Unknown";
    }
}

