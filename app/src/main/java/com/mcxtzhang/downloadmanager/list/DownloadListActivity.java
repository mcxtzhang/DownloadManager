package com.mcxtzhang.downloadmanager.list;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mcxtzhang.commonadapter.rv.CommonAdapter;
import com.mcxtzhang.commonadapter.rv.ViewHolder;
import com.mcxtzhang.downloadmanager.utils.LogUtils;
import com.mcxtzhang.downloadmanager.R;
import com.mcxtzhang.downloadmanager.download.DownloadBean;
import com.mcxtzhang.downloadmanager.download.IDownloadManager;
import com.mcxtzhang.downloadmanager.download.ZDownloadManager;
import com.mcxtzhang.downloadmanager.utils.UIUtils;

import java.util.List;

public class DownloadListActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    List<DownloadBean> mDatas;
    IDownloadManager mDownloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        mDownloadManager = ZDownloadManager.INSTANCE;

        mRecyclerView = (RecyclerView) findViewById(R.id.rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mDatas = mDownloadManager.selectAllDownloadBean();
        if (null != mDatas) {
            for (DownloadBean downloadBean : mDatas) {
                LogUtils.i("数据库给的数据:" + downloadBean);
            }
        }
        mRecyclerView.setAdapter(new CommonAdapter<DownloadBean>(this, mDatas, R.layout.item_down) {
            @Override
            public void convert(final ViewHolder holder, DownloadBean downloadBean) {
                //根据url查询当期前item最新的数据库值
                downloadBean = mDownloadManager.selectDownloadBean(downloadBean.getUrl());
                LogUtils.d("convert() called with: holder = [" + holder + "], downloadBean = [" + downloadBean + "]");
                //注销之前任务的监听器,防止重复修改UI
                final String pUrl = (String) holder.itemView.getTag();
                mDownloadManager.unregisterDownloadListener(pUrl);
                //url 为 tag,用于注销监听器
                holder.itemView.setTag(downloadBean.getUrl());

                //先根据数据库里的值更新UI
                holder.setText(R.id.id, downloadBean.getIndex());
                holder.setText(R.id.tvName, downloadBean.getFileName());
                UIUtils.setProgress((ProgressBar) holder.getView(R.id.progress), downloadBean.getBegin(), downloadBean.getTotalLength());
                setButtonStatus(downloadBean.getUrl(), holder);

                //监听进度 改变UI
                mDownloadManager.registerDownloadListener(downloadBean.getUrl(), new IDownloadManager.DownloadListener() {
                    @Override
                    public void onDownloading(String url, long progress, long maxLenght) {
                        if (!checkValid(url, holder)) return;//没来及注销 回调先执行的异常情况

                        UIUtils.setProgress((ProgressBar) holder.getView(R.id.progress), progress, maxLenght);
                        setButtonStatus(url, holder, IDownloadManager.STATUS_DOWNLOADING);
                    }

                    @Override
                    public void onDownloadPending(String url) {
                        if (!checkValid(url, holder)) return;//没来及注销 回调先执行的异常情况

                        //UIUtils.setProgress((ProgressBar) holder.getView(R.id.progress), downloadBean.getBegin(), downloadBean.getTotalLength());
                        setButtonStatus(url, holder, IDownloadManager.STATUS_PENDING);
                    }

                    @Override
                    public void onDownloadPause(String url, long progress) {
                        if (!checkValid(url, holder)) return;//没来及注销 回调先执行的异常情况

                        //Toast.makeText(mContext, "暂停，已下载字节：" + progress, Toast.LENGTH_SHORT).show();
                        setButtonStatus(url, holder, IDownloadManager.STATUS_UNFINISHED);
                    }

                    @Override
                    public void onDownloadComplete(String url) {
                        if (!checkValid(url, holder)) return;//没来及注销 回调先执行的异常情况

                        UIUtils.setProgress((ProgressBar) holder.getView(R.id.progress), 100, 100);
                        setButtonStatus(url, holder, IDownloadManager.STATUS_FINISHED);

                        //Toast.makeText(mContext, "下载完成", Toast.LENGTH_SHORT).show();
                        //当前任务下载完，再继续下载之前被我们取消的任务：
                        //但是静默下载，修改tag
                        //LogUtils.d("继续执行之前被我们取消的任务：" + pUrl + ", 已完成任务：" + url);
/*                        mDownloadManager.download(pUrl);
                        holder.itemView.setTag(pUrl);*/
                        mDownloadManager.unregisterDownloadListener(url);
                    }

                    @Override
                    public void onDownloadError(String url, Exception e) {
                        if (!checkValid(url, holder)) return;//没来及注销 回调先执行的异常情况

                        Toast.makeText(mContext, "下载出错：" + e, Toast.LENGTH_SHORT).show();
                        setButtonStatus(url, holder, IDownloadManager.STATUS_UNFINISHED);
                    }
                });


            }

            private boolean checkValid(String url, ViewHolder holder) {
                return (url.equals(holder.itemView.getTag()));
            }

            //根据status设置按钮
            public void setButtonStatus(final String url, ViewHolder holder) {
                setButtonStatus(url, holder, mDownloadManager.selectStatus(url));
            }

            public void setButtonStatus(final String url, final ViewHolder holder, int status) {
                switch (status) {
                    case IDownloadManager.STATUS_FINISHED:
                        holder.setText(R.id.stop, "删除");
                        holder.setOnClickListener(R.id.stop, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDownloadManager.deleteFile(url);
                                mDatas.remove(holder.getLayoutPosition());
                                notifyItemRemoved(holder.getLayoutPosition());
                            }
                        });
                        break;
                    case IDownloadManager.STATUS_DOWNLOADING:
                        holder.setText(R.id.stop, "暂停");
                        holder.setOnClickListener(R.id.stop, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDownloadManager.stop(url);
                            }
                        });
                        break;
                    case IDownloadManager.STATUS_PENDING:
                        holder.setText(R.id.stop, "队列中");
                        holder.setOnClickListener(R.id.stop, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        break;
                    case IDownloadManager.STATUS_UNFINISHED:
                    default:
                        holder.setText(R.id.stop, "继续");
                        holder.setOnClickListener(R.id.stop, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDownloadManager.download(url);
                            }
                        });
                        break;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDownloadManager.unregisterAllListener();
    }
}
