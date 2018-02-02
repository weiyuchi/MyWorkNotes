package com.test.chiapp.versionsUpdate;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import com.test.chiapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by WYC on 2018/1/5.
 * 下载的服务
 * 创建/更新通知栏
 */
public class DownloadService extends Service {

    private final String TAG ="DownloadService(下载的服务):";

    private static final int NOTIFY_ID = 0;//定义notify的id，避免与其它的notification的处理冲突
    private static final String CHANNEL = "update";

    private DownloadBinder binder = new DownloadBinder();

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    //定义个更新速率，避免更新通知栏过于频繁导致卡顿
    private float rate = .0f;

    private DownloadCallback callback;
    /**
     * 回调方法
     */
    public interface DownloadCallback{
        void onPrepare();
        void onProgress(int progress);
        void onComplete(File file);
        void onFail(String msg);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG,"onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.w(TAG,"onBind");
        return binder;
    }

    /**
     * 和activity通讯的binder
     */
    public class DownloadBinder extends Binder {
        public DownloadService getService(){
            Log.w(TAG,"DownloadBinder");
            return DownloadService.this;
        }
    }

    /**
     * 关闭Service
     */
    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        mNotificationManager.cancelAll();
        mNotificationManager = null;
        mBuilder = null;
    }

    /**
     * 销毁时清空一下对notify对象的持有
     */
    @Override
    public void onDestroy() {
        mNotificationManager = null;
        super.onDestroy();
    }

    /**
     * 开始下载apk
     */
    public void downApk(String url,DownloadCallback callback) {
        Log.i(TAG,"downApk");
        this.callback = callback;
        if (TextUtils.isEmpty(url)) {
            complete("下载路径错误");
            return;
        }
        setNotification();
        handler.sendEmptyMessage(0);

        //开始下载 OkHttp3
        Request request = new Request.Builder().url(url).build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS);

        OkHttpClient okHttpClient = builder.build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {//失败
                Message message = Message.obtain();
                message.what = 1;
                message.obj = e.getMessage();
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.body() == null) {
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = "下载错误";
                    handler.sendMessage(message);
                    return;
                }
                InputStream is = null;
                byte[] buff = new byte[2048];
                int len;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    File file = createFile();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buff)) != -1) {
                        fos.write(buff,0,len);
                        sum+=len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        if (rate != progress) {
                            Message message = Message.obtain();
                            message.what = 2;
                            message.obj = progress;
                            handler.sendMessage(message);
                            rate = progress;
                        }
                    }
                    fos.flush();
                    Message message = Message.obtain();
                    message.what = 3;
                    message.obj = file.getAbsoluteFile();
                    handler.sendMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG,e.toString());
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        if (fos != null)
                            fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG,e.toString());
                    }
                }
            }
        });
    }
    /**
     * 创建通知栏
     */
    private void setNotification() {
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this,CHANNEL);
        mBuilder.setContentTitle("开始下载")
                .setContentText("正在连接服务器")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setOngoing(true)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis());
        mNotificationManager.notify(NOTIFY_ID, mBuilder.build());
    }
    /**
     * 下载完成
     */
    private void complete(String msg) {
        if (mBuilder != null) {
            mBuilder.setContentTitle("新版本").setContentText(msg);
            Notification notification = mBuilder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(NOTIFY_ID, notification);
        }
        stopSelf();//结束Service
    }


    /**
     * 把处理结果放回ui线程
     */
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    callback.onPrepare();
                    break;

                case 1:
                    mNotificationManager.cancel(NOTIFY_ID);
                    callback.onFail((String) msg.obj);
                    stopSelf();
                    break;

                case 2:{
                    int progress = (int) msg.obj;
                    callback.onProgress(progress);
                    mBuilder.setContentTitle("正在下载：新版本...")
                            .setContentText(String.format(Locale.CHINESE,"%d%%",progress))
                            .setProgress(100,progress,false)
                            .setWhen(System.currentTimeMillis());
                    Notification notification = mBuilder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    mNotificationManager.notify(NOTIFY_ID,notification);}
                break;

                case 3:{
                    callback.onComplete((File) msg.obj);
                    //app运行在界面,直接安装
                    //否则运行在后台则通知形式告知完成
                    if (onFront()) {
                        mNotificationManager.cancel(NOTIFY_ID);
                    } else {
                        Intent intent = installIntent((String) msg.obj);
                        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext()
                                ,0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(pIntent)
                                .setContentTitle(getPackageName())
                                .setContentText("下载完成，点击安装")
                                .setProgress(0,0,false)
                                .setDefaults(Notification.DEFAULT_ALL);
                        Notification notification = mBuilder.build();
                        notification.flags = Notification.FLAG_AUTO_CANCEL;
                        mNotificationManager.notify(NOTIFY_ID,notification);
                    }
                    stopSelf();}
                break;
            }
            return false;
        }
    });
    /**
     * 是否运行在用户前面
     */
    private boolean onFront() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null || appProcesses.isEmpty())
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(getPackageName()) &&
                    appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }
    /**
     * 安装 通知形式告知完成
     * 7.0 以上记得配置 fileProvider
     *
     */
    private Intent installIntent(String path){
        try {
            File file = new File(path);
            String authority = getApplicationContext().getPackageName() + ".fileProvider";
            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), authority, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            return intent;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 路径为根目录
     * 创建文件名称为 updateDemo.apk
     */
    private File createFile() {
        String root = Environment.getExternalStorageDirectory().getPath();
        File file = new File(root,"updateDemo.apk");
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null ;
    }
}
