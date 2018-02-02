package com.test.chiapp.versionsUpdate;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.test.chiapp.R;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static android.os.Process.killProcess;

/**
 * Created by WYC on 2018/1/5.
 * 版本更新Activity
 */

public class VersionsUpdateActivity extends AppCompatActivity implements IndexContract.View {

    private Dialog mDialog;
    private IndexPresenter mPresenter;
    private TextView mTextView;
    private Context context;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_versions_update);
        this.context = VersionsUpdateActivity.this;
        mPresenter = new IndexPresenter(this);
        mTextView = findViewById(R.id.versions_t1);
        AndPermission.with(VersionsUpdateActivity.this).permission(Manifest.permission.WRITE_EXTERNAL_STORAGE).
                requestCode(200).rationale(new RationaleListener() {
            @Override
            public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                AndPermission.rationaleDialog(VersionsUpdateActivity.this, rationale).show();
            }
        }).callback(listener).start();
    }

    void needStorage() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(),0);
            String local = pi.versionName;
            mPresenter.checkUpdate(local);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showUpdate(final String version) {//展示下载APK对话框
        if (mDialog == null){
            mDialog = new AlertDialog.Builder(this)
                    .setTitle("检测到有新版本")
                    .setMessage("当前版本:"+version)
                    .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPresenter.downApk(context);
                        }
                    })
                    .setNegativeButton("忽略", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPresenter.setIgnore(version);
                        }
                    }) .create();
        }
        //重写这俩个方法，一般是强制更新不能取消弹窗
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_BACK && mDialog != null && mDialog.isShowing();
            }
        });
        mDialog.show();
    }

    @Override
    public void showProgress(int progress) {
        mTextView.setText(String.format(Locale.CHINESE,"%d%%", progress));
    }

    @Override
    public void showFail(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showComplete(File file) {//打开下载完成的安装包
        try {
            String authority = getApplicationContext().getPackageName() + ".fileProvider";
            Uri fileUri = FileProvider.getUriForFile(context, authority, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            //7.0以上需要添加临时读取权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            } else {
                Uri uri = Uri.fromFile(file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }

            startActivity(intent);

            //弹出安装窗口把原程序关闭。
            //避免安装完毕点击打开时没反应
            killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private PermissionListener listener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantedPermissions) {
            // 权限申请成功回调。
            if(requestCode == 200) {
                needStorage();
            }
        }

        @Override
        public void onFailed(int requestCode, final List<String> deniedPermissions) {
            // 权限申请失败回调。
            for (int i = 0;i<deniedPermissions.size();i++){
                // 用户是否勾选了不再提示并且拒绝了权限，那么提示用户到设置中授权。
                if (AndPermission.hasAlwaysDeniedPermission(context, deniedPermissions)) {
                    // 第一种：用默认的提示语。
                    AndPermission.defaultSettingDialog(context).show();

                    // 第二种：用自定义的提示语。
                    // AndPermission.defaultSettingDialog(this, REQUEST_CODE_SETTING)
                    // .setTitle("权限申请失败")
                    // .setMessage("我们需要的一些权限被您拒绝或者系统发生错误申请失败，请您到设置页面手动授权，否则功能无法正常使用！")
                    // .setPositiveButton("好，去设置")
                    // .show();

                    // 第三种：自定义dialog样式。
                    // SettingService settingService =
                    //    AndPermission.defineSettingDialog(this, REQUEST_CODE_SETTING);
                    // 你的dialog点击了确定调用：
                    // settingService.execute();
                    // 你的dialog点击了取消调用：
                    // settingService.cancel();
                }
            }

        }
    };
}
