package com.mcxtzhang.downloadmanager;

import android.app.Application;

import com.mcxtzhang.downloadmanager.db.realm.RealmConfig;

/**
 * Intro: Application类
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public class CstApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RealmConfig.init(this);
    }
}
