package com.archivesystem.service;

import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.impl.PreviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class PreviewServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private DigitalFileMapper digitalFileMapper;

    @Mock
    private AccessLogService accessLogService;

    @InjectMocks
    private PreviewServiceImpl previewService;

    private DigitalFile testFile;

    @BeforeEach
    void setUp() {
        testFile = new DigitalFile();
        testFile.setId(1L);
        testFile.setArchiveId(100L);
        testFile.setFileName("test.jpg");
        testFile.setFileExtension("jpg");
        testFile.setStoragePath("archives/2026/02/13/test.jpg");
        testFile.setMimeType("image/jpeg");
    }

    @Test
    void testGenerateThumbnail_FileNotFound() {
        when(digitalFileMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> previewService.generateThumbnail(999L));
    }

    @Test
    void testGenerateThumbnail_AlreadyHasThumbnail() {
        testFile.setThumbnailPath("thumbnails/existing.jpg");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);

        String result = previewService.generateThumbnail(1L);

        assertEquals("thumbnails/existing.jpg", result);
        verify(minioService, never()).getFile(anyString());
    }

    @Test
    void testGenerateThumbnail_UnsupportedType() {
        testFile.setFileExtension("doc");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);

        String result = previewService.generateThumbnail(1L);

        assertNull(result);
    }

    @Test
    void testGenerateThumbnail_NullExtension() {
        testFile.setFileExtension(null);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);

        String result = previewService.generateThumbnail(1L);

        assertNull(result);
    }

    @Test
    void testGetPreviewUrl_FileNotFound() {
        when(digitalFileMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            previewService.getPreviewUrl(999L, "192.168.1.1"));
    }

    @Test
    void testGetPreviewUrl_PreviewableFile() {
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenReturn("http://minio/test.jpg");

        String result = previewService.getPreviewUrl(1L, "192.168.1.1");

        assertNotNull(result);
        assertTrue(result.contains("test.jpg"));
        verify(accessLogService).logPreview(100L, 1L, "192.168.1.1");
    }

    @Test
    void testGetPreviewUrl_WithPreviewPath() {
        testFile.setPreviewPath("previews/preview.pdf");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getPresignedUrl(eq("previews/preview.pdf"), anyInt()))
            .thenReturn("http://minio/preview.pdf");

        String result = previewService.getPreviewUrl(1L, "192.168.1.1");

        assertNotNull(result);
        assertTrue(result.contains("preview.pdf"));
    }

    @Test
    void testGetPreviewUrl_UnsupportedType() {
        testFile.setFileExtension("doc");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);

        String result = previewService.getPreviewUrl(1L, "192.168.1.1");

        assertNull(result);
        verify(accessLogService).logPreview(100L, 1L, "192.168.1.1");
    }

    @Test
    void testGetPreviewUrl_NullExtension() {
        testFile.setFileExtension(null);
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);

        String result = previewService.getPreviewUrl(1L, "192.168.1.1");

        assertNull(result);
    }

    @Test
    void testIsPreviewable_PreviewableFile() {
        testFile.setFileExtension("pdf");
        
        boolean result = previewService.isPreviewable(testFile);
        
        assertTrue(result);
    }

    @Test
    void testIsPreviewable_NonPreviewableFile() {
        testFile.setFileExtension("doc");
        
        boolean result = previewService.isPreviewable(testFile);
        
        assertFalse(result);
    }

    @Test
    void testIsPreviewable_NullFile() {
        boolean result = previewService.isPreviewable(null);
        
        assertFalse(result);
    }

    @Test
    void testIsPreviewable_NullExtension() {
        testFile.setFileExtension(null);
        
        boolean result = previewService.isPreviewable(testFile);
        
        assertFalse(result);
    }

    @Test
    void testIsDirectPreviewable_AllTypes() {
        assertTrue(previewService.isDirectPreviewable("pdf"));
        assertTrue(previewService.isDirectPreviewable("jpg"));
        assertTrue(previewService.isDirectPreviewable("jpeg"));
        assertTrue(previewService.isDirectPreviewable("png"));
        assertTrue(previewService.isDirectPreviewable("gif"));
        assertTrue(previewService.isDirectPreviewable("bmp"));
        assertTrue(previewService.isDirectPreviewable("webp"));
        assertTrue(previewService.isDirectPreviewable("svg"));
    }

    @Test
    void testIsDirectPreviewable_CaseInsensitive() {
        assertTrue(previewService.isDirectPreviewable("PDF"));
        assertTrue(previewService.isDirectPreviewable("JPG"));
        assertTrue(previewService.isDirectPreviewable("Png"));
    }

    @Test
    void testIsDirectPreviewable_NonPreviewable() {
        assertFalse(previewService.isDirectPreviewable("doc"));
        assertFalse(previewService.isDirectPreviewable("xls"));
        assertFalse(previewService.isDirectPreviewable("zip"));
    }

    @Test
    void testIsDirectPreviewable_NullExtension() {
        assertFalse(previewService.isDirectPreviewable(null));
    }

    @Test
    void testGenerateThumbnail_MinioException() throws Exception {
        testFile.setFileExtension("jpg");
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getFile(anyString())).thenThrow(new RuntimeException("MinIO不可用"));

        String result = previewService.generateThumbnail(1L);

        assertNull(result);
    }

    @Test
    void testGenerateThumbnail_Success() throws Exception {
        // 创建测试图片
        BufferedImage testImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 800, 600);
        g.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "jpg", baos);
        byte[] imageData = baos.toByteArray();
        
        testFile.setFileExtension("jpg");
        testFile.setThumbnailPath(null);
        
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getFile(anyString())).thenReturn(new ByteArrayInputStream(imageData));
        doNothing().when(minioService).upload(anyString(), any(), anyLong(), anyString());
        when(digitalFileMapper.updateById(any(DigitalFile.class))).thenReturn(1);

        String result = previewService.generateThumbnail(1L);

        assertNotNull(result);
        assertTrue(result.contains("thumbnails/"));
        verify(minioService).upload(anyString(), any(), anyLong(), eq("image/jpeg"));
        verify(digitalFileMapper).updateById(any(DigitalFile.class));
    }

    @Test
    void testGenerateThumbnail_SmallImage() throws Exception {
        // 创建小于缩略图最大尺寸的图片
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", baos);
        byte[] imageData = baos.toByteArray();
        
        testFile.setFileExtension("png");
        testFile.setThumbnailPath(null);
        
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        when(minioService.getFile(anyString())).thenReturn(new ByteArrayInputStream(imageData));
        doNothing().when(minioService).upload(anyString(), any(), anyLong(), anyString());
        when(digitalFileMapper.updateById(any(DigitalFile.class))).thenReturn(1);

        String result = previewService.generateThumbnail(1L);

        assertNotNull(result);
    }

    @Test
    void testGenerateThumbnail_InvalidImage() throws Exception {
        testFile.setFileExtension("jpg");
        testFile.setThumbnailPath(null);
        
        when(digitalFileMapper.selectById(1L)).thenReturn(testFile);
        // 返回无效的图片数据
        when(minioService.getFile(anyString())).thenReturn(new ByteArrayInputStream("not an image".getBytes()));

        String result = previewService.generateThumbnail(1L);

        assertNull(result);
    }

    @Test
    void testIsPreviewable_AllSupportedTypes() {
        String[] previewableTypes = {"pdf", "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"};
        
        for (String type : previewableTypes) {
            testFile.setFileExtension(type);
            assertTrue(previewService.isPreviewable(testFile), "应该支持预览: " + type);
        }
    }
}
