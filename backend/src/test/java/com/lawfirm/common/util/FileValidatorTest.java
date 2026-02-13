package com.lawfirm.common.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * FileValidator 单元测试
 *
 * <p>测试文件验证功能： - 扩展名白名单/黑名单 - MIME类型验证 - 文件大小限制 - 文件签名验证
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@DisplayName("FileValidator 单元测试")
class FileValidatorTest {

  @Nested
  @DisplayName("扩展名验证")
  class ExtensionTests {

    @Test
    @DisplayName("允许的扩展名 - PDF")
    void validate_PdfFile_ShouldPass() {
      // Given - PDF文件魔数
      byte[] pdfContent = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
      MockMultipartFile file =
          new MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertTrue(result.isValid(), "PDF文件应该通过验证");
    }

    @Test
    @DisplayName("禁止的扩展名 - EXE")
    void validate_ExeFile_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[10]);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "EXE文件应该被禁止");
      assertTrue(
          result.getErrorMessage().contains("禁止") || result.getErrorMessage().contains("可执行"));
    }

    @Test
    @DisplayName("禁止的扩展名 - SH")
    void validate_ShellScript_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile("file", "script.sh", "application/x-sh", "#!/bin/bash".getBytes());

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "Shell脚本应该被禁止");
    }

    @Test
    @DisplayName("禁止的扩展名 - BAT")
    void validate_BatchFile_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile(
              "file", "virus.bat", "application/x-msdos-program", "@echo off".getBytes());

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "批处理文件应该被禁止");
    }

    @Test
    @DisplayName("禁止的扩展名 - JAR")
    void validate_JarFile_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile("file", "app.jar", "application/java-archive", new byte[10]);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "JAR文件应该被禁止");
    }

    @Test
    @DisplayName("不支持的扩展名")
    void validate_UnsupportedExtension_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile("file", "data.xyz", "application/octet-stream", new byte[10]);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "不支持的扩展名应该被拒绝");
    }
  }

  @Nested
  @DisplayName("文件内容验证")
  class ContentTests {

    @Test
    @DisplayName("空文件应该被拒绝")
    void validate_EmptyFile_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "空文件应该被拒绝");
      assertTrue(result.getErrorMessage().contains("空"));
    }

    @Test
    @DisplayName("null文件应该被拒绝")
    void validate_NullFile_ShouldFail() {
      // When
      FileValidator.ValidationResult result = FileValidator.validate(null);

      // Then
      assertFalse(result.isValid(), "null文件应该被拒绝");
    }
  }

  @Nested
  @DisplayName("文件大小验证")
  class SizeTests {

    @Test
    @DisplayName("文件大小在限制内")
    void validate_FileWithinSizeLimit_ShouldPass() {
      // Given - 1MB文件
      byte[] content = new byte[1024 * 1024];
      // 添加PNG魔数
      content[0] = (byte) 0x89;
      content[1] = 0x50;
      content[2] = 0x4E;
      content[3] = 0x47;
      content[4] = 0x0D;
      content[5] = 0x0A;
      content[6] = 0x1A;
      content[7] = 0x0A;

      MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", content);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertTrue(result.isValid(), "1MB文件应该通过大小验证");
    }
  }

  @Nested
  @DisplayName("MIME类型验证")
  class MimeTypeTests {

    @Test
    @DisplayName("允许的MIME类型 - image/png")
    void validate_PngMimeType_ShouldPass() {
      // Given - PNG魔数
      byte[] pngContent =
          new byte[] {
            (byte) 0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
            0x00,
            0x00,
            0x00,
            0x0D,
            0x49,
            0x48,
            0x44,
            0x52
          };
      MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngContent);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertTrue(result.isValid(), "PNG图片应该通过验证");
    }

    @Test
    @DisplayName("允许的MIME类型 - image/jpeg")
    void validate_JpegMimeType_ShouldPass() {
      // Given - JPEG魔数
      byte[] jpegContent =
          new byte[] {
            (byte) 0xFF,
            (byte) 0xD8,
            (byte) 0xFF,
            (byte) 0xE0,
            0x00,
            0x10,
            0x4A,
            0x46,
            0x49,
            0x46,
            0x00,
            0x01
          };
      MockMultipartFile file =
          new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegContent);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertTrue(result.isValid(), "JPEG图片应该通过验证");
    }
  }

  @Nested
  @DisplayName("安全性测试")
  class SecurityTests {

    @Test
    @DisplayName("检测伪装文件 - EXE伪装成JPG")
    void validate_DisguisedExeAsJpg_ShouldFail() {
      // Given - EXE魔数伪装成JPG
      byte[] exeContent = new byte[] {0x4D, 0x5A, 0x00, 0x00}; // MZ header
      MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", exeContent);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "伪装的EXE文件应该被检测到");
    }

    @Test
    @DisplayName("检测双扩展名攻击")
    void validate_DoubleExtension_ShouldFail() {
      // Given
      MockMultipartFile file =
          new MockMultipartFile(
              "file", "document.pdf.exe", "application/octet-stream", new byte[10]);

      // When
      FileValidator.ValidationResult result = FileValidator.validate(file);

      // Then
      assertFalse(result.isValid(), "双扩展名文件应该被拒绝");
    }
  }
}
