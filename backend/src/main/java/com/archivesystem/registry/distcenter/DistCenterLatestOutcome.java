package com.archivesystem.registry.distcenter;

/**
 * 拉取 Dist Center 版本描述文件的结果。
 *
 * @param configured  是否配置了 URL 并已发起拉取（未配置 URL 时为 false）
 * @param success     拉取并解析成功（仅在 configured 为 true 时有意义）
 * @param descriptor  解析成功时的描述；失败时为 null
 * @param errorMessage 失败原因；成功时为 null
 */
public record DistCenterLatestOutcome(boolean configured, boolean success, DistCenterDescriptor descriptor,
                                      String errorMessage) {

    public static DistCenterLatestOutcome notConfigured() {
        return new DistCenterLatestOutcome(false, false, null, null);
    }

    public static DistCenterLatestOutcome ok(DistCenterDescriptor descriptor) {
        return new DistCenterLatestOutcome(true, true, descriptor, null);
    }

    public static DistCenterLatestOutcome fail(String errorMessage) {
        return new DistCenterLatestOutcome(true, false, null, errorMessage);
    }
}
