package com.test.chiapp.versionsUpdate;

import android.content.Context;

import java.io.File;

/**
 * Created by Administrator on 2018/1/5.
 */

public interface IndexContract {
    interface View {
        void showUpdate(String version);
        void showProgress(int progress);
        void showFail(String msg);
        void showComplete(File file);
    }

    interface Presenter{
        void checkUpdate(String local);
        void setIgnore(String version);
        void downApk(Context context);
        void unbind(Context context);
    }
}
