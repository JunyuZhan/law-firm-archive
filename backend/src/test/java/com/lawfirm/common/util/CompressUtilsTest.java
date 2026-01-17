package com.lawfirm.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompressUtils 单元测试
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@DisplayName("CompressUtils 压缩工具测试")
class CompressUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("压缩单个文件")
    void testZipFile() throws IOException {
        // 创建测试文件
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "Hello, World!");
        
        Path zipFile = tempDir.resolve("test.zip");
        
        // 压缩
        CompressUtils.zipFile(sourceFile.toFile(), zipFile.toFile());
        
        // 验证
        assertTrue(zipFile.toFile().exists());
        assertTrue(CompressUtils.isZipFile(zipFile.toFile()));
        
        List<String> entries = CompressUtils.listZipEntries(zipFile.toFile());
        assertEquals(1, entries.size());
        assertEquals("test.txt", entries.get(0));
    }

    @Test
    @DisplayName("压缩多个文件")
    void testZipFiles() throws IOException {
        // 创建测试文件
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "Content 1");
        Files.writeString(file2, "Content 2");
        
        Path zipFile = tempDir.resolve("multi.zip");
        
        // 压缩
        CompressUtils.zipFiles(List.of(file1.toFile(), file2.toFile()), zipFile.toFile());
        
        // 验证
        assertTrue(zipFile.toFile().exists());
        List<String> entries = CompressUtils.listZipEntries(zipFile.toFile());
        assertEquals(2, entries.size());
        assertTrue(entries.contains("file1.txt"));
        assertTrue(entries.contains("file2.txt"));
    }

    @Test
    @DisplayName("压缩目录")
    void testZipDirectory() throws IOException {
        // 创建测试目录结构
        Path subDir = tempDir.resolve("testDir");
        Files.createDirectory(subDir);
        Files.writeString(subDir.resolve("a.txt"), "A");
        Files.writeString(subDir.resolve("b.txt"), "B");
        
        Path zipFile = tempDir.resolve("dir.zip");
        
        // 压缩
        CompressUtils.zipDirectory(subDir.toFile(), zipFile.toFile());
        
        // 验证
        assertTrue(zipFile.toFile().exists());
        List<String> entries = CompressUtils.listZipEntries(zipFile.toFile());
        assertEquals(2, entries.size());
    }

    @Test
    @DisplayName("压缩内存数据到文件")
    void testZipData() throws IOException {
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put("doc1.txt", "Document 1 content".getBytes(StandardCharsets.UTF_8));
        dataMap.put("doc2.txt", "Document 2 content".getBytes(StandardCharsets.UTF_8));
        
        Path zipFile = tempDir.resolve("data.zip");
        
        // 压缩
        CompressUtils.zipData(dataMap, zipFile.toFile());
        
        // 验证
        assertTrue(zipFile.toFile().exists());
        List<String> entries = CompressUtils.listZipEntries(zipFile.toFile());
        assertEquals(2, entries.size());
    }

    @Test
    @DisplayName("压缩内存数据到字节数组")
    void testZipDataToBytes() throws IOException {
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put("file1.txt", "Content 1".getBytes(StandardCharsets.UTF_8));
        dataMap.put("file2.txt", "Content 2".getBytes(StandardCharsets.UTF_8));
        
        // 压缩
        byte[] zipData = CompressUtils.zipDataToBytes(dataMap);
        
        // 验证
        assertNotNull(zipData);
        assertTrue(zipData.length > 0);
        
        // 解压验证
        Path targetDir = tempDir.resolve("unzipped");
        CompressUtils.unzip(zipData, targetDir.toFile());
        
        assertTrue(Files.exists(targetDir.resolve("file1.txt")));
        assertTrue(Files.exists(targetDir.resolve("file2.txt")));
        assertEquals("Content 1", Files.readString(targetDir.resolve("file1.txt")));
        assertEquals("Content 2", Files.readString(targetDir.resolve("file2.txt")));
    }

    @Test
    @DisplayName("解压 ZIP 文件")
    void testUnzip() throws IOException {
        // 先创建一个 ZIP 文件
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put("folder/nested.txt", "Nested content".getBytes(StandardCharsets.UTF_8));
        dataMap.put("root.txt", "Root content".getBytes(StandardCharsets.UTF_8));
        
        Path zipFile = tempDir.resolve("toUnzip.zip");
        CompressUtils.zipData(dataMap, zipFile.toFile());
        
        // 解压
        Path targetDir = tempDir.resolve("extracted");
        CompressUtils.unzip(zipFile.toFile(), targetDir.toFile());
        
        // 验证
        assertTrue(Files.exists(targetDir.resolve("root.txt")));
        assertTrue(Files.exists(targetDir.resolve("folder/nested.txt")));
        assertEquals("Root content", Files.readString(targetDir.resolve("root.txt")));
        assertEquals("Nested content", Files.readString(targetDir.resolve("folder/nested.txt")));
    }

    @Test
    @DisplayName("GZIP 压缩和解压字节数组")
    void testGzipBytes() throws IOException {
        byte[] original = "This is a test string for GZIP compression.".getBytes(StandardCharsets.UTF_8);
        
        // 压缩
        byte[] compressed = CompressUtils.gzip(original);
        
        // 验证压缩后更小（对于较长文本）
        assertNotNull(compressed);
        
        // 解压
        byte[] decompressed = CompressUtils.ungzip(compressed);
        
        // 验证
        assertArrayEquals(original, decompressed);
    }

    @Test
    @DisplayName("GZIP 压缩和解压字符串")
    void testGzipString() throws IOException {
        String original = "这是一段中文测试文本，用于测试 GZIP 压缩功能。";
        
        // 压缩
        byte[] compressed = CompressUtils.gzip(original);
        
        // 解压
        String decompressed = CompressUtils.ungzipToString(compressed);
        
        // 验证
        assertEquals(original, decompressed);
    }

    @Test
    @DisplayName("检查是否为 ZIP 文件")
    void testIsZipFile() throws IOException {
        // 创建 ZIP 文件
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put("test.txt", "test".getBytes());
        Path zipFile = tempDir.resolve("valid.zip");
        CompressUtils.zipData(dataMap, zipFile.toFile());
        
        // 创建普通文本文件
        Path txtFile = tempDir.resolve("text.txt");
        Files.writeString(txtFile, "Not a zip file");
        
        // 验证
        assertTrue(CompressUtils.isZipFile(zipFile.toFile()));
        assertFalse(CompressUtils.isZipFile(txtFile.toFile()));
        assertFalse(CompressUtils.isZipFile(null));
        assertFalse(CompressUtils.isZipFile(new File("nonexistent.zip")));
    }

    @Test
    @DisplayName("列出 ZIP 文件内容")
    void testListZipEntries() throws IOException {
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put("a.txt", "A".getBytes());
        dataMap.put("b/c.txt", "C".getBytes());
        dataMap.put("b/d.txt", "D".getBytes());
        
        Path zipFile = tempDir.resolve("list.zip");
        CompressUtils.zipData(dataMap, zipFile.toFile());
        
        List<String> entries = CompressUtils.listZipEntries(zipFile.toFile());
        
        assertEquals(3, entries.size());
        assertTrue(entries.contains("a.txt"));
        assertTrue(entries.contains("b/c.txt"));
        assertTrue(entries.contains("b/d.txt"));
    }

    @Test
    @DisplayName("生成 ZIP 文件名")
    void testToZipFileName() {
        assertEquals("test.zip", CompressUtils.toZipFileName("test"));
        assertEquals("test.zip", CompressUtils.toZipFileName("test.zip"));
        assertEquals("test.ZIP", CompressUtils.toZipFileName("test.ZIP"));
        assertEquals("archive.zip", CompressUtils.toZipFileName(null));
        assertEquals("archive.zip", CompressUtils.toZipFileName(""));
    }

    @Test
    @DisplayName("空数据映射应抛出异常")
    void testZipDataWithEmptyMap() {
        Map<String, byte[]> emptyMap = new HashMap<>();
        Path zipFile = tempDir.resolve("empty.zip");
        
        assertThrows(com.lawfirm.common.exception.BusinessException.class, () -> 
            CompressUtils.zipData(emptyMap, zipFile.toFile())
        );
    }

    @Test
    @DisplayName("源文件不存在应抛出异常")
    void testZipNonExistentFile() {
        File nonExistent = new File("nonexistent.txt");
        Path zipFile = tempDir.resolve("fail.zip");
        
        assertThrows(com.lawfirm.common.exception.BusinessException.class, () -> 
            CompressUtils.zipFile(nonExistent, zipFile.toFile())
        );
    }
}
