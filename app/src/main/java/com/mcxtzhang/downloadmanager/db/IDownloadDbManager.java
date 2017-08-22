package com.mcxtzhang.downloadmanager.db;

import com.mcxtzhang.downloadmanager.download.DownloadBean;

import java.util.List;

/**
 * Intro: 断点续传下载数据库接口
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public interface IDownloadDbManager {
    void insertDownloadBean(DownloadBean downloadBean);

    void deleteDownloadBean(DownloadBean downloadBean);

    void updateDownloadBean(DownloadBean downloadBean);

    DownloadBean selectDownloadBean(String url);

    List<DownloadBean> selectAllDownloadBean();

}
