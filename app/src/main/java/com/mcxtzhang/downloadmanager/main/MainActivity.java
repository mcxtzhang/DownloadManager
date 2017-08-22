package com.mcxtzhang.downloadmanager.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mcxtzhang.downloadmanager.R;
import com.mcxtzhang.downloadmanager.list.DownloadListActivity;
import com.mcxtzhang.downloadmanager.testleak.TestLeakActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.leakTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TestLeakActivity.class));
            }
        });
        findViewById(R.id.downloadList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
            }
        });

    }
}
