package com.steinsgatezero.vdinoss;

/**
 * 上传进度回调
 */
public interface OssProgressCallback {
    /**
     * @param currentSize 当前上传大小
     * @param totalSize 总大小
     */
    void onProgress(long currentSize, long totalSize);
}
