package com.archivesystem.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // 设置测试用的密钥和过期时间
        ReflectionTestUtils.setField(jwtUtils, "secret", "test-secret-key-for-jwt-testing-must-be-256-bits-long!");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600000L); // 1小时
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", 86400000L); // 1天
    }

    @Test
    void testGenerateAccessToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtUtils.generateRefreshToken(1L);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtils.validateToken(token));
        assertTrue(jwtUtils.isRefreshToken(token));
    }

    @Test
    void testParseToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        var claims = jwtUtils.parseToken(token);

        assertNotNull(claims);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("testuser", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("userType", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void testValidateToken_Valid() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    void testValidateToken_Null() {
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    void testValidateToken_Empty() {
        assertFalse(jwtUtils.validateToken(""));
    }

    @Test
    void testGetUserIdFromToken() {
        String token = jwtUtils.generateAccessToken(123L, "testuser", "USER");

        Long userId = jwtUtils.getUserIdFromToken(token);

        assertEquals(123L, userId);
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "johndoe", "USER");

        String username = jwtUtils.getUsernameFromToken(token);

        assertEquals("johndoe", username);
    }

    @Test
    void testGetUserTypeFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "OPERATOR");

        String userType = jwtUtils.getUserTypeFromToken(token);

        assertEquals("OPERATOR", userType);
    }

    @Test
    void testIsRefreshToken_AccessToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "USER");

        assertFalse(jwtUtils.isRefreshToken(token));
    }

    @Test
    void testIsRefreshToken_RefreshToken() {
        String token = jwtUtils.generateRefreshToken(1L);

        assertTrue(jwtUtils.isRefreshToken(token));
    }

    @Test
    void testParseToken_Expired() {
        // 创建一个过期时间很短的JwtUtils
        JwtUtils shortExpJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(shortExpJwtUtils, "secret", "test-secret-key-for-jwt-testing-must-be-256-bits-long!");
        ReflectionTestUtils.setField(shortExpJwtUtils, "expiration", 1L); // 1毫秒
        ReflectionTestUtils.setField(shortExpJwtUtils, "refreshExpiration", 1L);

        String token = shortExpJwtUtils.generateAccessToken(1L, "testuser", "USER");

        // 等待令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThrows(ExpiredJwtException.class, () -> shortExpJwtUtils.parseToken(token));
    }

    @Test
    void testParseToken_Malformed() {
        assertThrows(MalformedJwtException.class, () -> jwtUtils.parseToken("not.a.valid.jwt.token"));
    }

    @Test
    void testParseToken_WrongSignature() {
        // 使用不同的密钥生成的token
        JwtUtils otherJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(otherJwtUtils, "secret", "different-secret-key-for-testing-must-be-long-enough!");
        ReflectionTestUtils.setField(otherJwtUtils, "expiration", 3600000L);
        ReflectionTestUtils.setField(otherJwtUtils, "refreshExpiration", 86400000L);

        String tokenFromOther = otherJwtUtils.generateAccessToken(1L, "testuser", "USER");

        // 尝试用当前密钥解析应该失败
        assertFalse(jwtUtils.validateToken(tokenFromOther));
    }

    @Test
    void testGenerateAccessToken_MultipleTokens() {
        String token1 = jwtUtils.generateAccessToken(1L, "user1", "USER");
        String token2 = jwtUtils.generateAccessToken(2L, "user2", "ADMIN");

        assertNotEquals(token1, token2);
        assertEquals("user1", jwtUtils.getUsernameFromToken(token1));
        assertEquals("user2", jwtUtils.getUsernameFromToken(token2));
    }
}
