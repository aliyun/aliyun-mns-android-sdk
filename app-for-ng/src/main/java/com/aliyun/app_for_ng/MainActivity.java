package com.aliyun.app_for_ng;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.Credential;
import com.alibaba.sdk.android.oss.common.auth.PlainTextAKSKCredential;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.network.OSSAsyncTask;

public class MainActivity extends AppCompatActivity {

    private OSS oss;

    // 运行sample前需要配置以下字段为有效的值
    private static final String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
    private static final String accessKeyId = "LTAI5eIwLe0HAU24";
    private static final String accessKeySecret = "5uo0elIXhKUQucmvB6bN9oqbFotrgx";
    private static final String uploadFilePath = "/sdcard/oss/file1m";

    private static final String testBucket = "md-hangzhou";
    private static final String uploadObject = "sampleObject";
    private static final String downloadObject = "sampleObject";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Credential credential= new PlainTextAKSKCredential(accessKeyId, accessKeySecret);

        ClientConfiguration configuration = new ClientConfiguration.Builder()
                .endPoint(endpoint)
                .connectionTimeout(15 * 1000) // 连接超时，默认15秒
                .socketTimeout(15 * 1000) // socket超时，默认15秒
                .maxErrorRetry(2) // 失败后最大重试次数，默认2次
                .maxConcurrentRequest(5) // 最大并发请求书，默认5个
                .build();

        OSSLog.enableLog();
        oss = new OSSClient(this.getApplicationContext(), credential, configuration);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 构造上传请求
                PutObjectRequest put = new PutObjectRequest.Builder()
                        .putTo(testBucket, "file1m")
                        .fromFilePath(uploadFilePath)
                        .progressCallback(new OSSProgressCallback<PutObjectRequest>() {
                            @Override
                            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
                            }
                        })
                        .build();

                OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                    @Override
                    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                        Log.d("PutObject", "UploadSuccess");
                        Log.d("ETag", result.getETag());
                        Log.d("RequestId", result.getRequestId());
                    }

                    @Override
                    public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                        // 请求异常
                        if (clientExcepion != null) {
                            // 本地异常如网络异常等
                            clientExcepion.printStackTrace();
                        }
                        if (serviceException != null) {
                            // 服务异常
                            Log.e("ErrorCode", serviceException.getErrorCode());
                            Log.e("RequestId", serviceException.getRequestId());
                            Log.e("HostId", serviceException.getHostId());
                            Log.e("RawMessage", serviceException.getRawMessage());
                        }
                    }
                });
            }
        });
    }
}
