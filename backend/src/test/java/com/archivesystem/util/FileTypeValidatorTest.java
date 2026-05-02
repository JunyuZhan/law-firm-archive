package com.archivesystem.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

    @Test
    void testValidate_NullFile() {
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate((org.springframework.web.multipart.MultipartFile) null, "pdf");
        
        assertFalse(result.isValid());
        assertEquals("文件为空", result.getMessage());
    }

    @Test
    void testValidate_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "pdf");
        
        assertFalse(result.isValid());
        assertEquals("文件为空", result.getMessage());
    }

    @Test
    void testValidate_NullExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, null);
        
        assertFalse(result.isValid());
        assertEquals("文件扩展名为空", result.getMessage());
    }

    @Test
    void testValidate_BlankExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "content".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "   ");
        
        assertFalse(result.isValid());
        assertEquals("文件扩展名为空", result.getMessage());
    }

    @Test
    void testValidate_TextFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "txt");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_CsvFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv", "col1,col2\nval1,val2".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "csv");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_XmlFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.xml", "application/xml", "<?xml version=\"1.0\"?><root/>".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "xml");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_JsonFile() {
        MockMultipartFile file = new MockMultipartFile("file", "data.json", "application/json", "{\"key\":\"value\"}".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "json");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidPdf() {
        // PDF magic number: %PDF (25 50 44 46)
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "pdf");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_InvalidPdf() {
        // Invalid PDF content
        byte[] content = "This is not a PDF".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", content);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "pdf");
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("文件类型与扩展名不匹配"));
    }

    @Test
    void testValidate_ValidJpeg() {
        // JPEG magic number: FF D8 FF
        byte[] jpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "jpg");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidJpegWithJpegExtension() {
        byte[] jpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpeg", "image/jpeg", jpegContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "jpeg");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidPng() {
        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "png");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidGif() {
        // GIF magic number: GIF8 (47 49 46 38)
        byte[] gifContent = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", gifContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "gif");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidBmp() {
        // BMP magic number: BM (42 4D)
        byte[] bmpContent = new byte[]{0x42, 0x4D, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.bmp", "image/bmp", bmpContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "bmp");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidDocx() {
        // DOCX (ZIP) magic number: PK.. (50 4B 03 04)
        byte[] docxContent = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.docx", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "docx");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidXlsx() {
        byte[] xlsxContent = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "xlsx");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidDoc() {
        // DOC (OLE) magic number: D0 CF 11 E0 A1 B1 1A E1
        byte[] docContent = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        MockMultipartFile file = new MockMultipartFile("file", "test.doc", "application/msword", docContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "doc");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidZip() {
        byte[] zipContent = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "zip");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidZipAlternate() {
        // ZIP alternate magic: PK\x05\x06
        byte[] zipContent = new byte[]{0x50, 0x4B, 0x05, 0x06, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", zipContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "zip");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidRar() {
        // RAR magic number: Rar!.. (52 61 72 21 1A 07)
        byte[] rarContent = new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07};
        MockMultipartFile file = new MockMultipartFile("file", "test.rar", "application/x-rar-compressed", rarContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "rar");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_Valid7z() {
        // 7Z magic number: 37 7A BC AF 27 1C
        byte[] content = new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
        MockMultipartFile file = new MockMultipartFile("file", "test.7z", "application/x-7z-compressed", content);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "7z");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidTiff_LittleEndian() {
        // TIFF magic number (little endian): 49 49 2A 00
        byte[] tiffContent = new byte[]{0x49, 0x49, 0x2A, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.tif", "image/tiff", tiffContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "tif");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ValidTiff_BigEndian() {
        // TIFF magic number (big endian): 4D 4D 00 2A
        byte[] tiffContent = new byte[]{0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.tiff", "image/tiff", tiffContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "tiff");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_UnknownExtension() {
        // Unknown extension without magic number config should pass
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "application/octet-stream", "content".getBytes());
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "xyz");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_CaseInsensitiveExtension() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        MockMultipartFile file = new MockMultipartFile("file", "test.PDF", "application/pdf", pdfContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "PDF");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidateMimeType_ValidMatch() {
        assertTrue(FileTypeValidator.validateMimeType("pdf", "application/pdf"));
        assertTrue(FileTypeValidator.validateMimeType("jpg", "image/jpeg"));
        assertTrue(FileTypeValidator.validateMimeType("jpeg", "image/jpeg"));
        assertTrue(FileTypeValidator.validateMimeType("png", "image/png"));
        assertTrue(FileTypeValidator.validateMimeType("gif", "image/gif"));
    }

    @Test
    void testValidateMimeType_NullExtension() {
        assertFalse(FileTypeValidator.validateMimeType(null, "application/pdf"));
    }

    @Test
    void testValidateMimeType_NullMimeType() {
        assertFalse(FileTypeValidator.validateMimeType("pdf", null));
    }

    @Test
    void testValidateMimeType_UnknownExtension() {
        // Unknown extension should return true (not configured)
        assertTrue(FileTypeValidator.validateMimeType("xyz", "application/octet-stream"));
    }

    @Test
    void testValidateMimeType_OctetStreamAllowed() {
        // application/octet-stream should be allowed for any type
        assertTrue(FileTypeValidator.validateMimeType("pdf", "application/octet-stream"));
        assertTrue(FileTypeValidator.validateMimeType("jpg", "application/octet-stream"));
    }

    @Test
    void testValidateMimeType_MimeTypeWithParams() {
        // MIME type with parameters (e.g., charset)
        assertTrue(FileTypeValidator.validateMimeType("txt", "text/plain; charset=utf-8"));
    }

    @Test
    void testValidateMimeType_CaseInsensitive() {
        assertTrue(FileTypeValidator.validateMimeType("PDF", "APPLICATION/PDF"));
        assertTrue(FileTypeValidator.validateMimeType("Pdf", "Application/Pdf"));
    }

    @Test
    void testValidateMimeType_InvalidMatch() {
        assertFalse(FileTypeValidator.validateMimeType("pdf", "image/jpeg"));
        assertFalse(FileTypeValidator.validateMimeType("jpg", "application/pdf"));
    }

    @Test
    void testValidationResult_Success() {
        FileTypeValidator.ValidationResult result = FileTypeValidator.ValidationResult.success();
        
        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    void testValidationResult_Fail() {
        FileTypeValidator.ValidationResult result = FileTypeValidator.ValidationResult.fail("Test error");
        
        assertFalse(result.isValid());
        assertEquals("Test error", result.getMessage());
    }

    @Test
    void testValidate_ShortFile() {
        // File shorter than expected header length
        byte[] shortContent = new byte[]{0x25}; // Only 1 byte for PDF
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", shortContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "pdf");
        
        assertFalse(result.isValid());
    }

    @Test
    void testValidate_WebP() {
        // WebP magic number: RIFF (52 49 46 46)
        byte[] webpContent = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", webpContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "webp");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_OFD() {
        // OFD is ZIP-based
        byte[] ofdContent = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.ofd", "application/ofd", ofdContent);
        
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "ofd");
        
        assertTrue(result.isValid());
    }

    @Test
    void testValidate_ShouldHideInternalIoErrorDetails() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("disk error: /private/tmp/upload.bin"));

        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(file, "pdf");

        assertFalse(result.isValid());
        assertEquals("文件验证失败，请稍后重试或联系系统管理员", result.getMessage());
    }
}
