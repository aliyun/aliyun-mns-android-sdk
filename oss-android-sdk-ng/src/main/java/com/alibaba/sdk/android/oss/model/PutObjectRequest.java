package com.alibaba.sdk.android.oss.model;

/**
 * Created by zhouzhuo on 11/23/15.
 */
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;

import java.util.Map;

public class PutObjectRequest extends OSSRequest {
    
	private String bucketName;
    private String objectKey;
    
    private String uploadFilePath;
	private byte[] uploadData;

    private ObjectMetadata metadata;

	private Map<String, String> callbackParam;
	private Map<String, String> callbackVars;

	private OSSProgressCallback<PutObjectRequest> progressCallback;

	private PutObjectRequest() {}

	public static class Builder {
		private PutObjectRequest instance = new PutObjectRequest();

		/**
		 * @param bucketName 上传到Bucket的名字
		 * @param objectKey 上传到OSS后的ObjectKey
         * @return
         */
		public Builder putTo(String bucketName, String objectKey) {
			this.instance.bucketName = bucketName;
			this.instance.objectKey = objectKey;
			return this;
		}

		/**
		 * 构造上传文件请求
		 * @param filePath 上传文件的本地路径
		 */
		public Builder fromFilePath(String filePath) {
			this.instance.uploadFilePath = filePath;
			return this;
		}

		/**
		 * 上传byte数据到OSS，设置数据内容
		 * @param binaryData
		 */
		public Builder fromByteArray(byte[] binaryData) {
			this.instance.uploadData = binaryData;
			return this;
		}

		/**
		 * 设置上传的文件的元信息
		 * @param metadata 元信息
		 */
		public Builder metadata(ObjectMetadata metadata) {
			this.instance.metadata = metadata;
			return this;
		}

		/**
		 * 设置servercallback参数
		 */
		public Builder callbackParam(Map<String, String> callbackParam) {
			this.instance.callbackParam = callbackParam;
			return this;
		}

		/**
		 * 设置servercallback自定义变量
		 */
		public Builder callbackVars(Map<String, String> callbackVars) {
			this.instance.callbackVars = callbackVars;
			return this;
		}

		/**
		 * 设置上传进度回调
		 * @param progressCallback
		 */
		public Builder progressCallback(OSSProgressCallback<PutObjectRequest> progressCallback) {
			this.instance.progressCallback = progressCallback;
			return this;
		}

		/**
		 * 生成实例
		 * @return
         */
		public PutObjectRequest build() {
			return this.instance;
		}
	}

	/**
	 * 返回请求的BucketName
	 * @return 请求的BucketName
	 */
	public String getBucketName() {
		return bucketName;
	}

	/**
	 * 返回请求的ObjectKey
	 * @return 请求的ObjectKey
	 */
    public String getObjectKey() {
        return objectKey;
    }

    public String getUploadFilePath() {
		return uploadFilePath;
	}

	public byte[] getUploadData() {
		return uploadData;
	}

	public ObjectMetadata getMetadata() {
		return metadata;
	}

	public OSSProgressCallback<PutObjectRequest> getProgressCallback() {
		return progressCallback;
	}

	public Map<String, String> getCallbackParam() {
		return callbackParam;
	}

	public Map<String, String> getCallbackVars() {
		return callbackVars;
	}
}
