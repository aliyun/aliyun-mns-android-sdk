package com.alibaba.sdk.android.oss.network;

import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.model.OSSRequest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by zhouzhuo on 03/12/2016.
 */

public class ProgressTouchableRequestBody extends RequestBody {
    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private OSSRequest request;
    private byte[] data;
    private File file;
    private InputStream inputStream;
    private String contentType;
    private long contentLength;
    private OSSProgressCallback callback;
    private BufferedSink bufferedSink;

    public ProgressTouchableRequestBody(OSSRequest request, File file, String contentType, OSSProgressCallback callback) {
        this.request = request;
        this.file = file;
        this.contentType = contentType;
        this.contentLength = file.length();
        this.callback = callback;
    }

    public ProgressTouchableRequestBody(OSSRequest request, byte[] data, String contentType, OSSProgressCallback callback) {
        this.request = request;
        this.data = data;
        this.contentType = contentType;
        this.contentLength = data.length;
        this.callback = callback;
    }

    public ProgressTouchableRequestBody(OSSRequest request, InputStream input, long contentLength, String contentType, OSSProgressCallback callback) {
        this.request = request;
        this.inputStream = input;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.callback = callback;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(this.contentType);
    }

    @Override
    public long contentLength() throws IOException {
        return this.contentLength;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        if (this.file != null) {
            source = Okio.source(this.file);
        } else if (this.data != null) {
            source = Okio.source(new ByteArrayInputStream(this.data));
        } else if (this.inputStream != null) {
            source = Okio.source(this.inputStream);
        }
        long total = 0;
        long read, toRead, remain;

        while (total < contentLength) {
            remain = contentLength - total;
            toRead = Math.min(remain, SEGMENT_SIZE);

            read = source.read(sink.buffer(), toRead);
            if (read == -1) {
                break;
            }

            total += read;
            sink.flush();

            if (callback != null) {
                callback.onProgress(this.request, total, contentLength);
            }
        }
        if (source != null) {
            source.close();
        }
    }
}
