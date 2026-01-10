package com.lawfirm.infrastructure.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成服务
 * 基于 ZXing 库实现，支持生成 Base64 编码的二维码图片
 * 
 * 使用场景：
 * - 函件防伪验证
 * - 合同电子签章
 * - 项目信息查询
 * - 其他需要二维码的业务场景
 * 
 * @author LawFirm
 */
@Slf4j
@Service
public class QrCodeService {

    /**
     * 默认二维码尺寸
     */
    private static final int DEFAULT_SIZE = 200;

    /**
     * 默认字符集
     */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 默认图片格式
     */
    private static final String DEFAULT_FORMAT = "PNG";

    /**
     * 生成二维码图片（Base64编码）
     * 
     * @param content 二维码内容
     * @param size 二维码尺寸（像素），默认200
     * @return Base64编码的图片数据（data:image/png;base64,...）
     */
    public String generateQrCodeBase64(String content, Integer size) {
        return generateQrCodeBase64(content, size, null, null);
    }

    /**
     * 生成二维码图片（Base64编码）
     * 
     * @param content 二维码内容
     * @param size 二维码尺寸（像素）
     * @param logoPath 中心Logo路径（可选）
     * @param logoSize Logo尺寸（可选，默认size的1/5）
     * @return Base64编码的图片数据（data:image/png;base64,...）
     */
    public String generateQrCodeBase64(String content, Integer size, String logoPath, Integer logoSize) {
        try {
            BufferedImage qrImage = generateQrCodeImage(content, size, logoPath, logoSize);
            return imageToBase64(qrImage);
        } catch (Exception e) {
            log.error("生成二维码失败: content={}, error={}", content, e.getMessage(), e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成二维码图片（BufferedImage）
     * 
     * @param content 二维码内容
     * @param size 二维码尺寸（像素）
     * @param logoPath 中心Logo路径（可选）
     * @param logoSize Logo尺寸（可选）
     * @return BufferedImage对象
     */
    public BufferedImage generateQrCodeImage(String content, Integer size, String logoPath, Integer logoSize) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }

        int qrSize = size != null && size > 0 ? size : DEFAULT_SIZE;
        
        try {
            // 设置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 高容错率
            hints.put(EncodeHintType.CHARACTER_SET, DEFAULT_CHARSET);
            hints.put(EncodeHintType.MARGIN, 1); // 边距

            // 生成二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

            // 转换为图片
            BufferedImage qrImage = new BufferedImage(qrSize, qrSize, BufferedImage.TYPE_INT_RGB);
            qrImage.createGraphics();

            Graphics2D graphics = (Graphics2D) qrImage.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, qrSize, qrSize);
            graphics.setColor(Color.BLACK);

            // 绘制二维码
            for (int x = 0; x < qrSize; x++) {
                for (int y = 0; y < qrSize; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            // 添加Logo（如果有）
            if (logoPath != null && !logoPath.isEmpty()) {
                addLogoToQrCode(qrImage, logoPath, logoSize != null ? logoSize : qrSize / 5);
            }

            return qrImage;
        } catch (WriterException e) {
            log.error("生成二维码失败: content={}, error={}", content, e.getMessage(), e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 在二维码中心添加Logo
     */
    private void addLogoToQrCode(BufferedImage qrImage, String logoPath, int logoSize) {
        try {
            // 读取Logo图片
            BufferedImage logoImage = ImageIO.read(getClass().getResourceAsStream(logoPath));
            if (logoImage == null) {
                log.warn("Logo图片不存在: {}", logoPath);
                return;
            }

            // 缩放Logo
            Image scaledLogo = logoImage.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
            BufferedImage bufferedLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D logoGraphics = bufferedLogo.createGraphics();
            logoGraphics.drawImage(scaledLogo, 0, 0, null);
            logoGraphics.dispose();

            // 在二维码中心绘制Logo
            Graphics2D graphics = qrImage.createGraphics();
            int x = (qrImage.getWidth() - logoSize) / 2;
            int y = (qrImage.getHeight() - logoSize) / 2;
            graphics.drawImage(bufferedLogo, x, y, null);
            graphics.dispose();
        } catch (IOException e) {
            log.warn("添加Logo失败: logoPath={}, error={}", logoPath, e.getMessage());
        }
    }

    /**
     * 将BufferedImage转换为Base64字符串
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, DEFAULT_FORMAT, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/" + DEFAULT_FORMAT.toLowerCase() + ";base64," + base64;
    }

    /**
     * 生成二维码并返回字节数组（用于直接下载）
     * 
     * @param content 二维码内容
     * @param size 二维码尺寸
     * @return PNG格式的字节数组
     */
    public byte[] generateQrCodeBytes(String content, Integer size) {
        try {
            BufferedImage qrImage = generateQrCodeImage(content, size, null, null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, DEFAULT_FORMAT, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("生成二维码字节数组失败: content={}, error={}", content, e.getMessage(), e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}

