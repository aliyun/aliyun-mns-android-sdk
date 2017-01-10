package com.alibaba.sdk.android.oss.network;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.model.OSSResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;

/**
 * Created by zhouzhuo on 02/12/2016.
 */
public class OSSAsyncTask<T extends OSSResult> {

    private Object lock;
    private AtomicBoolean isCompleted;

    private Call call;
    private T result;
    private Exception exception;

    protected OSSAsyncTask() {
        this.lock = new Object();
        this.isCompleted = new AtomicBoolean(false);
    }

    protected Object getLock() {
        return lock;
    }

    protected AtomicBoolean getIsCompleted() {
        return isCompleted;
    }

    protected void setCall(Call call) {
        this.call = call;
    }

    protected void setResult(T result) {
        this.result = result;
    }

    protected void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * 取消任务
     */
    public void cancel() {
        call.cancel();
    }

    /**
     * 检查任务是否已经完成
     *
     * @return
     */
    public boolean isCompleted() {
        return this.isCompleted.get();
    }

    /**
     * 阻塞等待任务完成，并获取结果
     *
     * @return
     * @throws ClientException
     * @throws ServiceException
     */
    public T getResult() throws ClientException, ServiceException {
        try {
            this.waitUntilFinished();

            if (this.result != null) {
                return this.result;
            } else if (this.exception != null) {
                throw this.exception;
            } else {
                throw new IOException("invalid state");
            }
        } catch (InterruptedException e) {
            throw new ClientException(e.getMessage(), e);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClientException) {
                throw (ClientException) cause;
            } else if (cause instanceof ServiceException) {
                throw (ServiceException) cause;
            } else {
                cause.printStackTrace();
                throw new ClientException("Unexpected exception!" + cause.getMessage());
            }
        }
    }

    /**
     * 阻塞等待任务完成
     */
    public void waitUntilFinished() throws InterruptedException {
        synchronized (this.lock) {
            while (!this.isCompleted.get()) {
                this.lock.wait();
            }
        }
    }

    /**
     * 任务是否已经被取消过
     */
    public boolean isCanceled() {
        return call.isCanceled();
    }
}
