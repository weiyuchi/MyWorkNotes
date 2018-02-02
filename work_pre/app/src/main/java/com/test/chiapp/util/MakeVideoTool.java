package com.test.chiapp.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.test.chiapp.R;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.internal.Util;

/**
 * Created by WYC on 2018/1/24.
 * 录制视频
 */

public class MakeVideoTool extends Activity implements MediaRecorder.OnErrorListener {
    private final String MP4_FORMAT = ".mp4";
    /**
     * WYC 为自定义的文件夹名称
     */
    private final String BASE_SDCARD_IMAGES = Environment.getExternalStorageDirectory().getPath() + "/" + "WYC" + "/";
    private final int mWidth = 640;// 视频分辨率宽度
    private final int mHeight = 480;// 视频分辨率高度
    private final int mRecordMaxTime = 10;// 一次拍摄最长时间
    private  int mTimeCount;// 时间计数
    private final int DISTANCE_Y_CANCEL = 50;


    /**
     * 展示视频
     * SurfaceView的性质决定了其比较适合一些场景：需要界面迅速更新、对帧率要求较高的情况。
     */
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private File mVecordFile = null;// 存储 录制好的视频
    private Timer mTimer;// 计时器

    private ProgressBar mProgressBar;
    private TextView start_video_recorder;
    private RelativeLayout movieRecorder;

    public boolean isStopRecord = false; //停止拍摄
    private boolean isFinished = false;//判断录制是否已经完成
    private boolean isWantCancle = false;

    private Camera mCamera;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_recorder_view_continue);
        context = getApplicationContext();
        initView();
        initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                stop();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        movieRecorder = (RelativeLayout) findViewById(R.id.movieRecorder);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setMax(mRecordMaxTime);// 设置进度条最大量
        start_video_recorder = (TextView) findViewById(R.id.start_video_recorder);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new CustomCallBack());
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        start_video_recorder.setText("按住拍");
        start_video_recorder.setOnClickListener(null);
        start_video_recorder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();// 获得x轴坐标
                int y = (int) event.getY();// 获得y轴坐标
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        record();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (wantToCancle(x, y)) {
                            isWantCancle = true;
                            start_video_recorder.setText("手指上滑，取消发送");
                        } else {
                            isWantCancle = false;
                            start_video_recorder.setText("按住拍");
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isWantCancle) {
                            stop();
                            start_video_recorder.setText("按住拍");
                            finish();
                        } else if (mTimeCount < 2) {
                            stop();
                            if (!isFinished) {
                                Toast.makeText(context, "时间不能小于1秒", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            finishRecord();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    /**
     * 初始化
     */
    private void initRecord() throws IOException {
        isStopRecord = false;

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        if (mCamera != null)
            mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);// 视频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 音频源
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 视频输出格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// 音频格式
        mMediaRecorder.setVideoSize(mWidth, mHeight);// 设置分辨率：
//	        mMediaRecorder.setVideoSize(mHeight,mWidth);// 设置分辨率：
        // mMediaRecorder.setVideoFrameRate(16);// 这个我把它去掉了，感觉没什么用
        mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 512);// 设置帧频率，然后就清晰了
        mMediaRecorder.setOrientationHint(90);// 输出旋转90度，保持竖屏录制
        if (Build.MANUFACTURER.equalsIgnoreCase("meizu")) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 视频录制格式
        } else {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);// 视频录制格式
        }
        // mediaRecorder.setMaxDuration(Constant.MAXVEDIOTIME * 1000);
        mMediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());
        mMediaRecorder.prepare();
        try {
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void record() {
        isFinished = false;
        createRecordDir();
        try {
            if (mCamera == null) {// 如果未打开摄像头，则打开
                initCamera();
            }
            initRecord();
            mTimeCount = 0;// 时间计数器重新赋值
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    if (isStopRecord) {
                        return;
                    }
                    mTimeCount++;
                    mProgressBar.setProgress(mTimeCount);// 设置进度条
                    if (mTimeCount == mRecordMaxTime) {// 达到指定时间，停止拍摄

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isWantCancle) {
                                    stop();
                                } else {
                                    finishRecord();
                                }
                            }
                        });

//                    VideoMaker.this.post(new Runnable() {
//                        public void run() {
//                            if (isWantCancle) {
//                                stop();
//                            }else {
//                                finishRecord();
//                            }
//                        }
//                    });
                    }
                }
            }, 0, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean wantToCancle(int x, int y) {
        if (x < 0 || x > movieRecorder.getWidth()) { // 超过按钮的宽度
            return true;
        }
        // 超过按钮的高度
        if (y < -DISTANCE_Y_CANCEL || y > movieRecorder.getHeight() + DISTANCE_Y_CANCEL) {
            return true;
        }

        return false;
    }
    /**
     * 停止拍摄
     */
    private void stop() {
        try {
//            setVisibility(View.GONE);
            stopRecord();
            releaseRecord();
            freeCameraResource();
//				initCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 结束录制
     */
    private void finishRecord() {
        try {
            if (isFinished) {
                return;
            }
            isFinished = true;
            stopRecord();
            releaseRecord();
            freeCameraResource();
            if (mVecordFile != null&&!mVecordFile.getPath().equals("")) {

                //TODO 这里获得了 录制成功后的视频本地路径
//                Intent intent  = new Intent(context,QuickAskVideoActivity.class);
//                intent.putExtra("path",mVecordFile.getPath());
//                startActivity(intent);
//                Log.w("WYC",mVecordFile.getPath());
//                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        try {
            isStopRecord = true;
            if (mTimer != null)
                mTimer.cancel();
            if (mMediaRecorder != null) {
                // 设置后不会崩
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                try {
                    SystemClock.sleep(500);
                    mMediaRecorder.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mTimeCount = 0;// 时间计数器重新赋值
            mProgressBar.setProgress(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     *
     * @author liuyinjun
     * @date 2015-2-5
     */
    private void releaseRecord() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.setOnErrorListener(null);
                try {
                    mMediaRecorder.release();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mMediaRecorder = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化摄像头
     *
     * @throws IOException
     * @author lip
     * @date 2015-3-16
     */
    private void initCamera() {
        try {
            try {
                if (mCamera != null) {
                    freeCameraResource();
                }
                try {
                    mCamera = Camera.open();
                } catch (Exception e) {
                    e.printStackTrace();
                    freeCameraResource();
                }
                if (mCamera == null)
                    return;

                setCameraParams();
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                mCamera.unlock();
            } catch (Exception e) {
                e.printStackTrace();
                initCamera();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.lock();
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewDisplay(null);
                mCamera.release();
                mCamera = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置摄像头为竖屏
     *
     * @author lip
     * @date 2015-3-16
     */
    private void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> mSupportedPreviewSizes = params.getSupportedPreviewSizes();
            List<Camera.Size> fasd = params.getSupportedVideoSizes();
            Camera.Size size = params.getPreferredPreviewSizeForVideo();
            if (mSupportedPreviewSizes != null) {
                size = getOptimalPreviewSize(mSupportedPreviewSizes,
                        Math.max(mWidth, mHeight), Math.min(mWidth, mHeight));
            }
            params.set("orientation", "portrait");
            params.setPreviewSize(size.width, size.height);
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mCamera.setParameters(params);
        }
    }

    /**
     * 从Google官方的Camera示例程序中可以看出，选择预览尺寸的标准是
     * （1）摄像头支持的预览尺寸的宽高比与SurfaceView的宽高比的绝对差值小于0.1；
     * （2）在（1）获得的尺寸中，选取与SurfaceView的高的差值最小的。
     * 通过代码对这两个标准进行了实现，这里贴一下官方的代码：
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SurfaceView 控制
     */
    private class CustomCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                initCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            freeCameraResource();
        }

    }

    /**
     * 制作视频文件
     */
    private void createRecordDir() {
        //判断目录是否存在 ，不存创建目录
        File sampleDir = new File(BASE_SDCARD_IMAGES+"video/");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        String fileName = BASE_SDCARD_IMAGES + "video/recording" + System.currentTimeMillis() + MP4_FORMAT;
        // 创建文件
        try {
            mVecordFile = new File(fileName);
            boolean sucess = mVecordFile.createNewFile();
            Log.d("Path:", mVecordFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            mVecordFile = null;
        }
    }

}
