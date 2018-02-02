package com.test.chiapp.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.view.View.MeasureSpec;
/**
 * Created by WYC on 2018/1/3.
 * 屏幕相关工具类
 */

public class ScreenUtil {

    /*截屏（截图）https://mp.weixin.qq.com/s/JPVZtErFTzJ5PDuTAPk0DA*/
    //1普通截屏的实现
    /**
     * 普通截屏 获取当前Window 的 DrawingCache 的方式，即decorView的DrawingCache。
     * 在当前的屏幕上，使用状态，但状态是反式的
     * @param ctx current activity   */
    public static Bitmap shotActivity(Activity ctx) {
        View view = ctx.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bp = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, view.getMeasuredWidth(),
                view.getMeasuredHeight());
        view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();
        return bp;
    }

    /**
     * 获取当前View的DrawingCache
     */
    public static Bitmap getViewBp(View v) {
        if (null == v) {
            return null;
        }
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache();
        if (Build.VERSION.SDK_INT >= 11) {
            v.measure(MeasureSpec.makeMeasureSpec(v.getWidth(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    v.getHeight(), MeasureSpec.EXACTLY));
            v.layout((int) v.getX(), (int) v.getY(),
                    (int) v.getX() + v.getMeasuredWidth(),
                    (int) v.getY() + v.getMeasuredHeight());
        } else {
            v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        }
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache(), 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        v.setDrawingCacheEnabled(false);
        v.destroyDrawingCache();
        return b;
    }
}
