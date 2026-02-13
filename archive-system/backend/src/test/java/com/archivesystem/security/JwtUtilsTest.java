package com.archivesystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "test-secret-key-for-archive-system-unit-test-2026");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", 86400000L);
    }

    @Test
    void testGenerateAccessToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtUtils.generateRefreshToken(1L);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testParseToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        var claims = jwtUtils.parseToken(token);
        
        assertNotNull(claims);
        assertEquals(1, claims.get("userId"));
        assertEquals("testuser", claims.get("username"));
        assertEquals("ADMIN", claims.get("userType"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void testValidateTokenSuccess() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void testValidateTokenFail() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    void testGetUserIdFromToken() {
        String token = jwtUtils.generateAccessToken(123L, "testuser", "USER");
        
        Long userId = jwtUtils.getUserIdFromToken(token);
        
        assertEquals(123L, userId);
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "admin", "ADMIN");
        
        String username = jwtUtils.getUsernameFromToken(token);
        
        assertEquals("admin", username);
    }

    @Test
    void testGetUserTypeFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ARCHIVE_ADMIN");
        
        String userType = jwtUtils.getUserTypeFromToken(token);
        
        assertEquals("ARCHIVE_ADMIN", userType);
    }

    @Test
    void testIsRefreshToken() {
        String accessToken = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        String refreshToken = jwtUtils.generateRefreshToken(1L);
        
        assertFalse(jwtUtils.isRefreshToken(accessToken));
        assertTrue(jwtUtils.isRefreshToken(refreshToken));
    }

    @Test
    void testTokenContainsExpectedClaims() {
        String token = jwtUtils.generateAccessToken(999L, "specialuser", "SUPER_ADMIN");
        
        var claims = jwtUtils.parseToken(token);
        
        assertEquals(999, claims.get("userId"));
        assertEquals("specialuser", claims.get("username"));
        assertEquals("SUPER_ADMIN", claims.get("userType"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    void testDifferentTokensForSameUser() {
        String token1 = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        String token2 = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        assertNotEquals(token1, token2);
    }

    @Test
    void testTokenExpiration() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1000L);
        
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        assertTrue(jwtUtils.validateToken(token));
    }
}
