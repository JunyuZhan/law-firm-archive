package com.archivesystem.dto.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void testSettersAndGetters() {
        LoginRequest request = new LoginRequest();

        request.setUsername("admin");
        request.setPassword("password123");

        assertEquals("admin", request.getUsername());
        assertEquals("password123", request.getPassword());
    }

    @Test
    void testEqualsAndHashCode() {
        LoginRequest request1 = new LoginRequest();
        request1.setUsername("user1");
        request1.setPassword("pass1");

        LoginRequest request2 = new LoginRequest();
        request2.setUsername("user1");
        request2.setPassword("pass1");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testNotEquals() {
        LoginRequest request1 = new LoginRequest();
        request1.setUsername("user1");
        request1.setPassword("pass1");

        LoginRequest request2 = new LoginRequest();
        request2.setUsername("user2");
        request2.setPassword("pass2");

        assertNotEquals(request1, request2);
    }

    @Test
    void testToString() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("testpass");

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("LoginRequest"));
        assertTrue(str.contains("testuser"));
    }

    @Test
    void testNullValues() {
        LoginRequest request = new LoginRequest();

        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }
}
