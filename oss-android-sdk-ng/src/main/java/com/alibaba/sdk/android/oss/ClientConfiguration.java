/**
 * Copyright (C) Alibaba Cloud Computing, 2015
 * All rights reserved.
 * 
 * 版权所有 （C）阿里巴巴云计算，2015
 */

package com.alibaba.sdk.android.oss;

import com.alibaba.sdk.android.oss.common.utils.VersionInfoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 访问阿里云服务的客户端配置。
 */
public class ClientConfiguration {

    private static final String DEFAULT_USER_AGENT = VersionInfoUtils.getDefaultUserAgent();
    private static final int DEFAULT_MAX_RETRIES = 2;

    private String endPoint;
    private int maxConcurrentRequest = 5;
    private int socketTimeout = 15 * 1000;
    private int connectionTimeout = 15 * 1000;
    private int maxErrorRetry = DEFAULT_MAX_RETRIES;
    private List<String> customCnameExcludeList = new ArrayList<String>();
    private String proxyHost;
    private int proxyPort;

    private ClientConfiguration() {}

    public static class Builder {
        private ClientConfiguration instance = new ClientConfiguration();

        /**
         * @param endPoint OSS访问域名，参考http://help.aliyun.com/document_detail/oss/user_guide/endpoint_region.html
         * @return
         */
        public Builder endPoint(String endPoint) {
            this.instance.endPoint = endPoint;
            return this;
        }

        /**
         * 设置允许并发的最大HTTP请求数
         * @param maxConcurrentRequest
         *          最大HTTP并发请求数
         */
        public Builder maxConcurrentRequest(int maxConcurrentRequest) {
            this.instance.maxConcurrentRequest = maxConcurrentRequest;
            return this;
        }

        /**
         * 设置通过打开的连接传输数据的超时时间（单位：毫秒）。
         * 0表示无限等待（但不推荐使用）。
         * @param socketTimeout
         *          通过打开的连接传输数据的超时时间（单位：毫秒）。
         */
        public Builder socketTimeout(int socketTimeout) {
            this.instance.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * 设置建立连接的超时时间（单位：毫秒）。
         * @param connectionTimeout
         *          建立连接的超时时间（单位：毫秒）。
         */
        public Builder connectionTimeout(int connectionTimeout) {
            this.instance.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * 设置一个值表示当可重试的请求失败后最大的重试次数。（默认值为2）
         * @param maxErrorRetry
         *          当可重试的请求失败后最大的重试次数。
         */
        public Builder maxErrorRetry(int maxErrorRetry) {
            this.instance.maxErrorRetry = maxErrorRetry;
            return this;
        }

        /**
         * 设置CNAME排除列表。
         * @param customCnameExcludeList CNAME排除列表。
         */
        public Builder customCnameExcludeList(List<String> customCnameExcludeList) {
            if (customCnameExcludeList == null) {
                throw new IllegalArgumentException("cname exclude list should not be null.");
            }

            this.instance.customCnameExcludeList.clear();
            for (String host : customCnameExcludeList) {
                if (host.contains("://")) {
                    this.instance.customCnameExcludeList.add(host.substring(host.indexOf("://") + 3));
                } else {
                    this.instance.customCnameExcludeList.add(host);
                }
            }
            return this;
        }

        /**
         * 设置HTTP代理
         */
        public Builder proxy(String proxyHost, int proxyPort) {
            this.instance.proxyHost = proxyHost;
            this.instance.proxyPort = proxyPort;
            return this;
        }

        /**
         * 生成实例
         */
        public ClientConfiguration build() {
            if (this.instance.endPoint == null) {
                throw new IllegalArgumentException("An correct endpoint need to be set to configuration.");
            }
            return this.instance;
        }
    }

    /**
     * 返回设置的EndPoint
     */
    public String getEndPoint() {
        return endPoint;
    }

    /**
     * 返回最大的并发HTTP请求数
     * @return
     */
    public int getMaxConcurrentRequest() {
        return maxConcurrentRequest;
    }

    /**
     * 返回通过打开的连接传输数据的超时时间（单位：毫秒）。
     * 0表示无限等待（但不推荐使用）。
     * @return 通过打开的连接传输数据的超时时间（单位：毫秒）。
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * 返回建立连接的超时时间（单位：毫秒）。
     * @return 建立连接的超时时间（单位：毫秒）。
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 返回一个值表示当可重试的请求失败后最大的重试次数。（默认值为2）
     * @return 当可重试的请求失败后最大的重试次数。
     */
    public int getMaxErrorRetry() {
        return maxErrorRetry;
    }

    /**
     * 获取CNAME排除列表（不可修改），以列表元素作为后缀的域名将不进行CNAME解析。
     * @return CNAME排除列表。
     */
    public List<String> getCustomCnameExcludeList() {
        return Collections.unmodifiableList(this.customCnameExcludeList);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }
}
