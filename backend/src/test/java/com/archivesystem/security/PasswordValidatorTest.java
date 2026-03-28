package com.archivesystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
    }

    @Test
    void testValidate_NullPassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate(null);

        assertFalse(result.isValid());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("不能为空"));
        assertEquals(0, result.score());
    }

    @Test
    void testValidate_EmptyPassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("");

        assertFalse(result.isValid());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("不能为空"));
    }

    @Test
    void testValidate_TooShort() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abc123");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("长度至少")));
    }

    @Test
    void testValidate_TooLong() {
        String longPassword = "a".repeat(51) + "1";
        PasswordValidator.ValidationResult result = passwordValidator.validate(longPassword);

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("不能超过")));
    }

    @Test
    void testValidate_NoLetters() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("12345678");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("包含字母")));
    }

    @Test
    void testValidate_NoNumbers() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abcdefgh");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("包含数字")));
    }

    @Test
    void testValidate_ValidSimplePassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abcd1234");

        assertTrue(result.isValid());
        assertEquals(0, result.errors().size());
        assertTrue(result.score() >= 2);
    }

    @Test
    void testValidate_ValidStrongPassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("Abcd1234!@#");

        assertTrue(result.isValid());
        assertEquals(0, result.errors().size());
        assertTrue(result.score() >= 4);
    }

    @Test
    void testValidate_CommonWeakPassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("password");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("过于简单")));
    }

    @Test
    void testValidate_CommonWeakPasswordCaseInsensitive() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("PASSWORD");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("过于简单")));
    }

    @Test
    void testValidate_SequentialChars() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abc12345");

        assertTrue(result.isValid()); // Still valid but lower score
        assertTrue(result.score() < 5); // Score reduced due to sequential chars
    }

    @Test
    void testValidate_RepeatedChars() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("aaa12345");

        assertTrue(result.isValid()); // Still valid but lower score
        assertTrue(result.score() < 5); // Score reduced due to repeated chars
    }

    @Test
    void testValidate_WithSpecialChars() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("Abcd1234!@");

        assertTrue(result.isValid());
        assertTrue(result.score() >= 4);
    }

    @Test
    void testValidate_WithUpperAndLowerCase() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("AbCd1234");

        assertTrue(result.isValid());
        assertTrue(result.score() >= 3);
    }

    @Test
    void testValidate_LongPassword() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("Abcd1234!@#$");

        assertTrue(result.isValid());
        assertTrue(result.score() >= 5);
    }

    @Test
    void testValidateStrict_WithoutSpecialChars() {
        PasswordValidator.ValidationResult result = passwordValidator.validateStrict("Abcd1234");

        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("特殊字符")));
    }

    @Test
    void testValidateStrict_WithSpecialChars() {
        PasswordValidator.ValidationResult result = passwordValidator.validateStrict("Abcd1234!");

        assertTrue(result.isValid());
    }

    @Test
    void testValidationResult_GetFirstError() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abc");

        assertNotNull(result.getFirstError());
        assertTrue(result.getFirstError().contains("长度"));
    }

    @Test
    void testValidationResult_GetFirstError_NoErrors() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("Abcd1234!");

        assertNull(result.getFirstError());
    }

    @Test
    void testValidationResult_GetAllErrors() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("abc");

        String allErrors = result.getAllErrors();
        assertNotNull(allErrors);
        assertTrue(allErrors.contains("长度"));
    }

    @Test
    void testValidate_MultipleWeaknesses() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("123");

        assertFalse(result.isValid());
        assertTrue(result.errors().size() >= 2);
    }

    @Test
    void testValidate_SequentialDecreasing() {
        PasswordValidator.ValidationResult result = passwordValidator.validate("cba98765");

        assertTrue(result.isValid());
        assertTrue(result.score() < 5); // Score reduced
    }

    @Test
    void testValidate_AllCommonWeakPasswords() {
        String[] weakPasswords = {"password", "123456", "qwerty", "admin", "letmein"};
        
        for (String weak : weakPasswords) {
            PasswordValidator.ValidationResult result = passwordValidator.validate(weak);
            assertFalse(result.isValid(), "Password '" + weak + "' should be invalid");
        }
    }

    @Test
    void testValidate_BoundaryLength() {
        // Exactly 8 characters
        PasswordValidator.ValidationResult result1 = passwordValidator.validate("Abcd1234");
        assertTrue(result1.isValid());

        // Exactly 50 characters
        String password50 = "Abcd1234" + "x".repeat(42);
        PasswordValidator.ValidationResult result2 = passwordValidator.validate(password50);
        assertTrue(result2.isValid());
    }

    @Test
    void testValidate_SpecialCharacterVariety() {
        String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "_", "=", "+"};
        
        for (String special : specialChars) {
            String password = "Abcd1234" + special;
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);
            assertTrue(result.isValid(), "Password with '" + special + "' should be valid");
            assertTrue(result.score() >= 4, "Password with '" + special + "' should have score >= 4");
        }
    }
}
