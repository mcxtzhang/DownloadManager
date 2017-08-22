package com.mcxtzhang.downloadmanager.download;

import java.util.List;

/**
 * Intro:
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public interface IDownloadManager {
    interface DownloadListener {
        void onDownloading(long progress, long maxLenght);

        void onDownloadPause(long progress);

        void onDownloadComplete();

        void onDownloadError(Exception e);
    }

    //静默下载 不需要回调
    void download(String url);

    //下载同时得到回调
    void download(String url, DownloadListener downloadListener);

    void registerDownloadListener(String url, DownloadListener downloadListener);

    //activity退出时调用，不再监听回调
    void unregisterAllListener();

    void unregisterDownloadListener(List<String> urls);

    void unregisterDownloadListener(String url);

    //停止某任务下载(不会加入pending队列)
    void stop(String url);
/*

    //暂停某任务下载，会将任务加入pending队列
    void pause(String url);
*/

    //查询所有状态的下载任务
    List<DownloadBean> selectAllDownloadBean();

    int STATUS_FINISHED = 1;//已完成
    int STATUS_DOWNLOADING = 2;//下载中
    int STATUS_UNFINISHED = 3;//未下载、就绪

    //根据url查询下载任务状态
    int selectStatus(String url);

    void deleteFile(DownloadBean bean);

}
