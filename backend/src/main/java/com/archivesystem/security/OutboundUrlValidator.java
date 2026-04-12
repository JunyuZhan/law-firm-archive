package com.archivesystem.security;

import com.archivesystem.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * 出站URL安全校验器.
 * @author junyuzhan
 */
@Component
public class OutboundUrlValidator {

    public void validate(String rawUrl, String fieldName) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new BusinessException("400", fieldName + "不能为空");
        }

        URI uri = parseUri(rawUrl, fieldName);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new BusinessException("400", fieldName + "仅支持 HTTP/HTTPS");
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new BusinessException("400", fieldName + "缺少有效主机名");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new BusinessException("400", fieldName + "指向受限地址，不允许访问");
                }
            }
        } catch (UnknownHostException e) {
            throw new BusinessException("400", fieldName + "主机解析失败");
        }
    }

    private URI parseUri(String rawUrl, String fieldName) {
        try {
            return new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw new BusinessException("400", fieldName + "格式不正确");
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
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

        return false;
    }
}
