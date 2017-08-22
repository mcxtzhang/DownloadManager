package com.mcxtzhang.downloadmanager.download;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.mcxtzhang.downloadmanager.db.DownloadDbFactory;
import com.mcxtzhang.downloadmanager.db.IDownloadDbManager;
import com.mcxtzhang.downloadmanager.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Intro: 下载管理类
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public enum ZDownloadManager implements IDownloadManager {
    INSTANCE;

    private OkHttpClient mClient;//OKHttpClient;
    private IDownloadDbManager mDbManager;

    private int maxRequests;
    //正在下载的集合（url-Call)
    private Map<String, Call> mDownloadingCallMap;
    //被挂起的集合
    private List<String> mPendingCall;

    //回调相关
    private Handler mMainHandler;
    //url-listener
    private Map<String, DownloadListener> mDownloadListener;

    ZDownloadManager() {
        //mPendingCall = new LinkedHashMap<>(5, 0.75f, true);
        mPendingCall = new LinkedList<>();
        mDownloadingCallMap = new ArrayMap<>();
        mDbManager = DownloadDbFactory.getDownloadDbmanager();

        mMainHandler = new Handler(Looper.getMainLooper());
        mDownloadListener = new ArrayMap<>();
    }

    @Override
    public synchronized void download(String url) {
        download(url, null);
    }

    @Override
    public synchronized void download(final String url, DownloadListener downloadListener) {
        LogUtils.d("download() called with: url = [" + url + "], downloadListener = [" + downloadListener + "]");
        if (TextUtils.isEmpty(url)) return;
        //监听器加入集合
        registerDownloadListener(url, downloadListener);
        //不重复发起下载任务
        if (isDownloading(url)) return;

        //先查数据库里之前有没有该url的下载记录
        DownloadBean downloadBean = mDbManager.selectDownloadBean(url);
        //没有的话 插入一条新纪录
        if (downloadBean == null) {
            downloadBean = new DownloadBean(url);
            mDbManager.insertDownloadBean(downloadBean);
        } else if (downloadBean.isFinished()) {
            //下载完成
            notifyDownloadComplete(url);
            return;
        }

        Call call = getCall(url, downloadBean);
        //超过最大并发数
        if (reachMaxRequests()) {
            //挂起
            pendingCall(url);
        } else {
            doCall(url, call);
        }

        //notifyDownloadProgress(url, downloadBean.getBegin(), downloadBean.getTotalLength());
    }

    private boolean reachMaxRequests() {
        return mDownloadingCallMap.size() >= maxRequests;
    }

    @Override
    public synchronized void registerDownloadListener(String url, DownloadListener downloadListener) {
        if (TextUtils.isEmpty(url) || downloadListener == null) return;
        mDownloadListener.put(url, downloadListener);
    }

    @Override
    public synchronized void unregisterAllListener() {
        mDownloadListener.clear();
    }

    @Override
    public synchronized void unregisterDownloadListener(List<String> urls) {
        if (null == urls || urls.isEmpty()) return;
        for (String url : urls) {
            unregisterDownloadListener(url);
        }
    }


    @Override
    public synchronized void unregisterDownloadListener(String url) {
        if (TextUtils.isEmpty(url)) return;
        mDownloadListener.remove(url);
    }

    @Override
    public synchronized void stop(String url) {
        if (TextUtils.isEmpty(url)) return;
        Call call = mDownloadingCallMap.get(url);
        DownloadBean downloadBean = mDbManager.selectDownloadBean(url);
        if (null != call) {
            LogUtils.d("stop,停止执行任务 = [" + url + "]");
            call.cancel();
            mDownloadingCallMap.remove(url);
        }
        notifyDownloadPause(url, downloadBean != null ? downloadBean.getBegin() : 0);
    }

    @Override
    public List<DownloadBean> selectAllDownloadBean() {
        return mDbManager.selectAllDownloadBean();
    }

    @Override
    public synchronized int selectStatus(String url) {
        if (isDownloading(url)) {
            return STATUS_DOWNLOADING;
        } else if (isPending(url)) {
            return STATUS_PENDING;
        } else {
            DownloadBean downloadBean = mDbManager.selectDownloadBean(url);
            if (downloadBean == null || !downloadBean.isFinished()) {
                return STATUS_UNFINISHED;
            } else {
                return STATUS_FINISHED;
            }
        }
    }

    @Override
    public DownloadBean selectDownloadBean(String url) {
        return mDbManager.selectDownloadBean(url);
    }

    @Override
    public synchronized void deleteFile(DownloadBean bean) {
        deleteFile(bean.getUrl());
    }

    @Override
    public void deleteFile(String url) {
        // TODO: 2017/8/22 子线程操作 删除文件
        mDbManager.deleteDownloadBean(url);
    }

    private Call getCall(String url, DownloadBean downloadBean) {
        Request request = new Request.Builder()
                .header("RANGE", "bytes=" + downloadBean.getBegin() + "-")
                .url(url)
                .build();
        return getClient().newCall(request);
    }

    private void pendingCall(String url) {
        if (isDownloading(url)) return;
        if (mPendingCall.indexOf(url) > -1) {
            mPendingCall.remove(url);
        }
        mPendingCall.add(0, url);
        notifyDownloadPending(url);
    }

    //执行下载任务
    private void doCall(final String url, Call call) {
        //正在下载任务集合
        mDownloadingCallMap.put(url, call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (!"Canceled".equals(e.getMessage())) {
                    notifyDownloadError(url, e);
                }
                onEnd(url);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                saveFile(response, url);
                onEnd(url);
            }
        });
    }

    private void onEnd(String url) {
        //从运行中任务集合删除该url
        mDownloadingCallMap.remove(url);
        //从pending集合中取出一个执行
        promoteCalls();
    }

    private void promoteCalls() {
        if (reachMaxRequests()) return;
        if (mPendingCall.isEmpty()) return;
        Iterator<String> iterator = mPendingCall.iterator();
        String url;
        //逆序遍历 取出最新加入的请求 先执行
        while (iterator.hasNext()) {
            url = iterator.next();
            iterator.remove();//从pending删除
            if (isDownloading(url)) continue;
            doCall(url, getCall(url, mDbManager.selectDownloadBean(url)));//加入downloading
            if (reachMaxRequests()) return;
        }
    }

    //下载文件 同时 将进度存入数据库， 回调通知监听者
    private void saveFile(Response response, String url) {
        final DownloadBean downloadBean = mDbManager.selectDownloadBean(url);
        if (downloadBean == null) return;
        ResponseBody body = response.body();
        InputStream in = body.byteStream();
        FileChannel channelOut = null;
        // 随机访问文件，可以指定断点续传的起始位置
        RandomAccessFile randomAccessFile = null;
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadBean.getFileName());
        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");
            //Chanel NIO中的用法，由于RandomAccessFile没有使用缓存策略，直接使用会使得下载速度变慢，亲测缓存下载3.3秒的文件，用普通的RandomAccessFile需要20多秒。
            channelOut = randomAccessFile.getChannel();
            // 内存映射，直接使用RandomAccessFile，是用其seek方法指定下载的起始位置，使用缓存下载，在这里指定下载位置。
            MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE, downloadBean.getBegin(), body.contentLength());
            //
            downloadBean.setTotalLength(file.length());
            int bufferLength = Math.max(1024 * 500, (int) (file.length() / 20));
            byte[] buffer = new byte[bufferLength];
            int len;
            while ((len = in.read(buffer)) != -1) {
                mappedBuffer.put(buffer, 0, len);
                //先写文件 再 更新数据库(这样即使突然被中断，下次读取的是数据库的进度，最多覆盖掉上一段下载的内容，而不是发生跳段写。文件出错)
                downloadBean.setBegin(downloadBean.getBegin() + len);
                mDbManager.updateDownloadBean(downloadBean);

                notifyDownloadProgress(url, downloadBean.getBegin(), file.length());

                //Log.d("TAG", " downloading = [" + downloadBean.getBegin() + "],body.contentLength():" + body.contentLength() + ", file.length():" + file.length());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                if (channelOut != null) {
                    channelOut.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //判断是完成 还是暂停
        if (file.length() == downloadBean.getBegin()) {
            downloadBean.setFinished(true);
            mDbManager.updateDownloadBean(downloadBean);
            notifyDownloadComplete(url);
        } else {
            notifyDownloadPause(url, downloadBean.getBegin());
        }
        //Log.d("TAG", "download success() called with: body.contentLength() = [" + body.contentLength() + "], downloadBean = [" + downloadBean);
        LogUtils.d("save 结束， downloadBean = [" + downloadBean + "],body.contentLength():" + body.contentLength() + ", file.length():" + file.length());

    }


    private void notifyDownloadComplete(final String url) {
        final DownloadListener downloadListener = mDownloadListener.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadComplete(url);
                }
            });
        }
    }

    private void notifyDownloadError(final String url, final Exception e) {
        final DownloadListener downloadListener = mDownloadListener.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadError(url, e);
                }
            });
        }
    }

    private void notifyDownloadProgress(final String url, final long progress, final long maxLenght) {
        final DownloadListener downloadListener = mDownloadListener.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloading(url, progress, maxLenght);
                }
            });
        }
    }

    private void notifyDownloadPause(final String url, final long progress) {
        final DownloadListener downloadListener = mDownloadListener.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadPause(url, progress);
                }
            });
        }
    }

    private void notifyDownloadPending(final String url) {
        final DownloadListener downloadListener = mDownloadListener.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadPending(url);
                }
            });
        }
    }

    //判断某个下载任务是否正在运行中
    private boolean isDownloading(String url) {
        return mDownloadingCallMap.get(url) != null;
    }

    //是否挂起中
    private boolean isPending(String url) {
        return mPendingCall.indexOf(url) > -1;
    }


    private OkHttpClient getClient() {
        if (null == mClient) {
            mClient = new OkHttpClient.Builder().build();
            maxRequests = mClient.dispatcher().getMaxRequestsPerHost();//5
            //没有那么多下载url，将最大请求数限制为2，测试列表复用item，停止之前的请求，执行新请求，同时排队老请求
            //mClient.dispatcher().setMaxRequests(2);
        }
        return mClient;
    }

    //项目中一般共用一个OkHttpClient ，可以通过此方法set进来
    public IDownloadManager setClient(OkHttpClient client) {
        if (null != client) {
            mClient = client;
            maxRequests = mClient.dispatcher().getMaxRequestsPerHost();//5
        }
        return this;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }
}
