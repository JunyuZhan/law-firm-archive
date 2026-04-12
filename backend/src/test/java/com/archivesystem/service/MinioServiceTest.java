package com.archivesystem.service;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MultipartFile multipartFile;

    private MinioService minioService;

    @BeforeEach
    void setUp() throws Exception {
        minioService = new MinioService();
        
        // 设置私有字段
        ReflectionTestUtils.setField(minioService, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(minioService, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(minioService, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(minioService, "bucketName", "archives");
        ReflectionTestUtils.setField(minioService, "minioClient", minioClient);
    }

    @Test
    void testGetBucketName() {
        assertEquals("archives", minioService.getBucketName());
    }

    @Test
    void testUpload_Success() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        assertDoesNotThrow(() -> minioService.upload("test/file.txt", inputStream, 12, "text/plain"));

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUpload_WithNullContentType() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        assertDoesNotThrow(() -> minioService.upload("test/file.bin", inputStream, 12, null));

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUpload_Failure() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("上传失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.upload("test/file.txt", inputStream, 12, "text/plain")
        );

        assertTrue(exception.getMessage().contains("文件上传失败"));
    }

    @Test
    void testUploadFile_Success() throws Exception {
        byte[] content = "test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String result = minioService.uploadFile(multipartFile, "test/file.txt");

        assertEquals("test/file.txt", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadFile_Failure() throws Exception {
        when(multipartFile.getInputStream()).thenThrow(new RuntimeException("读取文件失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.uploadFile(multipartFile, "test/file.txt")
        );

        assertTrue(exception.getMessage().contains("文件上传失败"));
    }

    @Test
    void testUploadBytes_Success() throws Exception {
        byte[] data = "test content".getBytes();
        
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String result = minioService.uploadBytes(data, "test/file.txt", "text/plain");

        assertEquals("test/file.txt", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void testUploadBytes_Failure() throws Exception {
        byte[] data = "test content".getBytes();
        
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("上传失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.uploadBytes(data, "test/file.txt", "text/plain")
        );

        assertTrue(exception.getMessage().contains("文件上传失败"));
    }

    @Test
    void testGetFile_Success() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        InputStream result = minioService.getFile("test/file.txt");

        assertNotNull(result);
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void testGetFile_Failure() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("获取文件失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.getFile("test/file.txt")
        );

        assertTrue(exception.getMessage().contains("获取文件失败"));
    }

    @Test
    void testGetPresignedUrl_DefaultExpiry() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/archives/test/file.txt?token=xxx");

        String result = minioService.getPresignedUrl("test/file.txt");

        assertNotNull(result);
        assertTrue(result.contains("test/file.txt"));
    }

    @Test
    void testGetPresignedUrl_CustomExpiry() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/archives/test/file.txt?token=xxx");

        String result = minioService.getPresignedUrl("test/file.txt", 7200);

        assertNotNull(result);
        assertTrue(result.contains("test/file.txt"));
    }

    @Test
    void testGetPresignedUrl_Failure() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("获取URL失败"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.getPresignedUrl("test/file.txt")
        );

        assertTrue(exception.getMessage().contains("获取下载链接失败"));
    }

    @Test
    void testDelete_Success() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> minioService.delete("test/file.txt"));

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void testDelete_Failure() throws Exception {
        doThrow(new RuntimeException("删除失败"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        // delete方法不抛出异常，只记录日志
        assertDoesNotThrow(() -> minioService.delete("test/file.txt"));
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> minioService.deleteFile("test/file.txt"));

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void testExists_True() throws Exception {
        StatObjectResponse mockResponse = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockResponse);

        boolean result = minioService.exists("test/file.txt");

        assertTrue(result);
    }

    @Test
    void testExists_False() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new RuntimeException("文件不存在"));

        boolean result = minioService.exists("test/file.txt");

        assertFalse(result);
    }

    @Test
    void testInit_BucketExists() throws Exception {
        // 创建新实例来测试init方法
        MinioService newService = new MinioService();
        ReflectionTestUtils.setField(newService, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(newService, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(newService, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(newService, "bucketName", "archives");

        // init方法会在@PostConstruct时调用，这里测试不会抛出异常
        assertDoesNotThrow(() -> newService.init());
    }

    @Test
    void testDownloadAndStore_Failure() {
        // 测试下载无效URL
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            minioService.downloadAndStore("invalid-url", "test/file.txt")
        );

        assertTrue(exception.getMessage().contains("文件下载存储失败"));
    }
}
