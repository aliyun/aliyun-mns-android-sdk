package com.alibaba.sdk.android.oss.internal;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.common.OSSConstants;
import com.alibaba.sdk.android.oss.common.auth.Credential;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.alibaba.sdk.android.oss.common.auth.PlainTextAKSKCredential;
import com.alibaba.sdk.android.oss.common.auth.StsTokenCredential;
import com.alibaba.sdk.android.oss.common.utils.DateUtil;
import com.alibaba.sdk.android.oss.common.utils.HttpUtil;
import com.alibaba.sdk.android.oss.common.utils.OSSUtils;

import java.net.URI;

/**
 * Created by zhouzhuo on 11/29/15.
 */
public class ObjectURLPresigner {

    private URI endpoint;
    private Credential credentialProvider;
    private ClientConfiguration configuration;

    public ObjectURLPresigner(URI endpoint, Credential credentialProvider, ClientConfiguration configuration) {
        this.endpoint = endpoint;
        this.credentialProvider = credentialProvider;
        this.configuration = configuration;
    }

    public String presignConstrainedURL(String bucketName, String objectKey, long expiredTimeInSeconds)
            throws ClientException {

        String resource = "/" + bucketName + "/" + objectKey;
        String expires = String.valueOf(DateUtil.getFixedSkewedTimeMillis() / 1000 + expiredTimeInSeconds);
        OSSFederationToken token = null;

        if (credentialProvider instanceof StsTokenCredential) {
            token = ((StsTokenCredential) credentialProvider).getFederationToken();
            resource += "?security-token=" + token.getSecurityToken();
        }

        String contentToSign = "GET\n\n\n" + expires + "\n" + resource;
        String signature = "";

        if (credentialProvider instanceof StsTokenCredential) {
            signature = OSSUtils.sign(token.getTempAK(), token.getTempSK(), contentToSign);
        } else if (credentialProvider instanceof PlainTextAKSKCredential) {
            signature = OSSUtils.sign(((PlainTextAKSKCredential) credentialProvider).getAccessKeyId(),
                    ((PlainTextAKSKCredential) credentialProvider).getAccessKeySecret(), contentToSign);
        } else {
            throw new ClientException("Unknown credentialProvider!");
        }

        String accessKey = signature.split(":")[0].substring(4);
        signature = signature.split(":")[1];

        String host = endpoint.getHost();
        if (!OSSUtils.isCname(host, configuration.getCustomCnameExcludeList())) {
            host = bucketName + "." + host;
        }

        String url = endpoint.getScheme() + "://" + host + "/" + HttpUtil.urlEncode(objectKey, OSSConstants.DEFAULT_CHARSET_NAME)
                + "?OSSAccessKeyId=" + HttpUtil.urlEncode(accessKey, OSSConstants.DEFAULT_CHARSET_NAME)
                + "&Expires=" + expires
                + "&Signature=" + HttpUtil.urlEncode(signature, OSSConstants.DEFAULT_CHARSET_NAME);

        if (credentialProvider instanceof StsTokenCredential) {
            url = url + "&security-token=" + HttpUtil.urlEncode(token.getSecurityToken(), OSSConstants.DEFAULT_CHARSET_NAME);
        }

        return url;
    }

    public String presignPublicURL(String bucketName, String objectKey) {
        String host = endpoint.getHost();
        if (!OSSUtils.isCname(host, configuration.getCustomCnameExcludeList())) {
            host = bucketName + "." + host;
        }
        return endpoint.getScheme() + "://" + host + "/" + HttpUtil.urlEncode(objectKey, OSSConstants.DEFAULT_CHARSET_NAME);
    }
}
