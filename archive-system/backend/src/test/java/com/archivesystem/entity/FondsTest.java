package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FondsTest {

    @Test
    void testBuilder() {
        Fonds fonds = Fonds.builder()
                .fondsNo("FD-001")
                .fondsName("测试全宗")
                .fondsType(Fonds.TYPE_INTERNAL)
                .description("测试全宗描述")
                .contactPerson("张三")
                .contactPhone("13800138000")
                .status(Fonds.STATUS_ACTIVE)
                .build();

        assertEquals("FD-001", fonds.getFondsNo());
        assertEquals("测试全宗", fonds.getFondsName());
        assertEquals(Fonds.TYPE_INTERNAL, fonds.getFondsType());
        assertEquals("测试全宗描述", fonds.getDescription());
        assertEquals("张三", fonds.getContactPerson());
        assertEquals("13800138000", fonds.getContactPhone());
        assertEquals(Fonds.STATUS_ACTIVE, fonds.getStatus());
    }

    @Test
    void testDefaultValues() {
        Fonds fonds = Fonds.builder().build();

        assertEquals(Fonds.STATUS_ACTIVE, fonds.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        Fonds fonds = new Fonds();

        assertNull(fonds.getFondsNo());
        assertNull(fonds.getFondsName());
    }

    @Test
    void testAllArgsConstructor() {
        Fonds fonds = new Fonds("FD-002", "外部全宗", Fonds.TYPE_EXTERNAL,
                "外部全宗描述", "李四", "13900139000", Fonds.STATUS_INACTIVE);

        assertEquals("FD-002", fonds.getFondsNo());
        assertEquals("外部全宗", fonds.getFondsName());
        assertEquals(Fonds.TYPE_EXTERNAL, fonds.getFondsType());
    }

    @Test
    void testStatusConstants() {
        assertEquals("ACTIVE", Fonds.STATUS_ACTIVE);
        assertEquals("INACTIVE", Fonds.STATUS_INACTIVE);
    }

    @Test
    void testTypeConstants() {
        assertEquals("INTERNAL", Fonds.TYPE_INTERNAL);
        assertEquals("EXTERNAL", Fonds.TYPE_EXTERNAL);
    }

    @Test
    void testSettersAndGetters() {
        Fonds fonds = new Fonds();

        fonds.setFondsNo("FD-003");
        fonds.setFondsName("新全宗");
        fonds.setFondsType(Fonds.TYPE_EXTERNAL);
        fonds.setDescription("新全宗描述");
        fonds.setContactPerson("王五");
        fonds.setContactPhone("13700137000");
        fonds.setStatus(Fonds.STATUS_INACTIVE);

        assertEquals("FD-003", fonds.getFondsNo());
        assertEquals("新全宗", fonds.getFondsName());
        assertEquals(Fonds.TYPE_EXTERNAL, fonds.getFondsType());
        assertEquals("新全宗描述", fonds.getDescription());
        assertEquals("王五", fonds.getContactPerson());
        assertEquals("13700137000", fonds.getContactPhone());
        assertEquals(Fonds.STATUS_INACTIVE, fonds.getStatus());
    }

    @Test
    void testToString() {
        Fonds fonds = Fonds.builder()
                .fondsNo("TEST")
                .build();

        String str = fonds.toString();
        assertNotNull(str);
        assertTrue(str.contains("Fonds"));
    }
}
