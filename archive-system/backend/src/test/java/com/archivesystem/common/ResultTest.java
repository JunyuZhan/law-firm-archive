package com.archivesystem.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccessWithoutData() {
        Result<String> result = Result.success();
        
        assertTrue(result.getSuccess());
        assertEquals("200", result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithData() {
        String data = "test data";
        Result<String> result = Result.success(data);
        
        assertTrue(result.getSuccess());
        assertEquals("200", result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    void testSuccessWithMessageAndData() {
        String message = "Operation successful";
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        
        Result<Map<String, Object>> result = Result.success(message, data);
        
        assertTrue(result.getSuccess());
        assertEquals("200", result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    void testErrorWithMessage() {
        Result<String> result = Result.error("Error message");
        
        assertFalse(result.getSuccess());
        assertEquals("500", result.getCode());
        assertEquals("Error message", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        Result<String> result = Result.error("404", "Not found");
        
        assertFalse(result.getSuccess());
        assertEquals("404", result.getCode());
        assertEquals("Not found", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithList() {
        var list = Arrays.asList("item1", "item2", "item3");
        Result<java.util.List<String>> result = Result.success(list);
        
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData().size());
    }

    @Test
    void testSuccessWithEmptyList() {
        java.util.List<String> list = Collections.emptyList();
        Result<java.util.List<String>> result = Result.success(list);
        
        assertTrue(result.getSuccess());
        assertTrue(result.getData().isEmpty());
    }
}
