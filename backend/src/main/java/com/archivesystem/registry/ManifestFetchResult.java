package com.archivesystem.registry;

import lombok.Value;

/**
 * 拉取 Registry manifest 的结果（成功带 digest，失败带可读原因）。
 */
@Value
public class ManifestFetchResult {

    boolean success;
    String digest;
    String failureHint;

    public static ManifestFetchResult ok(String digest) {
        return new ManifestFetchResult(true, digest, null);
    }

    public static ManifestFetchResult fail(String failureHint) {
        return new ManifestFetchResult(false, null, failureHint);
    }
}
