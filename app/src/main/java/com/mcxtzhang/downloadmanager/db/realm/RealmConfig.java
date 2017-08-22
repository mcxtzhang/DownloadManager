package com.mcxtzhang.downloadmanager.db.realm;

import android.content.Context;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmConfig {

    public static void init(Context context) {
        context = context.getApplicationContext();
        Realm.init(context);

        Realm.getInstance(new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name("DownloadManager.realm")
                .build());
    }
}