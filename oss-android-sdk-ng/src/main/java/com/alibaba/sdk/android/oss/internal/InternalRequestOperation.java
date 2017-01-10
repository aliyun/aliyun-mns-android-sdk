package com.alibaba.sdk.android.oss.internal;

import android.content.Context;
import android.os.Build;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.HttpMethod;
import com.alibaba.sdk.android.oss.common.OSSConstants;
import com.alibaba.sdk.android.oss.common.OSSHeaders;
import com.alibaba.sdk.android.oss.common.auth.Credential;
import com.alibaba.sdk.android.oss.common.utils.DateUtil;
import com.alibaba.sdk.android.oss.common.utils.HttpHeaders;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;
import com.alibaba.sdk.android.oss.common.utils.VersionInfoUtils;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.network.OSSAsyncTask;
import com.alibaba.sdk.android.oss.network.OSSRequestTask;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

/**
 * Created by zhouzhuo on 11/22/15.
 */
public class InternalRequestOperation {

    private volatile URI endpoint;
    private OkHttpClient innerClient;
    private Context applicationContext;
    private Credential credential;
    private int maxRetryCount = OSSConstants.DEFAULT_RETRY_COUNT;
    private ClientConfiguration configuration;

    private static ExecutorService executorService = Executors.newFixedThreadPool(OSSConstants.DEFAULT_BASE_THREAD_POOL_SIZE);

    private InternalRequestOperation() {}

    public InternalRequestOperation(Context context, final URI endpoint, Credential credential, ClientConfiguration configuration) {
        this.applicationContext = context;
        this.endpoint = endpoint;
        this.credential = credential;
        this.configuration = configuration;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .cache(null)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(endpoint.getHost(), session);
                    }
                });

        if (configuration != null) {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(configuration.getMaxConcurrentRequest());

            builder.connectTimeout(configuration.getConnectionTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(configuration.getSocketTimeout(), TimeUnit.MILLISECONDS)
                    .writeTimeout(configuration.getSocketTimeout(), TimeUnit.MILLISECONDS)
                    .dispatcher(dispatcher);

            if (configuration.getProxyHost() != null && configuration.getProxyPort() != 0) {
                builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(configuration.getProxyHost(), configuration.getProxyPort())));
            }

            this.maxRetryCount = configuration.getMaxErrorRetry();
        }

        this.innerClient = builder.build();
    }

    public OSSAsyncTask<PutObjectResult> putObject(
            PutObjectRequest request, OSSCompletedCallback<PutObjectRequest, PutObjectResult> completedCallback) {

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setEndpoint(endpoint);
        requestMessage.setMethod(HttpMethod.PUT);
        requestMessage.setBucketName(request.getBucketName());
        requestMessage.setObjectKey(request.getObjectKey());
        if (request.getUploadData() != null) {
            requestMessage.setUploadData(request.getUploadData());
        }
        if (request.getUploadFilePath() != null) {
            requestMessage.setUploadFilePath(request.getUploadFilePath());
        }
        if (request.getCallbackParam() != null) {
            requestMessage.getHeaders().put("x-oss-callback", OSSUtils.populateMapToBase64JsonString(request.getCallbackParam()));
        }
        if (request.getCallbackVars() != null) {
            requestMessage.getHeaders().put("x-oss-callback-var", OSSUtils.populateMapToBase64JsonString(request.getCallbackVars()));
        }

        OSSUtils.populateRequestMetadata(requestMessage.getHeaders(), request.getMetadata());

        canonicalizeRequestMessage(requestMessage);

        if (completedCallback != null) {
            requestMessage.setCompletedCallback(completedCallback);
        }
        requestMessage.setProgressCallback(request.getProgressCallback());
        ResponseParser<PutObjectResult> parser = new ResponseParsers.PutObjectReponseParser();

        OSSAsyncTask asyncTask = new OSSRequestTask<PutObjectResult>(request, requestMessage, parser, this.innerClient, maxRetryCount).doRequest();

        return asyncTask;
    }

    private boolean checkIfHttpdnsAwailable() {
        if (applicationContext == null) {
            return false;
        }

        boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

        String proxyHost;

        if (IS_ICS_OR_LATER) {
            proxyHost = System.getProperty("http.proxyHost");
        } else {
            proxyHost = android.net.Proxy.getHost(applicationContext);
        }
        return proxyHost == null;
    }

    public OkHttpClient getInnerClient() {
        return innerClient;
    }

    private void canonicalizeRequestMessage(RequestMessage message) {
        Map<String, String> header = message.getHeaders();

        // 如果用户没有显式设置 Date 头，用SDK记录的标准时间设置一个
        if (header.get(OSSHeaders.DATE) == null) {
            header.put(OSSHeaders.DATE, DateUtil.currentFixedSkewedTimeInRFC822Format());
        }

        // 如果上传未设置 Content-Type ，根据本地文件路径和上传路径决定一个默认的
        if (message.getMethod() == HttpMethod.POST || message.getMethod() == HttpMethod.PUT) {
            if (header.get(OSSHeaders.CONTENT_TYPE) == null) {
                String determineContentType = OSSUtils.determineContentType(null,
                        message.getUploadFilePath(), message.getObjectKey());
                header.put(OSSHeaders.CONTENT_TYPE, determineContentType);
            }
        }

        // 设置了代理的情况下，不开启httpdns
        message.setIsHttpdnsEnable(checkIfHttpdnsAwailable());

        // 设置 User-Agent 头
        message.getHeaders().put(HttpHeaders.USER_AGENT, VersionInfoUtils.getUserAgent());

        // 传递凭证和相关设置
        message.setCredential(credential);
        message.setConfiguration(configuration);
    }
}
