package com.archivesystem.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端IP解析工具.
 * @author junyuzhan
 */
public final class ClientIpUtils {

    private static final List<String> DEFAULT_TRUSTED_PROXIES = List.of("127.0.0.1", "::1");

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static volatile List<IpAddressMatcher> trustedProxyMatchers = buildMatchers(DEFAULT_TRUSTED_PROXIES);

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            for (String header : IP_HEADERS) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                    int index = ip.indexOf(',');
                    return index >= 0 ? ip.substring(0, index).trim() : ip.trim();
                }
            }
        }

        return remoteAddr != null ? remoteAddr : "";
    }

    public static void configureTrustedProxies(List<String> trustedProxies) {
        List<String> proxyList = trustedProxies == null || trustedProxies.isEmpty()
                ? DEFAULT_TRUSTED_PROXIES
                : trustedProxies;
        trustedProxyMatchers = buildMatchers(proxyList);
    }

    public static String resolveCurrentRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        return resolve(attributes.getRequest());
    }

    private static boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        String candidate = remoteAddr.trim();
        for (IpAddressMatcher matcher : trustedProxyMatchers) {
            if (matcher.matches(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static List<IpAddressMatcher> buildMatchers(List<String> trustedProxies) {
        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String proxy : trustedProxies) {
            if (proxy == null || proxy.isBlank()) {
                continue;
            }
            matchers.add(new IpAddressMatcher(proxy.trim()));
        }
        return List.copyOf(matchers);
    }
}
