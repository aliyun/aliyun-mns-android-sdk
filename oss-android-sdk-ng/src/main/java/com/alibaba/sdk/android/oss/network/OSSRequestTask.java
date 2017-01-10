package com.alibaba.sdk.android.oss.network;

import android.os.Handler;
import android.os.Looper;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.common.OSSHeaders;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.utils.DateUtil;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;
import com.alibaba.sdk.android.oss.internal.OSSRetryHandler;
import com.alibaba.sdk.android.oss.internal.OSSRetryType;
import com.alibaba.sdk.android.oss.internal.RequestMessage;
import com.alibaba.sdk.android.oss.internal.ResponseParser;
import com.alibaba.sdk.android.oss.internal.ResponseParsers;
import com.alibaba.sdk.android.oss.model.OSSRequest;
import com.alibaba.sdk.android.oss.model.OSSResult;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by zhouzhuo on 01/12/2016.
 */
public class OSSRequestTask<T extends OSSResult> {

    private ResponseParser<T> responseParser;
    private OSSRequest request;
    private RequestMessage message;
    private OkHttpClient client;
    private OSSRetryHandler retryHandler;
    private int currentRetryCount = 0;

    private OSSAsyncTask asyncTask = new OSSAsyncTask();

    public OSSRequestTask(OSSRequest request, RequestMessage message, ResponseParser parser, OkHttpClient client, int maxRetry) {
        this.request = request;
        this.responseParser = parser;
        this.message = message;
        this.client = client;
        this.retryHandler = new OSSRetryHandler(maxRetry);
    }

    // 从服务端响应的Header取出"Date"头，作为标准时间
    private void updateStandardTimeStampWithServerResponse(Response resp) {
        if (resp != null) {
            String responseDateString = resp.header(OSSHeaders.DATE);
            try {
                // 每次请求回来都更新一下标准时间值
                long serverTime = DateUtil.parseRfc822Date(responseDateString).getTime();
                DateUtil.setCurrentServerTime(serverTime);
            } catch (Exception ignore) {
                // 解析时间失败，不做任何事情
            }
        }
    }

    // 组装Okhttp的请求头
    private Request assembleRequest() throws IOException {
        OSSLog.logD("[assembleRequest] - ");

        // validate request
        // OSSUtils.ensureRequestValid(asyncTask.getRequest(), message); should validate requestmessage

        // signing
        OSSUtils.signRequest(message);

        Request.Builder requestBuilder = new Request.Builder();

        // build request url
        String url = message.buildCanonicalURL();
        requestBuilder = requestBuilder.url(url);

        // set request headers
        for (String key : message.getHeaders().keySet()) {
            requestBuilder = requestBuilder.addHeader(key, message.getHeaders().get(key));
        }

        String contentType = message.getHeaders().get(OSSHeaders.CONTENT_TYPE);

        // set request body
        switch (message.getMethod()) {
            case POST:
            case PUT:
                OSSUtils.assertTrue(contentType != null, "Content type can't be null when upload!");

                if (message.getUploadData() != null) {
                    requestBuilder = requestBuilder.method(message.getMethod().toString(),
                            new ProgressTouchableRequestBody(this.request, message.getUploadData(), contentType,
                                    message.getProgressCallback()));
                } else if (message.getUploadFilePath() != null) {
                    requestBuilder = requestBuilder.method(message.getMethod().toString(),
                            new ProgressTouchableRequestBody(this.request, new File(message.getUploadFilePath()), contentType,
                                    message.getProgressCallback()));
                } else if (message.getUploadInputStream() != null) {
                    requestBuilder = requestBuilder.method(message.getMethod().toString(),
                            new ProgressTouchableRequestBody(this.request, message.getUploadInputStream(),
                                    message.getReadStreamLength(), contentType,
                                    message.getProgressCallback()));
                } else {
                    requestBuilder = requestBuilder.method(message.getMethod().toString(), RequestBody.create(null, new byte[0]));
                }
                break;
            case GET:
                requestBuilder = requestBuilder.get();
                break;
            case HEAD:
                requestBuilder = requestBuilder.head();
                break;
            case DELETE:
                requestBuilder = requestBuilder.delete();
                break;
            default:
                break;
        }

        Request request = requestBuilder.build();

        if (OSSLog.isEnableLog()) {
            OSSLog.logD("request url: " + request.url());
            Map<String, List<String>> headerMap = request.headers().toMultimap();
            for (String key : headerMap.keySet()) {
                OSSLog.logD("requestHeader " + key + ": " + headerMap.get(key).get(0));
            }
        }
        return request;
    }

    // 检查是否需要重试
    private OSSRetryType shouldRetry(Exception e) {
        if (currentRetryCount >= 3) {
            return OSSRetryType.OSSRetryTypeShouldNotRetry;
        }

        if (e instanceof ClientException) {
            if (((ClientException) e).isCanceledException()) {
                return OSSRetryType.OSSRetryTypeShouldNotRetry;
            }

            Exception localException = (Exception) e.getCause();
            if (localException instanceof InterruptedIOException
                    && !(localException instanceof SocketTimeoutException)) {
                OSSLog.logE("[shouldRetry] - is interrupted!");
                return OSSRetryType.OSSRetryTypeShouldNotRetry;
            } else if (localException instanceof IllegalArgumentException) {
                return OSSRetryType.OSSRetryTypeShouldNotRetry;
            }
            OSSLog.logD("shouldRetry - " + e.toString());
            e.getCause().printStackTrace();
            return OSSRetryType.OSSRetryTypeShouldRetry;
        } else if (e instanceof ServiceException) {
            ServiceException serviceException = (ServiceException) e;
            if (serviceException.getErrorCode() != null && serviceException.getErrorCode().equalsIgnoreCase("RequestTimeTooSkewed")) {
                return OSSRetryType.OSSRetryTypeShouldFixedTimeSkewedAndRetry;
            } else if (serviceException.getStatusCode() >= 500){
                return OSSRetryType.OSSRetryTypeShouldRetry;
            } else {
                return OSSRetryType.OSSRetryTypeShouldNotRetry;
            }
        } else {
            return OSSRetryType.OSSRetryTypeShouldNotRetry;
        }
    }

    // 为了支持同步调用，okhttp异步回调的最后都要调用一下这个方法通知完成并传递结果
    private void finishTask(OSSResult result, Exception exception) {
        if (result != null) {
            synchronized (this.asyncTask.getLock()) {
                this.asyncTask.getIsCompleted().set(true);
                this.asyncTask.setResult(result);
                this.asyncTask.getLock().notify();
            }
        } else {
            synchronized (OSSRequestTask.this.asyncTask.getLock()) {
                this.asyncTask.getIsCompleted().set(true);
                this.asyncTask.setException(exception);
                this.asyncTask.getLock().notify();
            }
        }
    }

    // 发起请求
    public OSSAsyncTask doRequest() {
        Response response = null;
        final Exception exception = null;
        Call call = null;

        try {
            call = client.newCall(this.assembleRequest());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // send request
        call.enqueue(new Callback() {
            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                OSSLog.logD("request errro: " + e.toString());

                final ClientException clientException = new ClientException(e);
                if (message.getCompletedCallback() != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                message.getCompletedCallback().onFailure(OSSRequestTask.this.request, clientException, null);
                            } catch (Exception ignore) {
                                // 用户在回调里抛出的异常，不予理会
                            }
                        }
                    });
                    OSSRequestTask.this.finishTask(null, exception);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (OSSLog.isEnableLog()) {
                    OSSLog.logD("response code: " + response.code() + " for url: " + call.request().url());
                    Map<String, List<String>> headerMap = response.headers().toMultimap();
                    for (String key : headerMap.keySet()) {
                        OSSLog.logD("responseHeader " + key + ": " + headerMap.get(key).get(0));
                    }
                }

                OSSRequestTask.this.updateStandardTimeStampWithServerResponse(response);

                if (response.code() == 203 || response.code() >= 300) {
                    final ServiceException serviceException = ResponseParsers.parseResponseErrorXML(response, call.request().method().equals("HEAD"));
                    if (serviceException != null && message.getCompletedCallback() != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    message.getCompletedCallback().onFailure(OSSRequestTask.this.request, null, serviceException);
                                } catch (Exception ignore) {
                                    // 用户在回调里抛出的异常，不予理会
                                }
                            }
                        });
                        OSSRequestTask.this.finishTask(null, exception);
                    }
                }

                final T result = responseParser.parse(response);
                if (message.getCompletedCallback() != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                message.getCompletedCallback().onSuccess(OSSRequestTask.this.request, result);
                            } catch (Exception ignore) {
                                // 用户在回调里抛出的异常，不予理会
                            }
                        }
                    });
                    OSSRequestTask.this.finishTask(result, null);
                }
            }
        });
        this.asyncTask.setCall(call);
        return this.asyncTask;
    }
}
