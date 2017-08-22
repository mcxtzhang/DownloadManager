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

    //正在下载的集合（url-Call)
    private Map<String, Call> downloadingCallMap;
    //被挂起的集合


    //回调相关
    private Handler mMainHandler;
    //url-listener
    private Map<String, DownloadListener> downloadListeners;

    ZDownloadManager() {
        downloadingCallMap = new ArrayMap<>();
        mDbManager = DownloadDbFactory.getDownloadDbmanager();

        mMainHandler = new Handler(Looper.getMainLooper());
        downloadListeners = new ArrayMap<>();
    }

    @Override
    public void download(String url) {
        download(url, null);
    }

    @Override
    public void download(final String url, DownloadListener downloadListener) {
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

        Request request = new Request.Builder()
                .header("RANGE", "bytes=" + downloadBean.getBegin() + "-")
                .url(url)
                .build();
        Call call = getClient().newCall(request);
        //正在下载任务集合
        downloadingCallMap.put(url, call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyDownloadError(url, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                save(response, url);
            }
        });
        notifyDownloadProgress(url, downloadBean.getBegin(), downloadBean.getTotalLength());
    }

    @Override
    public void registerDownloadListener(String url, DownloadListener downloadListener) {
        if (TextUtils.isEmpty(url) || downloadListener == null) return;
        downloadListeners.put(url, downloadListener);
    }

    @Override
    public void unregisterAllListener() {
        downloadListeners.clear();
    }

    @Override
    public void unregisterDownloadListener(List<String> urls) {
        if (null == urls || urls.isEmpty()) return;
        for (String url : urls) {
            unregisterDownloadListener(url);
        }
    }


    @Override
    public void unregisterDownloadListener(String url) {
        if (TextUtils.isEmpty(url)) return;
        downloadListeners.remove(url);
    }

    @Override
    public void stop(String url) {
        if (TextUtils.isEmpty(url)) return;
        Call call = downloadingCallMap.get(url);
        DownloadBean downloadBean = mDbManager.selectDownloadBean(url);
        if (null != call) {
            LogUtils.d("stop,停止执行任务 = [" + url + "]");
            call.cancel();
            downloadingCallMap.remove(url);
        }
        notifyDownloadPause(url, downloadBean!=null?downloadBean.getBegin():0);
    }

    @Override
    public List<DownloadBean> selectAllDownloadBean() {
        return mDbManager.selectAllDownloadBean();
    }

    @Override
    public int selectStatus(String url) {
        if (isDownloading(url)) {
            return STATUS_DOWNLOADING;
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
    public void deleteFile(DownloadBean bean) {
        // TODO: 2017/8/22 子线程操作 删除文件
        mDbManager.deleteDownloadBean(bean);
    }

    //下载文件 同时 将进度存入数据库， 回调通知监听者
    private void save(Response response, String url) {
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
        //从运行中任务集合删除该url
        downloadingCallMap.remove(downloadBean.getUrl());
        //Log.d("TAG", "download success() called with: body.contentLength() = [" + body.contentLength() + "], downloadBean = [" + downloadBean);
        LogUtils.d("save 结束， downloadBean = [" + downloadBean + "],body.contentLength():" + body.contentLength() + ", file.length():" + file.length());

    }


    private void notifyDownloadComplete(String url) {
        final DownloadListener downloadListener = downloadListeners.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadComplete();
                }
            });
        }
    }

    private void notifyDownloadError(String url, final Exception e) {
        final DownloadListener downloadListener = downloadListeners.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadError(e);
                }
            });
        }
    }

    private void notifyDownloadProgress(String url, final long progress, final long maxLenght) {
        final DownloadListener downloadListener = downloadListeners.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloading(progress, maxLenght);
                }
            });
        }
    }

    private void notifyDownloadPause(String url, final long progress) {
        final DownloadListener downloadListener = downloadListeners.get(url);
        if (null != downloadListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    downloadListener.onDownloadPause(progress);
                }
            });
        }
    }

    //判断某个下载任务是否正在运行中
    private boolean isDownloading(String url) {
        Call call = downloadingCallMap.get(url);
        return call != null;
    }


    private OkHttpClient getClient() {
        if (null == mClient) {
            mClient = new OkHttpClient.Builder().build();
            //没有那么多下载url，将最大请求数限制为2，测试列表复用item，停止之前的请求，执行新请求，同时排队老请求
            //mClient.dispatcher().setMaxRequests(2);
        }
        return mClient;
    }

    //项目中一般共用一个OkHttpClient ，可以通过此方法set进来
    public IDownloadManager setClient(OkHttpClient client) {
        if (null != client) {
            mClient = client;
        }
        return this;
    }


}
