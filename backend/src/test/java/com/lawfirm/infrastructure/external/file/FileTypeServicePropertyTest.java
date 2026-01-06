package com.lawfirm.infrastructure.external.file;

import net.jqwik.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件类型服务属性测试
 * 
 * Feature: evidence-management
 * Property 1: 文件类型验证
 * Validates: Requirements 1.1, 1.2, 1.8
 * 
 * 测试文件类型识别和验证逻辑的正确性
 */
class FileTypeServicePropertyTest {

    private final FileTypeService fileTypeService = new FileTypeService();

    // 支持的文件扩展名
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> SUPPORTED_DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final Set<String> SUPPORTED_AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "aac");
    private static final Set<String> SUPPORTED_VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv");

    /**
     * Property 1.1: 所有支持的图片扩展名应被识别为 image 类型
     */
    @Property(tries = 100)
    void supportedImageExtensionsShouldBeRecognizedAsImage(
            @ForAll("supportedImageExtensions") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        
        assertThat(info.getType()).isEqualTo("image");
        assertThat(info.isCanPreview()).isTrue();
        assertThat(info.isCanGenerateThumbnail()).isTrue();
        assertThat(fileTypeService.isSupported(fileName)).isTrue();
        assertThat(fileTypeService.isImageFile(fileName)).isTrue();
    }

    /**
     * Property 1.2: 所有支持的文档扩展名应被正确识别
     */
    @Property(tries = 100)
    void supportedDocumentExtensionsShouldBeRecognized(
            @ForAll("supportedDocumentExtensions") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        
        assertThat(info.getType()).isIn("pdf", "word", "excel", "ppt");
        assertThat(info.isCanPreview()).isTrue();
        assertThat(fileTypeService.isSupported(fileName)).isTrue();
        assertThat(fileTypeService.isDocumentFile(fileName)).isTrue();
    }

    /**
     * Property 1.3: 所有支持的音频扩展名应被识别为 audio 类型
     */
    @Property(tries = 100)
    void supportedAudioExtensionsShouldBeRecognizedAsAudio(
            @ForAll("supportedAudioExtensions") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        
        assertThat(info.getType()).isEqualTo("audio");
        assertThat(info.isCanPreview()).isTrue();
        assertThat(fileTypeService.isSupported(fileName)).isTrue();
        assertThat(fileTypeService.isAudioFile(fileName)).isTrue();
    }

    /**
     * Property 1.4: 所有支持的视频扩展名应被识别为 video 类型
     */
    @Property(tries = 100)
    void supportedVideoExtensionsShouldBeRecognizedAsVideo(
            @ForAll("supportedVideoExtensions") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        
        assertThat(info.getType()).isEqualTo("video");
        assertThat(info.isCanPreview()).isTrue();
        assertThat(fileTypeService.isSupported(fileName)).isTrue();
        assertThat(fileTypeService.isVideoFile(fileName)).isTrue();
    }

    /**
     * Property 1.5: 不支持的扩展名应被识别为 other 类型且不支持预览
     */
    @Property(tries = 100)
    void unsupportedExtensionsShouldBeRecognizedAsOther(
            @ForAll("unsupportedExtensions") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        
        assertThat(info.getType()).isEqualTo("other");
        assertThat(info.isCanPreview()).isFalse();
        assertThat(info.isCanGenerateThumbnail()).isFalse();
        assertThat(fileTypeService.isSupported(fileName)).isFalse();
    }

    /**
     * Property 1.6: 文件扩展名识别应不区分大小写
     */
    @Property(tries = 100)
    void fileExtensionRecognitionShouldBeCaseInsensitive(
            @ForAll("supportedExtensionsWithCase") String extension,
            @ForAll("randomFileBaseName") String baseName) {
        String fileName = baseName + "." + extension;
        String lowerFileName = baseName + "." + extension.toLowerCase();
        
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(fileName);
        FileTypeService.FileTypeInfo lowerInfo = fileTypeService.getFileTypeInfo(lowerFileName);
        
        assertThat(info.getType()).isEqualTo(lowerInfo.getType());
        assertThat(info.isCanPreview()).isEqualTo(lowerInfo.isCanPreview());
    }

    /**
     * Property 1.7: 没有扩展名的文件应被识别为 other 类型
     */
    @Property(tries = 100)
    void filesWithoutExtensionShouldBeOther(
            @ForAll("randomFileBaseName") String baseName) {
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(baseName);
        
        assertThat(info.getType()).isEqualTo("other");
        assertThat(info.isCanPreview()).isFalse();
        assertThat(fileTypeService.isSupported(baseName)).isFalse();
    }

    /**
     * Property 1.8: null 文件名应被安全处理
     */
    @Example
    void nullFileNameShouldBeHandledSafely() {
        FileTypeService.FileTypeInfo info = fileTypeService.getFileTypeInfo(null);
        
        assertThat(info.getType()).isEqualTo("other");
        assertThat(info.isCanPreview()).isFalse();
        assertThat(fileTypeService.isSupported(null)).isFalse();
        assertThat(fileTypeService.isImageFile(null)).isFalse();
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> supportedImageExtensions() {
        return Arbitraries.of(SUPPORTED_IMAGE_EXTENSIONS);
    }

    @Provide
    Arbitrary<String> supportedDocumentExtensions() {
        return Arbitraries.of(SUPPORTED_DOCUMENT_EXTENSIONS);
    }

    @Provide
    Arbitrary<String> supportedAudioExtensions() {
        return Arbitraries.of(SUPPORTED_AUDIO_EXTENSIONS);
    }

    @Provide
    Arbitrary<String> supportedVideoExtensions() {
        return Arbitraries.of(SUPPORTED_VIDEO_EXTENSIONS);
    }

    @Provide
    Arbitrary<String> unsupportedExtensions() {
        return Arbitraries.of("exe", "bat", "sh", "dll", "so", "zip", "rar", "7z", "tar", "gz", 
                "txt", "csv", "json", "xml", "html", "css", "js", "java", "py", "rb");
    }

    @Provide
    Arbitrary<String> supportedExtensionsWithCase() {
        Set<String> allSupported = Set.of(
                "jpg", "JPG", "Jpg", "jpeg", "JPEG", "png", "PNG", "gif", "GIF",
                "pdf", "PDF", "doc", "DOC", "docx", "DOCX",
                "mp3", "MP3", "wav", "WAV",
                "mp4", "MP4", "avi", "AVI"
        );
        return Arbitraries.of(allSupported);
    }

    @Provide
    Arbitrary<String> randomFileBaseName() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(s -> s.replaceAll("[^a-zA-Z0-9_-]", "_"));
    }
}
