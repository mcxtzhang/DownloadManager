package com.mcxtzhang.downloadmanager.db;

import com.mcxtzhang.downloadmanager.db.realm.RealmIDownloadDbManager;

/**
 * Intro: 断点续传数据库管理 工厂类
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public class DownloadDbFactory {
    public static IDownloadDbManager getDownloadDbmanager() {
        return RealmIDownloadDbManager.INSTANCE;
    }
}
