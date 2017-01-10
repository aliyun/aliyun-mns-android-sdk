/**
 * Copyright (C) Alibaba Cloud Computing, 2015
 * All rights reserved.
 * 
 * 版权所有 （C）阿里巴巴云计算，2015
 */

package com.alibaba.sdk.android.oss;

import android.content.Context;

import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.auth.Credential;
import com.alibaba.sdk.android.oss.internal.InternalRequestOperation;
import com.alibaba.sdk.android.oss.internal.ObjectURLPresigner;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.network.OSSAsyncTask;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 访问阿里云开放存储服务（Open Storage Service， OSS）的入口类。
 */
public class OSSClient implements OSS {

    private URI endpointURI;
    private Credential credential;
    private ClientConfiguration configuration;
    private InternalRequestOperation internalRequestOperation;

    /**
     * 构造一个OSSClient实例
     *
     * @param context android应用的applicationContext
     * @param credential 鉴权设置
     * @param configuration 网络参数设置
     */
    public OSSClient(Context context, Credential credential, ClientConfiguration configuration) {
        try {
            String endpoint = configuration.getEndPoint();
            if (!endpoint.startsWith("http")) {
                endpoint = "http://" + endpoint;
            }
            this.endpointURI = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Endpoint must be a string like 'http://oss-cn-****.aliyuncs.com'," +
                    "or your cname like 'http://image.cnamedomain.com'!");
        }
        if (credential == null) {
            throw new IllegalArgumentException("CredentialProvider can't be null.");
        }
        this.credential = credential;
        this.configuration = configuration;
        this.internalRequestOperation = new InternalRequestOperation(context, endpointURI, credential, this.configuration);
    }

    @Override
    public OSSAsyncTask<PutObjectResult> asyncPutObject(
            PutObjectRequest request, OSSCompletedCallback<PutObjectRequest, PutObjectResult> completedCallback) {

        return internalRequestOperation.putObject(request, completedCallback);
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest request)
            throws ClientException, ServiceException {

        return internalRequestOperation.putObject(request, null).getResult();
    }

    @Override
    public String presignConstrainedObjectURL(String bucketName, String objectKey, long expiredTimeInSeconds)
            throws ClientException {

        return new ObjectURLPresigner(this.endpointURI, this.credential, this.configuration).presignConstrainedURL(bucketName, objectKey, expiredTimeInSeconds);
    }

    @Override
    public String presignPublicObjectURL(String bucketName, String objectKey) {

        return new ObjectURLPresigner(this.endpointURI, this.credential, this.configuration).presignPublicURL(bucketName, objectKey);
    }
}
