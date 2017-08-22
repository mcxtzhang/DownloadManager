package com.mcxtzhang.downloadmanager.testleak;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mcxtzhang.downloadmanager.utils.LogUtils;
import com.mcxtzhang.downloadmanager.R;
import com.mcxtzhang.downloadmanager.download.IDownloadManager;
import com.mcxtzhang.downloadmanager.download.ZDownloadManager;

import java.util.Arrays;

public class TestLeakActivity extends AppCompatActivity {
    IDownloadManager mDownloadManager;
    String[] URLS = new String[]{
            "http://imtt.dd.qq.com/16891/3AFA21F3690FB27C82A6AB6024E56852.apk?fsname=com.tencent.mobileqq_7.1.8_718.apk",
            "http://imtt.dd.qq.com/16891/DB5589010CEBEB416B679F96AE2DDE5A.apk?fsname=com.qiyi.video_8.8.0_80920.apk",
            "http://imtt.dd.qq.com/16891/184C6F379049E0409184564CFE23E5BF.apk?fsname=com.tencent.news_5.4.10_5410.apk",
            "http://imtt.dd.qq.com/16891/534821B3D6D65A9E3A3EF86B17925E1F.apk?fsname=com.taobao.taobao_6.10.3_160.apk",
            "http://imtt.dd.qq.com/16891/8F299C668E55846688C41697227370F3.apk?fsname=com.wuba_7.13.1_71301.apk",
            "http://imtt.dd.qq.com/16891/7A347BC1F3742C07B80018E791C45B19.apk?fsname=com.cyjh.gundam_2.8.2_282.apk",
            "http://imtt.dd.qq.com/16891/7A347BC1F3742C07B80018E791C45B19.apk?fsname=com.cyjh.gundam_2.8.2_282.apk",
            "http://imtt.dd.qq.com/16891/604AE9B8082A436BFDD2FE9BE375484B.apk?fsname=com.tencent.qqmusic_7.7.0.10_679.apk",
            "http://imtt.dd.qq.com/16891/16326E8A1340854CAD6C2995A29FD734.apk?fsname=com.sohu.inputmethod.sogou_8.12_660.apk",
            "http://imtt.dd.qq.com/16891/ABC019F9487340C5E3C0C15D267AD4E8.apk?fsname=com.moji.mjweather_7.0100.02_7010002.apk",
            "http://imtt.dd.qq.com/16891/A35BB3B996C62BD702A5E03D53D019EA.apk?fsname=com.baidu.BaiduMap_10.1.0_815.apk",
            "http://imtt.dd.qq.com/16891/51E77E87191D5E9F1B4E31F6BEA67E44.apk?fsname=com.tencent.reading_3.3.0_3300.apk",
            "http://imtt.dd.qq.com/16891/3E3FEBCCEA96112EF24C79B09BB0B379.apk?fsname=com.meelive.ingkee_4.1.11_901.apk",
            "http://imtt.dd.qq.com/16891/9D9D1DE2A2B0ECAD276F9114442DD324.apk?fsname=com.cubic.autohome_8.3.0_830.apk",
            "http://imtt.dd.qq.com/16891/3C5D6A4D5461CE7410E3B66A02757724.apk?fsname=com.sdu.didi.psnger_5.1.8_256.apk",
/*
            "http://flv2.bn.netease.com/videolib3/1604/28/fVobI0704/SD/fVobI0704-mobile.mp4",
            "https://apk.anlaiye.com/XiaoChao-v2.0.0-20170816.apk",
            "https://apk.anlaiye.com/XiaoChao-v2.0.0-20170817.apk",
            "http://dldir1.qq.com/weixin/android/weixin6513android1100.apk",

            "https://dldir1.qq.com/qqfile/qq/TIM1.2.0/21645/TIM1.2.0.exe",
            "https://apk.anlaiye.com/XiaoChao-v2.0.0-20170818.apk",
            "https://apk.anlaiye.com/XiaoChao-v2.0.0-20170821.apk",*/
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_leak);
        mDownloadManager = ZDownloadManager.INSTANCE;

        final ProgressBar progressBar1 = (ProgressBar) findViewById(R.id.pb1);
        final ProgressBar progressBar2 = (ProgressBar) findViewById(R.id.pb2);
        findViewById(R.id.down1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDownloadManager.download(URLS[0], new ZDownloadManager.DownloadListener() {
                    @Override
                    public void onDownloading(String url, long progress, long maxLenght) {
                        progressBar1.setMax((int) maxLenght);
                        progressBar1.setProgress((int) progress);
                    }

                    @Override
                    public void onDownloadPending(String url) {
                        Toast.makeText(TestLeakActivity.this, "排队中...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadPause(String url,long progress) {
                        Toast.makeText(TestLeakActivity.this, "下载暂停:" + progress, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadComplete(String url) {
                        progressBar1.setMax(progressBar1.getMax());
                        Toast.makeText(TestLeakActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadError(String url,Exception e) {
                        Toast.makeText(TestLeakActivity.this, "下载失败:" + e, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.cancel1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDownloadManager.stop(URLS[0]);
            }
        });


        findViewById(R.id.down2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDownloadManager.download(URLS[1], new ZDownloadManager.DownloadListener() {
                    @Override
                    public void onDownloading(String url, long progress, long maxLenght) {
                        progressBar2.setMax((int) maxLenght);
                        progressBar2.setProgress((int) progress);
                    }

                    @Override
                    public void onDownloadPending(String url) {
                        Toast.makeText(TestLeakActivity.this, "排队中...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadPause(String url,long progress) {
                        Toast.makeText(TestLeakActivity.this, "下载暂停:" + progress, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadComplete(String url) {
                        progressBar2.setMax(progressBar2.getMax());
                        Toast.makeText(TestLeakActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDownloadError(String url,Exception e) {
                        Toast.makeText(TestLeakActivity.this, "下载失败:" + e, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        findViewById(R.id.cancel2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDownloadManager.stop(URLS[1]);
            }
        });


        findViewById(R.id.btnStartAllTask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtils.i("静默开始所有下载任务");
                for (String s : URLS) {
                    mDownloadManager.download(s);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDownloadManager.unregisterDownloadListener(Arrays.asList(URLS));
    }
}
