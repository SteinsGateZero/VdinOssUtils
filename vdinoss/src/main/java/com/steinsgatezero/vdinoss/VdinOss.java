package com.steinsgatezero.vdinoss;

import android.app.Activity;
import android.content.Context;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VdinOss {
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endPoint;
    private final String bucketName;
    private boolean isAsync;//默认为异步上传
    private OSS oss;
    private OSSCredentialProvider credentialProvider;
    private Context context;
    private ClientConfiguration configuration;
    private HashMap<String, OSSAsyncTask> taskHashMap;
    public static final int ERR_REQUEST = 0;//上传请求发生异常
    public static final int ERR_NET = 1;//本地异常如网络异常,没有权限等等
    public static final int ERR_SERVICE = 2;//服务异常
    private OssCompleteCallback completeCallback;
    private OssProgressCallback progressCallback;

    public static final class Builder {
        private final Context context;
        private String accessKeyId = "LTAIQWCTRBo7lVY3";
        private String accessKeySecret = "cqzDQqcDkcSm72YkJqvgtb1cIGT7a4";
        private String endPoint = "https://oss-cn-qingdao.aliyuncs.com";
        private String bucketName = "picasso-dev";
        private boolean isAsync = true;//默认为异步上传
        private int connectionTimeout = 15 * 1000;//连接超时时间,默认15s
        private int socketTimeout = 15 * 1000;//socket超时时间
        private int maxConcurrentRequest = 5;//最大并发请求数
        private int maxErrorRetry = 2;//失败后重试次数
        private final OssCompleteCallback completeCallback;
        private OssProgressCallback progressCallback;

        public Builder(@NotNull Context context, @NotNull String accessKeyId, @NotNull String accessKeySecret, @NotNull String endPoint, @NotNull String bucketName, @NotNull OssCompleteCallback completeCallback) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.endPoint = endPoint;
            this.bucketName = bucketName;
            this.context = context;
            this.completeCallback = completeCallback;
        }

        public Builder(@NotNull Context context, @NotNull OssCompleteCallback completeCallback) {
            this.context = context;
            this.completeCallback = completeCallback;
        }

        /**
         * @param isAsync 是否是异步
         * @return Builder
         */
        public Builder isAsync(boolean isAsync) {
            this.isAsync = isAsync;
            return this;
        }

        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder setMaxConcurrentRequest(int maxConcurrentRequest) {
            this.maxConcurrentRequest = maxConcurrentRequest;
            return this;
        }

        public Builder setMaxErrorRetry(int maxErrorRetry) {
            this.maxErrorRetry = maxErrorRetry;
            return this;
        }

        public Builder setProgressCallback(OssProgressCallback progressCallback) {
            this.progressCallback = progressCallback;
            return this;
        }

        public VdinOss build() {
            return new VdinOss(this);
        }
    }

    private VdinOss(Builder builder) {
        this.context = builder.context;
        this.accessKeyId = builder.accessKeyId;
        this.accessKeySecret = builder.accessKeySecret;
        this.endPoint = builder.endPoint;
        this.bucketName = builder.bucketName;
        this.isAsync = builder.isAsync;
        this.completeCallback = builder.completeCallback;
        this.progressCallback = builder.progressCallback;
        credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
        configuration = new ClientConfiguration();
        configuration.setConnectionTimeout(builder.connectionTimeout);
        configuration.setMaxConcurrentRequest(builder.maxConcurrentRequest);
        configuration.setMaxErrorRetry(builder.maxErrorRetry);
        configuration.setSocketTimeout(builder.socketTimeout);
        taskHashMap = new HashMap<>();
        oss = new OSSClient(context.getApplicationContext(), endPoint, credentialProvider, configuration);
    }

    /**
     * @param filePath   文件路径
     * @param objectName oss上的命名(可以制定命名规则)
     * @return 返回预测的假的上传成功的url
     */
    public String upload(@NotNull String filePath, @NotNull final String objectName) {
        PutObjectRequest put = new PutObjectRequest(bucketName, objectName, filePath);
        if (isAsync) {
            if (progressCallback != null) {
                // 异步上传时可以设置进度回调
                put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                    @Override
                    public void onProgress(PutObjectRequest request, final long currentSize, final long totalSize) {

                        if (context != null) {
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressCallback.onProgress(currentSize, totalSize);
                                }
                            });

                        }

                    }

                });
            }
            OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                @Override
                public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                    taskHashMap.remove(request.getObjectKey());
                    if (context != null) {
                        final String fileUrl = oss.presignPublicObjectURL(request.getBucketName(), request.getObjectKey());
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                completeCallback.onSuccess(fileUrl);
                            }
                        });
                    }

                }

                @Override
                public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                    taskHashMap.remove(request.getObjectKey());
                    if (context == null) {
                        return;
                    }
                    int code = ERR_REQUEST;
                    // 请求异常
                    if (clientExcepion != null) {
                        code = ERR_NET;
                        // 本地异常如网络异常,没有权限等等
                        clientExcepion.printStackTrace();
                    }
                    if (serviceException != null) {
                        code = ERR_SERVICE;
                        // 服务异常
                    }
                    final int errCode = code;
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            completeCallback.onFailure(errCode);
                        }
                    });
                }
            });
            if (task != null) {
                taskHashMap.put(objectName, task);
                StringBuilder stringBuilder = new StringBuilder(endPoint);
                stringBuilder.insert(stringBuilder.indexOf("//") + 2, bucketName);
                stringBuilder.insert(stringBuilder.indexOf(bucketName) + bucketName.length(), ".");
                stringBuilder.append("/");
                stringBuilder.append(objectName);
                return stringBuilder.toString();
            }
        } else {
            try {
                PutObjectResult putResult = oss.putObject(put);
                completeCallback.onSuccess(oss.presignPublicObjectURL(bucketName, objectName));
            } catch (ClientException e) {
                // 本地异常如网络异常等
                completeCallback.onFailure(ERR_NET);
                e.printStackTrace();
            } catch (ServiceException e) {
                // 服务异常
                completeCallback.onFailure(ERR_SERVICE);
            }

        }
        return null;
    }

    /**
     * @param filePath 文件路径
     * @return 返回预测的假的上传成功的url
     */
    public String upload(@NotNull String filePath) {
        return this.upload(filePath, getDefaultName(filePath));
    }

    /**
     * @return 默认格式命名
     */
    private String getDefaultName(String filePath) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
        String time = format.format(new Date());
        StringBuilder stringBuilder = new StringBuilder(time);
        StringBuilder stringBuilder2 = new StringBuilder(filePath);
        stringBuilder.append("-");
        stringBuilder.append(UUID.randomUUID().toString());
        int lastIndex = stringBuilder2.lastIndexOf(".");
        if (lastIndex == -1) {
            stringBuilder.append(stringBuilder2.substring(stringBuilder2.lastIndexOf("/"), stringBuilder2.length()));
        } else {
            stringBuilder.append(stringBuilder2.substring(lastIndex, stringBuilder2.length()));
        }
        return stringBuilder.toString();
    }

    /**
     * 关闭上传并销毁
     */
    public void destroy() {
        for (Map.Entry<String, OSSAsyncTask> entry : taskHashMap.entrySet()) {
            if (!entry.getValue().isCompleted()) {
                entry.getValue().cancel();
            }
        }
        taskHashMap.clear();
        taskHashMap = null;
        completeCallback = null;
        progressCallback = null;
        oss = null;
        credentialProvider = null;
        context = null;
    }
}
