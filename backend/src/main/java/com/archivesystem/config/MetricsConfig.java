package com.archivesystem.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义业务监控指标配置
 * 
 * 关键指标：
 * - archive_receive_total: 档案接收总数
 * - archive_receive_success: 档案接收成功数
 * - archive_receive_failed: 档案接收失败数
 * - file_upload_total: 文件上传总数
 * - file_upload_bytes_total: 文件上传总字节数
 * - borrow_request_total: 借阅申请总数
 * - search_request_total: 检索请求总数
 */
@Component
@Getter
public class MetricsConfig {

    private final Counter archiveReceiveTotal;
    private final Counter archiveReceiveSuccess;
    private final Counter archiveReceiveFailed;
    private final Counter fileUploadTotal;
    private final Counter fileUploadBytesTotal;
    private final Counter borrowRequestTotal;
    private final Counter searchRequestTotal;
    private final Timer archiveReceiveTimer;
    private final Timer fileDownloadTimer;
    
    // 当前活跃处理数
    private final AtomicLong activeArchiveProcessing = new AtomicLong(0);
    
    public MetricsConfig(MeterRegistry registry) {
        // 档案接收计数器
        this.archiveReceiveTotal = Counter.builder("archive.receive.total")
                .description("档案接收请求总数")
                .register(registry);
        
        this.archiveReceiveSuccess = Counter.builder("archive.receive.success")
                .description("档案接收成功数")
                .register(registry);
        
        this.archiveReceiveFailed = Counter.builder("archive.receive.failed")
                .description("档案接收失败数")
                .register(registry);
        
        // 文件上传计数器
        this.fileUploadTotal = Counter.builder("file.upload.total")
                .description("文件上传总数")
                .register(registry);
        
        this.fileUploadBytesTotal = Counter.builder("file.upload.bytes.total")
                .description("文件上传总字节数")
                .baseUnit("bytes")
                .register(registry);
        
        // 借阅请求计数器
        this.borrowRequestTotal = Counter.builder("borrow.request.total")
                .description("借阅申请总数")
                .register(registry);
        
        // 检索请求计数器
        this.searchRequestTotal = Counter.builder("search.request.total")
                .description("检索请求总数")
                .register(registry);
        
        // 档案接收耗时
        this.archiveReceiveTimer = Timer.builder("archive.receive.duration")
                .description("档案接收处理耗时")
                .register(registry);
        
        // 文件下载耗时
        this.fileDownloadTimer = Timer.builder("file.download.duration")
                .description("文件下载耗时")
                .register(registry);
        
        // 当前活跃处理数（Gauge）
        Gauge.builder("archive.processing.active", activeArchiveProcessing, AtomicLong::get)
                .description("当前正在处理的档案数")
                .register(registry);
    }
    
    /**
     * 记录档案接收开始
     */
    public void recordArchiveReceiveStart() {
        archiveReceiveTotal.increment();
        activeArchiveProcessing.incrementAndGet();
    }
    
    /**
     * 记录档案接收成功
     */
    public void recordArchiveReceiveSuccess() {
        archiveReceiveSuccess.increment();
        activeArchiveProcessing.decrementAndGet();
    }
    
    /**
     * 记录档案接收失败
     */
    public void recordArchiveReceiveFailed() {
        archiveReceiveFailed.increment();
        activeArchiveProcessing.decrementAndGet();
    }
    
    /**
     * 记录文件上传
     */
    public void recordFileUpload(long bytes) {
        fileUploadTotal.increment();
        fileUploadBytesTotal.increment(bytes);
    }
    
    /**
     * 记录借阅请求
     */
    public void recordBorrowRequest() {
        borrowRequestTotal.increment();
    }
    
    /**
     * 记录检索请求
     */
    public void recordSearchRequest() {
        searchRequestTotal.increment();
    }
}
