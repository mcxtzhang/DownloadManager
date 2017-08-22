package com.mcxtzhang.downloadmanager.db.realm;

import android.text.TextUtils;

import com.mcxtzhang.downloadmanager.download.DownloadBean;
import com.mcxtzhang.downloadmanager.db.IDownloadDbManager;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Intro: Realm 实现的断点续传数据库下载类
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public enum RealmIDownloadDbManager implements IDownloadDbManager {
    INSTANCE;

    RealmIDownloadDbManager() {

    }

    private static final String PRIMARY_KEY = "url";

    @Override
    public void insertDownloadBean(DownloadBean downloadBean) {
        updateDownloadBean(downloadBean);
    }

    @Override
    public void deleteDownloadBean(DownloadBean downloadBean) {
        deleteDownloadBean(downloadBean.getUrl());
    }

    @Override
    public void deleteDownloadBean(String url) {
        if (TextUtils.isEmpty(url)) return;
        Realm realm = Realm.getDefaultInstance();
        try {
            final RealmResults<DownloadBean> toDeleteList = realm.where(DownloadBean.class)
                    .equalTo(PRIMARY_KEY, url)
                    .findAll();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    toDeleteList.deleteFirstFromRealm();
                }
            });
        } finally {
            realm.close();
        }
    }

    @Override
    public void updateDownloadBean(final DownloadBean downloadBean) {
        if (downloadBean == null)
            return;
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.insertOrUpdate(downloadBean);
                }
            });
        } finally {
            realm.close();
        }

    }

    @Override
    public DownloadBean selectDownloadBean(String url) {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<DownloadBean> list = realm.where(DownloadBean.class)
                    .equalTo(PRIMARY_KEY, url)
                    .findAll();
            return list.isEmpty() ? null : realm.copyFromRealm(list.first());
        } finally {
            realm.close();
        }
    }

    @Override
    public List<DownloadBean> selectAllDownloadBean() {
        Realm realm = Realm.getDefaultInstance();
        try {
            RealmResults<DownloadBean> list = realm.where(DownloadBean.class)
                    .findAll();
            return list.isEmpty() ? null : realm.copyFromRealm(list);
        } finally {
            realm.close();
        }
    }


}
