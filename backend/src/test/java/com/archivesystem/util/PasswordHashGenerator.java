package com.archivesystem.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/**
 * @author junyuzhan
 */

class PasswordHashGenerator {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("==================================");
        System.out.println("BCrypt hash for 'admin123': " + hash);
        System.out.println("Verify: " + encoder.matches("admin123", hash));
        System.out.println("==================================");
    }
}
