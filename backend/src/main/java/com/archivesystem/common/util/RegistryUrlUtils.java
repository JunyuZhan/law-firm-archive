package com.archivesystem.common.util;

import org.springframework.util.StringUtils;

/**
 * 将用户填写的仓库地址规范为 Registry API 根（不含 /v2/...）。
 */
public final class RegistryUrlUtils {

    private RegistryUrlUtils() {
    }

    public static String normalizeRegistryBaseUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String s = raw.trim();
        if (s.endsWith("/v2/_catalog") || s.endsWith("/v2/_catalog/")) {
            s = s.replace("/v2/_catalog/", "").replace("/v2/_catalog", "");
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith("/v2")) {
            s = s.substring(0, s.length() - 3);
            while (s.endsWith("/")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }
}
