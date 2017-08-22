/**
 * Copyright (C) Alibaba Cloud Computing, 2015
 * All rights reserved.
 * 
 * 版权所有 （C）阿里巴巴云计算，2015
 */

package com.alibaba.sdk.android.common.utils;

import android.text.TextUtils;


public class VersionInfoUtils {

    private static String userAgent = null;
    


    /**
     * 获取系统UA值
     *
     * @return
     */
    public static String getDefaultUserAgent() {
        String result = System.getProperty("http.agent");
        if (TextUtils.isEmpty(result)) {
            result = "(" + System.getProperty("os.name") + "/" + System.getProperty("os.version") + "/" +
                    System.getProperty("os.arch") + ";" + System.getProperty("java.version") + ")";
        }
        return result.replaceAll("[^\\p{ASCII}]", "?");
    }

}
