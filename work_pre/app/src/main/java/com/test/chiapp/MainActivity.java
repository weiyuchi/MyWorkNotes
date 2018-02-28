package com.test.chiapp;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.test.chiapp.view.roll_banner.RollBannerShowActivity;

public class MainActivity extends Activity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.main_activity_hello);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this , VersionsUpdateActivity.class);//更新/下载APP版本
                Intent intent = new Intent(MainActivity.this , RollBannerShowActivity.class);//自定义的轮播图
                startActivity(intent);
            }
        });
    }

}
