package com.jack.demo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.jack.systemcatch.hook.ActivityThreadHooker;
import com.jack.systemcatch.hook.CatchThrowable;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author jack
 * @since 2020/7/28 18:03
 */
public class App extends Application {
    private File logFile;

    @Override
    public void onCreate() {
        super.onCreate();
        logFile = new File(this.getExternalCacheDir(), "throwable.txt");
        String processName = getProcessName(this, Process.myPid());
        if (TextUtils.isEmpty(processName) || !TextUtils.equals(getPackageName(), processName)) {
            Log.e("APP",processName);
            //如果有多个进程，其他进程也需要管理。请重复注册
            hook();
            return;
        }
        hook();
    }

    private void hook() {
        ActivityThreadHooker.hook("com.jack.demo.MainActivity");
        ActivityThreadHooker.addThrowableListener(new CatchThrowable() {
            @Override
            public void throwable(Throwable throwable) {
                Log.e("APP", "ThrowableListener:" + throwable.getMessage());
                throwable.printStackTrace();
                handlelException(throwable);
                //由用户主动触发退出
//                exit();
            }
        });
    }

    /**
     * 获取进程名
     *
     * @param cxt
     * @param pid
     * @return
     */
    public String getProcessName(Context cxt, int pid) {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningApps = null;
            if (am != null) {
                runningApps = am.getRunningAppProcesses();
            }
            if (runningApps == null) {
                return null;
            }
            for (android.app.ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
                if (procInfo.pid == pid) {
                    return procInfo.processName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void exit() {
        Process.killProcess(Process.myPid());
        System.exit(0);
    }


    /**
     * 记录异常信息
     */
    private boolean handlelException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        PrintWriter pw = null;
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            pw = new PrintWriter(logFile);

            // 收集手机及错误信息
            collectInfoToSDCard(pw, ex);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 收集记录错误信息
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @SuppressLint("SimpleDateFormat")
    private void collectInfoToSDCard(PrintWriter pw, Throwable ex) throws PackageManager.NameNotFoundException, IllegalAccessException, IllegalArgumentException {
        PackageManager pm = this.getPackageManager();
        PackageInfo mPackageInfo = pm.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);

        pw.println("time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())); // 记录错误发生的时间
        pw.println("versionCode: " + mPackageInfo.versionCode); // 版本号
        pw.println("versionName: " + mPackageInfo.versionName); // 版本名称

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            pw.print(field.getName() + " : ");
            pw.println(field.get(null).toString());
        }
        ex.printStackTrace(pw);
    }
}
