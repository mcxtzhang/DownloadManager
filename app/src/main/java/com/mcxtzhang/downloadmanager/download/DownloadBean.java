package com.mcxtzhang.downloadmanager.download;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Intro: 下载任务bean
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public class DownloadBean extends RealmObject {
    private static int base = 1;
    //序号 展示列表时用
    private String index;

    @PrimaryKey
    private String url;
    //下载起始位置
    private long begin;
    //文件总字节
    private long totalLength;
    //文件名
    private String fileName;
    //下载完成
    private boolean finished;

    //根据url 命名fileName
    public DownloadBean(String url) {
        this.url = url;
        this.fileName = url.substring(url.lastIndexOf("/"));
        this.index = (base++)+"";
    }

    public DownloadBean() {

    }

    public String getIndex() {
        return index;
    }

    public DownloadBean setIndex(String index) {
        this.index = index;
        return this;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public DownloadBean setTotalLength(long totalLength) {
        this.totalLength = totalLength;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public DownloadBean setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DownloadBean setUrl(String url) {
        this.url = url;
        return this;
    }

    public long getBegin() {
        return begin;
    }

    public DownloadBean setBegin(long begin) {
        this.begin = begin;
        return this;
    }

    public boolean isFinished() {
        return finished;
    }

    public DownloadBean setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }

    @Override
    public String toString() {
        return "DownloadBean{" +
                "url='" + url + '\'' +
                ", begin=" + begin +
                ", totalLength=" + totalLength +
                ", fileName='" + fileName + '\'' +
                ", finished=" + finished +
                '}';
    }

/*
    public DownloadBean copy() {
        DownloadBean downloadBean = new DownloadBean(this.url, this.begin, this.totalLength, this.fileName, this.finished);
        return downloadBean;
    }*/
}
