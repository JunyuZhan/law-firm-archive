package com.archivesystem.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 客户端IP解析工具.
 * @author junyuzhan
 */
public final class ClientIpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

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

        try {
            InetAddress address = InetAddress.getByName(remoteAddr.trim());
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return true;
            }

            byte[] bytes = address.getAddress();
            if (bytes.length == 4) {
                int first = bytes[0] & 0xff;
                int second = bytes[1] & 0xff;
                return first == 100 && second >= 64 && second <= 127;
            }

            if (address instanceof Inet6Address && bytes.length == 16) {
                int first = bytes[0] & 0xff;
                int second = bytes[1] & 0xff;
                if ((first & 0xfe) == 0xfc) {
                    return true;
                }
                return first == 0xfe && (second & 0xc0) == 0x80;
            }
        } catch (UnknownHostException ignored) {
            return false;
        }

        return false;
    }
}
