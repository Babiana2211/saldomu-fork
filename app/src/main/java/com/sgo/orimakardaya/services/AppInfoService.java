package com.sgo.orimakardaya.services;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;
import android.util.Log;
import com.sgo.orimakardaya.coreclass.AppInfoHandler;

import timber.log.Timber;

/*
  Created by Administrator on 1/13/2015.
 */
public class AppInfoService extends Service {

    private final IBinder testBinder = new MyLocalBinder();
    private boolean isServiceDestroyed;

    public static final long LOOPING_TIME = 500000; // 30 detik = 30 * 1000 ms

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
        }
    };

    private Runnable callBalance = new Runnable() {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            AppInfoHandler mBH = new AppInfoHandler(getApplicationContext());
            mBH.getAppVersion();
            Timber.i("Service jalankan call AppInfo Service");
            if(!isServiceDestroyed)mHandler.postDelayed(this, LOOPING_TIME);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("Masuk onCreate call AppInfo Service");
        setServiceDestroyed(false);
        mHandler.removeCallbacks(callBalance);
        mHandler.postDelayed(callBalance, LOOPING_TIME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("Masuk onBind Service App Info");
        return testBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("Masuk onStartCommand");
        return START_STICKY;
    }

    public boolean isServiceDestroyed() {
        return isServiceDestroyed;
    }

    public void setServiceDestroyed(boolean isServiceDestroyed) {
        this.isServiceDestroyed = isServiceDestroyed;
    }

    public class MyLocalBinder extends Binder {
        public AppInfoService getService() {
            return AppInfoService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.i("Masuk onDestroy App Info Service");
        setServiceDestroyed(true);
        mHandler.removeCallbacks(callBalance);
    }

    public void StopCallAppInfo(){
        mHandler.removeCallbacks(callBalance);
    }

    public void StartCallAppInfo(){
        mHandler.postDelayed(callBalance, LOOPING_TIME);
    }

}
