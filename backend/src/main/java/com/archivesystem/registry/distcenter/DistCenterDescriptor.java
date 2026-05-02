package com.archivesystem.registry.distcenter;

/**
 * Dist Center {@code versions/latest.json} 中与版本比对相关的字段。
 */
public record DistCenterDescriptor(String publishedImageTag, String snapshotVersion, String channelAppVersion) {
}
