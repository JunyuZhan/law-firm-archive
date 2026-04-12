package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class RetentionPeriodTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        RetentionPeriod period = RetentionPeriod.builder()
                .id(1L)
                .periodCode(RetentionPeriod.PERMANENT)
                .periodName("永久")
                .periodYears(null)
                .description("永久保管")
                .sortOrder(1)
                .createdAt(now)
                .build();

        assertEquals(1L, period.getId());
        assertEquals(RetentionPeriod.PERMANENT, period.getPeriodCode());
        assertEquals("永久", period.getPeriodName());
        assertNull(period.getPeriodYears());
        assertEquals("永久保管", period.getDescription());
        assertEquals(1, period.getSortOrder());
        assertEquals(now, period.getCreatedAt());
    }

    @Test
    void testDefaultValues() {
        RetentionPeriod period = RetentionPeriod.builder().build();

        assertEquals(0, period.getSortOrder());
    }

    @Test
    void testNoArgsConstructor() {
        RetentionPeriod period = new RetentionPeriod();

        assertNull(period.getId());
        assertNull(period.getPeriodCode());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        RetentionPeriod period = new RetentionPeriod(1L, "Y30", "30年",
                30, "30年保管期限", 2, now);

        assertEquals(1L, period.getId());
        assertEquals("Y30", period.getPeriodCode());
        assertEquals("30年", period.getPeriodName());
        assertEquals(30, period.getPeriodYears());
    }

    @Test
    void testPeriodCodeConstants() {
        assertEquals("PERMANENT", RetentionPeriod.PERMANENT);
        assertEquals("Y30", RetentionPeriod.Y30);
        assertEquals("Y15", RetentionPeriod.Y15);
        assertEquals("Y10", RetentionPeriod.Y10);
        assertEquals("Y5", RetentionPeriod.Y5);
    }

    @Test
    void testSettersAndGetters() {
        RetentionPeriod period = new RetentionPeriod();

        period.setId(2L);
        period.setPeriodCode(RetentionPeriod.Y10);
        period.setPeriodName("10年");
        period.setPeriodYears(10);
        period.setDescription("10年保管期限");
        period.setSortOrder(3);

        assertEquals(2L, period.getId());
        assertEquals(RetentionPeriod.Y10, period.getPeriodCode());
        assertEquals("10年", period.getPeriodName());
        assertEquals(10, period.getPeriodYears());
        assertEquals("10年保管期限", period.getDescription());
        assertEquals(3, period.getSortOrder());
    }

    @Test
    void testPermanentPeriod() {
        RetentionPeriod period = RetentionPeriod.builder()
                .periodCode(RetentionPeriod.PERMANENT)
                .periodName("永久")
                .periodYears(null)
                .build();

        assertEquals(RetentionPeriod.PERMANENT, period.getPeriodCode());
        assertNull(period.getPeriodYears());
    }

    @Test
    void testTimedPeriod() {
        RetentionPeriod period = RetentionPeriod.builder()
                .periodCode(RetentionPeriod.Y15)
                .periodName("15年")
                .periodYears(15)
                .build();

        assertEquals(RetentionPeriod.Y15, period.getPeriodCode());
        assertEquals(15, period.getPeriodYears());
    }

    @Test
    void testEqualsAndHashCode() {
        RetentionPeriod period1 = new RetentionPeriod();
        period1.setId(1L);
        period1.setPeriodCode(RetentionPeriod.Y30);

        RetentionPeriod period2 = new RetentionPeriod();
        period2.setId(1L);
        period2.setPeriodCode(RetentionPeriod.Y30);

        assertEquals(period1, period2);
        assertEquals(period1.hashCode(), period2.hashCode());
    }

    @Test
    void testToString() {
        RetentionPeriod period = RetentionPeriod.builder()
                .id(1L)
                .periodCode("TEST")
                .build();

        String str = period.toString();
        assertNotNull(str);
        assertTrue(str.contains("RetentionPeriod"));
    }
}
