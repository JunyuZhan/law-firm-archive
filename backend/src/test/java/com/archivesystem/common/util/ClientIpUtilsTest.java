package com.archivesystem.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * @author junyuzhan
 */

class ClientIpUtilsTest {

    @Test
    void testResolve_UsesForwardedHeaderWhenRemoteAddrIsTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 192.168.1.10");

        assertEquals("203.0.113.5", ClientIpUtils.resolve(request));
    }

    @Test
    void testResolve_IgnoresForwardedHeaderWhenRemoteAddrIsPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertEquals("8.8.8.8", ClientIpUtils.resolve(request));
    }

    @Test
    void testResolve_FallsBackToRemoteAddrWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertEquals("127.0.0.1", ClientIpUtils.resolve(request));
    }
}
