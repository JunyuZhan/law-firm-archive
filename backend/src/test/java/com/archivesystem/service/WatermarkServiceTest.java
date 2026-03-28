package com.archivesystem.service;

import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.WatermarkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatermarkServiceTest {

    @InjectMocks
    private WatermarkServiceImpl watermarkService;

    private byte[] testImageData;

    @BeforeEach
    void setUp() throws IOException {
        // 创建测试图片
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        testImageData = baos.toByteArray();
    }

    @Test
    void testAddWatermarkToImage_Success() {
        byte[] result = watermarkService.addWatermarkToImage(testImageData, "测试水印");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testAddWatermarkToImage_InvalidData() {
        byte[] invalidData = "not an image".getBytes();

        byte[] result = watermarkService.addWatermarkToImage(invalidData, "测试水印");

        // 无效数据应返回原始数据
        assertArrayEquals(invalidData, result);
    }

    @Test
    void testAddWatermarkToImage_EmptyData() {
        byte[] emptyData = new byte[0];

        byte[] result = watermarkService.addWatermarkToImage(emptyData, "测试水印");

        assertArrayEquals(emptyData, result);
    }

    @Test
    void testAddWatermarkToImage_NullWatermarkText() {
        byte[] result = watermarkService.addWatermarkToImage(testImageData, null);

        assertNotNull(result);
    }

    @Test
    void testAddWatermarkToPdf() {
        byte[] pdfData = "PDF content".getBytes();

        byte[] result = watermarkService.addWatermarkToPdf(pdfData, "测试水印");

        // PDF水印功能待实现，返回原始数据
        assertArrayEquals(pdfData, result);
    }

    @Test
    void testGetUserWatermarkText_WithRealName() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("张三");
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("zhangsan");

            String result = watermarkService.getUserWatermarkText();

            assertNotNull(result);
            assertTrue(result.contains("张三"));
        }
    }

    @Test
    void testGetUserWatermarkText_WithUsername() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("");
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("zhangsan");

            String result = watermarkService.getUserWatermarkText();

            assertNotNull(result);
            assertTrue(result.contains("zhangsan"));
        }
    }

    @Test
    void testGetUserWatermarkText_NoUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn(null);
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn(null);

            String result = watermarkService.getUserWatermarkText();

            assertNotNull(result);
            assertTrue(result.contains("档案管理系统"));
        }
    }

    @Test
    void testGetUserWatermarkText_EmptyStrings() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentRealName).thenReturn("");
            securityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("");

            String result = watermarkService.getUserWatermarkText();

            assertNotNull(result);
            assertTrue(result.contains("档案管理系统"));
        }
    }

    @Test
    void testAddWatermarkToImage_LongWatermarkText() {
        String longText = "这是一个非常长的水印文本，用于测试长文本水印的显示效果";

        byte[] result = watermarkService.addWatermarkToImage(testImageData, longText);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testAddWatermarkToImage_SpecialCharacters() {
        String specialText = "测试@#$%^&*()水印";

        byte[] result = watermarkService.addWatermarkToImage(testImageData, specialText);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
