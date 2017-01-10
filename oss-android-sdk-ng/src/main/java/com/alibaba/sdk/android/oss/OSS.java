/**
 * Copyright (C) Alibaba Cloud Computing, 2015
 * All rights reserved.
 * 
 * 版权所有 （C）阿里巴巴云计算，2015
 */

package com.alibaba.sdk.android.oss;

import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.network.OSSAsyncTask;

/**
 * 阿里云开放存储服务（Open Storage Service， OSS）的访问接口。
 * <p>
 * 阿里云存储服务（Open Storage Service，简称OSS），是阿里云对外提供的海量，安全，低成本，
 * 高可靠的云存储服务。用户可以通过简单的REST接口，在任何时间、任何地点上传和下载数据，
 * 也可以使用WEB页面对数据进行管理。<br />
 * 基于OSS，用户可以搭建出各种多媒体分享网站、网盘、个人企业数据备份等基于大规模数据的服务。
 * </p>
 *
 * <p>
 * OSS为SDK的接口类，封装了OSS的RESTFul Api接口，考虑到移动端不能在UI线程发起网络请求的编程规范，
 * SDK为所有接口提供了异步的调用形式，也提供了同步接口。
 * </p>
 */
public interface OSS {

    /** * 异步上传文件
     * Put Object用于上传文件。
     *
     * @param request 请求信息
     * @param completedCallback
     * @return
     */
    public OSSAsyncTask<PutObjectResult> asyncPutObject(
            PutObjectRequest request, OSSCompletedCallback<PutObjectRequest, PutObjectResult> completedCallback);
    /**
     * 同步上传文件
     * Put Object用于上传文件。
     *
     * @param request 请求信息
     * @return
     * @throws ClientException
     * @throws ServiceException
     */
    public PutObjectResult putObject(PutObjectRequest request)
            throws ClientException, ServiceException;

    /******************** extension function **********************/

    /**
     * 签名Object的访问URL，以便授权第三方访问
     *
     * @param bucketName 存储Object的Bucket名
     * @param objectKey Object名
     * @param expiredTimeInSeconds URL的有效时长，秒为单位
     * @return
     * @throws ClientException
     */
    public String presignConstrainedObjectURL(String bucketName, String objectKey, long expiredTimeInSeconds)
            throws ClientException;

    /**
     * 签名公开可访问的URL
     *
     * @param bucketName 存储Object的Bucket名
     * @param objectKey  Object名
     * @return
     */
    public String presignPublicObjectURL(String bucketName, String objectKey);
}
