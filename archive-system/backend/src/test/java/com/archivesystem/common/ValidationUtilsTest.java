package com.archivesystem.common;

import com.archivesystem.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void testNotNullSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.notNull(new Object(), "Should not throw"));
    }

    @Test
    void testNotNullFail() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> ValidationUtils.notNull(null, "Object cannot be null")
        );
        assertEquals("400", exception.getCode());
    }

    @Test
    void testNotBlankSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.notBlank("test", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.notBlank("  test  ", "Should not throw"));
    }

    @Test
    void testNotBlankFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.notBlank(null, "Cannot be null"));
        assertThrows(BusinessException.class, () -> ValidationUtils.notBlank("", "Cannot be empty"));
        assertThrows(BusinessException.class, () -> ValidationUtils.notBlank("   ", "Cannot be blank"));
    }

    @Test
    void testNotEmptySuccess() {
        List<String> list = Arrays.asList("item1", "item2");
        assertDoesNotThrow(() -> ValidationUtils.notEmpty(list, "Should not throw"));
    }

    @Test
    void testNotEmptyFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.notEmpty(null, "Cannot be null"));
        assertThrows(BusinessException.class, () -> ValidationUtils.notEmpty(new ArrayList<>(), "Cannot be empty"));
    }

    @Test
    void testIsTrueSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.isTrue(true, "Should not throw"));
    }

    @Test
    void testIsTrueFail() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> ValidationUtils.isTrue(false, "Condition must be true")
        );
        assertEquals("400", exception.getCode());
    }

    @Test
    void testIsFalseSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.isFalse(false, "Should not throw"));
    }

    @Test
    void testIsFalseFail() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> ValidationUtils.isFalse(true, "Condition must be false")
        );
        assertEquals("400", exception.getCode());
    }

    @Test
    void testValidArchiveNoSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.validArchiveNo("ARC-2026-0001", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.validArchiveNo("ABC-123", "Should not throw"));
    }

    @Test
    void testValidArchiveNoFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.validArchiveNo(null, "Invalid archive number"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validArchiveNo("", "Invalid archive number"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validArchiveNo("arc-2026-0001", "Invalid format"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validArchiveNo("ARC_2026_0001", "Invalid format"));
    }

    @Test
    void testValidEmailSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.validEmail("test@example.com", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.validEmail("user.name@domain.co.uk", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.validEmail("", "Should not throw - empty is valid"));
        assertDoesNotThrow(() -> ValidationUtils.validEmail(null, "Should not throw - null is valid"));
    }

    @Test
    void testValidEmailFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.validEmail("invalid", "Invalid email"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validEmail("invalid@", "Invalid email"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validEmail("@domain.com", "Invalid email"));
    }

    @Test
    void testValidPhoneSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.validPhone("13800138000", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.validPhone("15912345678", "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.validPhone("", "Should not throw - empty is valid"));
        assertDoesNotThrow(() -> ValidationUtils.validPhone(null, "Should not throw - null is valid"));
    }

    @Test
    void testValidPhoneFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.validPhone("1234567890", "Invalid phone"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validPhone("1380013800", "Invalid phone"));
        assertThrows(BusinessException.class, () -> ValidationUtils.validPhone("23800138000", "Invalid phone"));
    }

    @Test
    void testInRangeSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.inRange(5, 1, 10, "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.inRange(1, 1, 10, "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.inRange(10, 1, 10, "Should not throw"));
    }

    @Test
    void testInRangeFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.inRange(0, 1, 10, "Out of range"));
        assertThrows(BusinessException.class, () -> ValidationUtils.inRange(11, 1, 10, "Out of range"));
    }

    @Test
    void testMaxLengthSuccess() {
        assertDoesNotThrow(() -> ValidationUtils.maxLength("short", 10, "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.maxLength(null, 10, "Should not throw"));
        assertDoesNotThrow(() -> ValidationUtils.maxLength("", 10, "Should not throw"));
    }

    @Test
    void testMaxLengthFail() {
        assertThrows(BusinessException.class, () -> ValidationUtils.maxLength("this is too long", 10, "Too long"));
    }
}
