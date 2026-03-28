package com.archivesystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlInjectionValidatorTest {

    private final SqlInjectionValidator validator = new SqlInjectionValidator();

    @Test
    void testSafeInput_ShouldReturnTrue() {
        assertTrue(validator.isSafe("正常的中文内容"));
        assertTrue(validator.isSafe("Normal English Content"));
        assertTrue(validator.isSafe("档案编号-2024-001"));
        assertTrue(validator.isSafe("user@example.com"));
        assertTrue(validator.isSafe("123456"));
    }

    @Test
    void testNullInput_ShouldReturnTrue() {
        assertTrue(validator.isSafe(null));
    }

    @Test
    void testEmptyInput_ShouldReturnTrue() {
        assertTrue(validator.isSafe(""));
    }

    @Test
    void testUnionInjection_ShouldReturnFalse() {
        assertFalse(validator.isSafe("' UNION SELECT * FROM users"));
        assertFalse(validator.isSafe("1 UNION ALL SELECT password FROM users"));
    }

    @Test
    void testCommentPatterns_ShouldReturnFalse() {
        assertFalse(validator.isSafe("admin' --comment"));
        assertFalse(validator.isSafe("admin'/* comment */"));
    }

    @Test
    void testSleepInjection_ShouldReturnFalse() {
        assertFalse(validator.isSafe("1; sleep(10)"));
        assertFalse(validator.isSafe("'; WAITFOR DELAY '0:0:10'"));
    }

    @Test
    void testStackedQueries_ShouldReturnFalse() {
        assertFalse(validator.isSafe("; DROP TABLE users"));
        assertFalse(validator.isSafe("; DELETE FROM archives"));
    }

    @Test
    void testDropTable_ShouldReturnFalse() {
        assertFalse(validator.isSafe("DROP TABLE archives"));
        assertFalse(validator.isSafe("TRUNCATE TABLE users"));
    }

    @Test
    void testLongInput_ShouldBeValidated() {
        String longInput = "这是一段很长的正常内容".repeat(100);
        assertTrue(validator.isSafe(longInput));
    }

    @Test
    void testSanitize_ShouldRemoveDangerousPatterns() {
        String input = "test'; DROP TABLE users; --comment";
        String sanitized = validator.sanitize(input);
        
        assertNotNull(sanitized);
        assertFalse(sanitized.contains("--"));
        assertFalse(sanitized.contains(";"));
    }

    @Test
    void testIsValidOrderByField_ValidFields() {
        assertTrue(validator.isValidOrderByField("created_at"));
        assertTrue(validator.isValidOrderByField("archive.title"));
        assertTrue(validator.isValidOrderByField("id"));
    }

    @Test
    void testIsValidOrderByField_InvalidFields() {
        assertFalse(validator.isValidOrderByField(null));
        assertFalse(validator.isValidOrderByField(""));
        assertFalse(validator.isValidOrderByField("name; DROP TABLE"));
        assertFalse(validator.isValidOrderByField("field'"));
    }

    @Test
    void testIsValidOrderDirection() {
        assertTrue(validator.isValidOrderDirection("ASC"));
        assertTrue(validator.isValidOrderDirection("DESC"));
        assertTrue(validator.isValidOrderDirection("asc"));
        assertTrue(validator.isValidOrderDirection("desc"));
        assertTrue(validator.isValidOrderDirection(null));
        assertTrue(validator.isValidOrderDirection(""));
        
        assertFalse(validator.isValidOrderDirection("ASCENDING"));
        assertFalse(validator.isValidOrderDirection("DROP TABLE"));
    }
}
