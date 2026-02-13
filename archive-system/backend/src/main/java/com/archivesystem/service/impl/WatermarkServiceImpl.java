package com.archivesystem.service.impl;

import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.WatermarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 水印服务实现.
 */
@Slf4j
@Service
public class WatermarkServiceImpl implements WatermarkService {

    private static final float WATERMARK_OPACITY = 0.3f;
    private static final int WATERMARK_FONT_SIZE = 36;
    private static final int WATERMARK_ANGLE = -30; // 旋转角度

    @Override
    public byte[] addWatermarkToImage(byte[] imageData, String watermarkText) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage originalImage = ImageIO.read(bais);
            
            if (originalImage == null) {
                log.error("无法读取图片数据");
                return imageData;
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 创建带水印的图片
            BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = watermarkedImage.createGraphics();

            // 绘制原图
            g2d.drawImage(originalImage, 0, 0, null);

            // 设置水印属性
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, WATERMARK_OPACITY));
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("SimSun", Font.BOLD, WATERMARK_FONT_SIZE));
            
            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 计算水印间距
            int textWidth = g2d.getFontMetrics().stringWidth(watermarkText);
            int textHeight = g2d.getFontMetrics().getHeight();
            int xSpacing = textWidth + 100;
            int ySpacing = textHeight + 80;

            // 保存原始变换
            AffineTransform originalTransform = g2d.getTransform();

            // 旋转画布
            g2d.rotate(Math.toRadians(WATERMARK_ANGLE), width / 2.0, height / 2.0);

            // 铺满水印
            for (int x = -width; x < width * 2; x += xSpacing) {
                for (int y = -height; y < height * 2; y += ySpacing) {
                    g2d.drawString(watermarkText, x, y);
                }
            }

            // 恢复原始变换
            g2d.setTransform(originalTransform);
            g2d.dispose();

            // 输出为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(watermarkedImage, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("添加图片水印失败", e);
            return imageData;
        }
    }

    @Override
    public byte[] addWatermarkToPdf(byte[] pdfData, String watermarkText) {
        // PDF水印需要使用PDFBox库
        // 这里先返回原始数据，后续可以集成PDFBox实现
        log.info("PDF水印功能待实现");
        return pdfData;
    }

    @Override
    public String getUserWatermarkText() {
        String realName = SecurityUtils.getCurrentRealName();
        String username = SecurityUtils.getCurrentUsername();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        
        if (realName != null && !realName.isEmpty()) {
            return realName + " " + time;
        } else if (username != null && !username.isEmpty()) {
            return username + " " + time;
        }
        return "档案管理系统 " + time;
    }
}
