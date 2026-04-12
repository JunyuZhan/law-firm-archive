package com.archivesystem.security;

import com.archivesystem.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

class MaintenanceModeFilterTest {

    private ConfigService configService;
    private MaintenanceModeFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        filter = new MaintenanceModeFilter(configService, new ObjectMapper());
        filterChain = mock(FilterChain.class);
    }

    @Test
    void shouldAllowWhitelistedRestoreWriteRequestWhenContextPathPresent() throws ServletException, IOException {
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/restores/maintenance");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBlockNonWhitelistedWriteRequestDuringMaintenanceMode() throws ServletException, IOException {
        when(configService.getBooleanValue("system.runtime.maintenance.enabled", false)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sources/1/test");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("系统当前处于维护模式");
    }
}
