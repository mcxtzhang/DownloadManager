package com.mcxtzhang.downloadmanager.utils;

import android.widget.ProgressBar;

/**
 * Intro:
 * Author: zhangxutong
 * E-mail: mcxtzhang@163.com
 * Home Page: http://blog.csdn.net/zxt0601
 * Created:   2017/8/22.
 * History:
 */

public class UIUtils {
    //解决进度值过大不显示
    public static void setProgress(ProgressBar progressBar, long current, long max) {
        float precent = (float) current / max;
        progressBar.setMax(100);
        progressBar.setProgress((int) (precent * 100));
    }
}
