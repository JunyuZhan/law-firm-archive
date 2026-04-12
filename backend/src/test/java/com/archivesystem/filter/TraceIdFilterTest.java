package com.archivesystem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    private TraceIdFilter traceIdFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        traceIdFilter = new TraceIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @Test
    void testDoFilterInternal_GeneratesTraceId() throws ServletException, IOException {
        request.setRequestURI("/api/test");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        String responseTraceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(responseTraceId);
        assertTrue(responseTraceId.contains("-"));
    }

    @Test
    void testDoFilterInternal_UsesExistingTraceId() throws ServletException, IOException {
        String existingTraceId = "existing-trace-id-12345";
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, existingTraceId);
        request.setRequestURI("/api/test");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(existingTraceId, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    }

    @Test
    void testDoFilterInternal_EmptyTraceIdHeader() throws ServletException, IOException {
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "");
        request.setRequestURI("/api/test");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        String responseTraceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(responseTraceId);
        assertNotEquals("", responseTraceId);
    }

    @Test
    void testDoFilterInternal_ClearsMDCAfterFilter() throws ServletException, IOException {
        request.setRequestURI("/api/test");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        // After filter completes, MDC should be cleared
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
        assertNull(MDC.get(TraceIdFilter.REQUEST_URI_KEY));
        assertNull(MDC.get(TraceIdFilter.USER_ID_KEY));
    }

    @Test
    void testDoFilterInternal_ClearsMDCOnException() throws ServletException, IOException {
        request.setRequestURI("/api/test");

        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> 
            traceIdFilter.doFilterInternal(request, response, filterChain)
        );

        // MDC should still be cleared even after exception
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY));
        assertNull(MDC.get(TraceIdFilter.REQUEST_URI_KEY));
    }

    @Test
    void testSetUserId() {
        TraceIdFilter.setUserId(123L);

        assertEquals("123", MDC.get(TraceIdFilter.USER_ID_KEY));

        MDC.clear();
    }

    @Test
    void testSetUserId_Null() {
        TraceIdFilter.setUserId(null);

        assertNull(MDC.get(TraceIdFilter.USER_ID_KEY));
    }

    @Test
    void testTraceIdFormat() throws ServletException, IOException {
        request.setRequestURI("/api/test");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(traceId);
        // 格式应该是: timestamp-uuid8chars
        assertTrue(traceId.matches("\\d+-[a-f0-9]{8}"));
    }

    @Test
    void testConstants() {
        assertEquals("X-Trace-Id", TraceIdFilter.TRACE_ID_HEADER);
        assertEquals("traceId", TraceIdFilter.TRACE_ID_KEY);
        assertEquals("requestUri", TraceIdFilter.REQUEST_URI_KEY);
        assertEquals("userId", TraceIdFilter.USER_ID_KEY);
    }
}
