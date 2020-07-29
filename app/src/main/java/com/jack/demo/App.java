package com.jack.demo;

import android.app.Application;
import android.os.Process;
import android.util.Log;

import com.jack.systemcatch.hook.ActivityThreadHooker;
import com.jack.systemcatch.hook.CatchThrowable;

/**
 * @author jack
 * @since 2020/7/28 18:03
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ActivityThreadHooker.hook("com.jack.demo.MainActivity");
        ActivityThreadHooker.addThrowableListener(new CatchThrowable() {
            @Override
            public void throwable(Throwable throwable) {
                Log.e("APP", "ThrowableListener:" + throwable.getMessage());
                throwable.printStackTrace();
                //由用户主动触发退出
//                exit();
            }
        });
    }

    private void exit() {
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
