package com.steinsgatezero.vdinoss;

public interface OssCompleteCallback {
    /**
     * @param fileUrl 图片地址
     */
    void onSuccess(String fileUrl);

    /**
     * @param err 错误码
     */
    void onFailure(int err);
}
