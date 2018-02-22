package com.test.chiapp.rx_java;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.test.chiapp.R;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;


/**
 * Rxjava 入门
 */

public class RxjavaPrimary extends Activity {

    private final String TAG = "RxjavaPrimary{Rxjava入门}";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //方式1：分步骤实现
        // 步骤1：创建被观察者 Observable & 生产事件
        //  1. 创建被观察者 Observable 对象
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            // 2. 在复写的subscribe（）里定义需要发送的事件
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        });
        // 步骤2：创建观察者 Observer 并 定义响应事件行为
        Observer<Integer> observer = new Observer<Integer>() {
            //定义Disposable类变量
            private Disposable mDisposable;
            // 默认最先调用复写的 onSubscribe（）
            @Override
            public void onSubscribe(Disposable d) {
                // 2. 对Disposable类变量赋值
                mDisposable = d;
                Log.d(TAG, "开始采用subscribe连接");
            }
            // 通过复写对应方法来 响应 被观察者
            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "对Next事件"+ integer +"作出响应"  );
                if (integer == 2) {
                    // 设置在接收到第二个事件后切断观察者和被观察者的连接
                    mDisposable.dispose();
                    Log.d(TAG, "已经切断了连接：" + mDisposable.isDisposed());
                }
            }
            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "对Error事件作出响应");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "对Complete事件作出响应");
            }
        };
        // 步骤3：通过订阅（subscribe）连接观察者和被观察者
        observable.subscribe(observer);
    }


    /**
     * Rxjava 基于事件流的链式调用方式
     */
    private void RxJava(){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            // 1. 创建被观察者 & 生产事件
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
            }
        }).subscribe(new Observer<Integer>() {// 2. 通过通过订阅（subscribe）连接观察者和被观察者
            // 默认最先调用复写的 onSubscribe（）
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "开始采用subscribe连接");
            }
            // 3. 创建观察者 & 定义响应事件的行为
            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "对Next事件"+ integer +"作出响应"  );
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "对Error事件作出响应");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "对Complete事件作出响应");
            }
        });
    }
}
