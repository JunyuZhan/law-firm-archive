package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ArchiveLocationTest {

    @Test
    void testBuilder() {
        ArchiveLocation location = ArchiveLocation.builder()
                .locationCode("LOC-001")
                .locationName("A区1号架3层")
                .roomName("档案库房A")
                .area("A区")
                .shelfNo("1")
                .layerNo("3")
                .totalCapacity(100)
                .usedCapacity(50)
                .status(ArchiveLocation.STATUS_AVAILABLE)
                .remarks("常用文书档案存放区")
                .build();

        assertEquals("LOC-001", location.getLocationCode());
        assertEquals("A区1号架3层", location.getLocationName());
        assertEquals("档案库房A", location.getRoomName());
        assertEquals("A区", location.getArea());
        assertEquals("1", location.getShelfNo());
        assertEquals("3", location.getLayerNo());
        assertEquals(100, location.getTotalCapacity());
        assertEquals(50, location.getUsedCapacity());
        assertEquals(ArchiveLocation.STATUS_AVAILABLE, location.getStatus());
        assertEquals("常用文书档案存放区", location.getRemarks());
    }

    @Test
    void testNoArgsConstructor() {
        ArchiveLocation location = new ArchiveLocation();

        assertNull(location.getLocationCode());
        assertNull(location.getLocationName());
    }

    @Test
    void testStatusConstants() {
        assertEquals("AVAILABLE", ArchiveLocation.STATUS_AVAILABLE);
        assertEquals("FULL", ArchiveLocation.STATUS_FULL);
        assertEquals("DISABLED", ArchiveLocation.STATUS_DISABLED);
    }

    @Test
    void testSettersAndGetters() {
        ArchiveLocation location = new ArchiveLocation();

        location.setLocationCode("LOC-002");
        location.setLocationName("B区2号架1层");
        location.setRoomName("档案库房B");
        location.setArea("B区");
        location.setShelfNo("2");
        location.setLayerNo("1");
        location.setTotalCapacity(200);
        location.setUsedCapacity(200);
        location.setStatus(ArchiveLocation.STATUS_FULL);

        assertEquals("LOC-002", location.getLocationCode());
        assertEquals("B区2号架1层", location.getLocationName());
        assertEquals("档案库房B", location.getRoomName());
        assertEquals("B区", location.getArea());
        assertEquals("2", location.getShelfNo());
        assertEquals("1", location.getLayerNo());
        assertEquals(200, location.getTotalCapacity());
        assertEquals(200, location.getUsedCapacity());
        assertEquals(ArchiveLocation.STATUS_FULL, location.getStatus());
    }

    @Test
    void testCapacityCalculation() {
        ArchiveLocation location = ArchiveLocation.builder()
                .totalCapacity(100)
                .usedCapacity(75)
                .build();

        // 剩余容量
        int remaining = location.getTotalCapacity() - location.getUsedCapacity();
        assertEquals(25, remaining);
    }

    @Test
    void testDisabledStatus() {
        ArchiveLocation location = ArchiveLocation.builder()
                .locationCode("LOC-OLD")
                .status(ArchiveLocation.STATUS_DISABLED)
                .remarks("已停用")
                .build();

        assertEquals(ArchiveLocation.STATUS_DISABLED, location.getStatus());
    }

    @Test
    void testToString() {
        ArchiveLocation location = ArchiveLocation.builder()
                .locationCode("TEST")
                .build();

        String str = location.toString();
        assertNotNull(str);
        assertTrue(str.contains("ArchiveLocation"));
    }
}
