package com.jack.systemcatch.hook;

import android.content.res.Resources;
import android.os.Build;
import android.os.DeadSystemException;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.jack.systemcatch.hook.Constants.TAG;
import static com.jack.systemcatch.hook.Reflection.getFieldValue;
import static com.jack.systemcatch.hook.Reflection.getStaticFieldValue;
import static com.jack.systemcatch.hook.Reflection.invokeMethod;
import static com.jack.systemcatch.hook.Reflection.invokeStaticMethod;
import static com.jack.systemcatch.hook.Reflection.setFieldValue;


/**
 * 用于捕获由系统引起的ActivityThread异常
 *
 * @author jack
 * @since 2020/7/28 16:57
 */
class ActivityThreadCallback implements Handler.Callback {
    /**
     * 系统升级引发的一些异常，可以直接退出，重启会正常
     */
    private static final String LOADED_APK_GET_ASSETS = "android.app.LoadedApk.getAssets";
    private static final String ASSET_MANAGER_GET_RESOURCE_VALUE = "android.content.res.AssetManager.getResourceValue";

    /**
     * 系统类的包名
     */
    private static final String[] SYSTEM_PACKAGE_PREFIXES = {
            "java.",
            "android.",
            "androidx.",
            "dalvik.",
            "com.android.",
    };

    /**
     * ActivityThread$H
     */
    public final Handler mHandler;

    /**
     * ActivityThread.mH.mCallback
     */
    private final Handler.Callback mDelegate;

    /**
     * 堆栈信息。通过堆栈中是否存在非系统的类，便可判断异常是否由APP自身业务导致的
     */
    private final Set<String> mIgnorePackages;

    /**
     * 回传Throwable监听
     */
    private static CatchThrowable catchThrowable;

    /**
     * @param ignorePackages 包忽略名单
     */
    public ActivityThreadCallback(String[] ignorePackages) {
        Set<String> packages = new HashSet<>(Arrays.asList(SYSTEM_PACKAGE_PREFIXES));
        for (String pkg : ignorePackages) {
            if (TextUtils.isEmpty(pkg)) {
                continue;
            }
            packages.add(pkg);
        }
        //本lib工程可能引发的问题，也catch住
        Package aPackage = getClass().getPackage();
        if (aPackage != null)
            packages.add(aPackage.getName() + ".");
        //返回不可修改的set
        this.mIgnorePackages = Collections.unmodifiableSet(packages);
        //反射得到ActivityThread.mH
        this.mHandler = getHandler(getActivityThread());
        //反射得到Handler.mCallback
        this.mDelegate = getFieldValue(this.mHandler, "mCallback");
    }

    @Override
    public final boolean handleMessage(Message msg) {
        try {
            //交由自己处理
            if (this.mDelegate != null) {
                return this.mDelegate.handleMessage(msg);
            }
            if (this.mHandler != null) {
                this.mHandler.handleMessage(msg);
            }
        } catch (NullPointerException e) {
            //通常发生在app升级安装之后，看起来像是系统bug
            if (hasStackTraceElement(e, ASSET_MANAGER_GET_RESOURCE_VALUE, LOADED_APK_GET_ASSETS)) {
                //终止应用
                return abort(e);
            }
            //重新处理异常
            rethrowIfCausedByUser(e);
        } catch (SecurityException
                | IllegalArgumentException
                | AndroidRuntimeException
                | Resources.NotFoundException
                | WindowManager.BadTokenException e) {
            //完全交由自己处理，不影响后续操作
            rethrowIfCausedByUser(e);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            //Android7.0才有该异常DeadSystemException
            if (((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) && isCausedBy(cause, DeadSystemException.class))
                    // 通常发生在app升级安装之后，看起来像是系统bug
                    || (isCausedBy(cause, NullPointerException.class) && hasStackTraceElement(e, LOADED_APK_GET_ASSETS))) {
                //终止应用
                return abort(e);
            }
            rethrowIfCausedByUser(e);
        } catch (Error e) {
            rethrowIfCausedByUser(e);
            //处理DeadObjectException，一般都是由于提供远程服务的进程挂掉导致
            //说明是系统内部错误，终止应用
            return abort(e);
        }

        return true;
    }

    /**
     * 如果由用户引起，则重新抛出
     */
    private void rethrowIfCausedByUser(RuntimeException e) {
        if (isCausedByUser(e)) {
            throw e;
        } else {
            //自己内部try-catch回传Throwable出去
            catchThrowableShow(e);
        }
    }

    private void rethrowIfCausedByUser(Error e) {
        if (isCausedByUser(e)) {
            throw e;
        }
    }

    /**
     * 是否是用户引起的异常
     */
    private boolean isCausedByUser(Throwable t) {
        if (t == null) {
            return false;
        }
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            for (StackTraceElement element : cause.getStackTrace()) {
                //不在白名单里面，认为是用户app业务造成的异常
                if (!isStackTraceInIgnorePackages(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断当前堆栈信息是否在白名单里面
     */
    private boolean isStackTraceInIgnorePackages(StackTraceElement element) {
        String name = element.getClassName();
        for (String prefix : mIgnorePackages) {
            //在mIgnorePackages中找，只要有一个符合便认为是系统异常
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        Log.e(TAG, element.toString());
        return false;
    }

    private static boolean hasStackTraceElement(Throwable t, String... traces) {
        return hasStackTraceElement(t, new HashSet<>(Arrays.asList(traces)));
    }

    /**
     * Throwable是否存在该条堆栈
     */
    private static boolean hasStackTraceElement(Throwable t, Set<String> traces) {
        if (t == null || traces == null || traces.isEmpty()) {
            return false;
        }

        for (StackTraceElement element : t.getStackTrace()) {
            if (traces.contains(element.getClassName() + "." + element.getMethodName())) {
                return true;
            }
        }

        return hasStackTraceElement(t.getCause(), traces);
    }

    /**
     * Throwable是否由该种类型异常引发
     * eg:
     * 1java.lang.RuntimeException:android.os.DeadSystemException
     * 2 android.hardware.display.DisplayManagerGlobal.getDisplayInfo(DisplayManagerGlobal.java:201)
     * 3 ......
     * 4 android.os.DeadSystemException:
     * 5 android.hardware.display.DisplayManagerGlobal.getDisplayInfo(DisplayManagerGlobal.java:201)
     * 6 android.view.Display.updateDisplayInfoLocked(Display.java:990)
     * 7 android.view.Display.updateDisplayInfoLocked(Display.java:984)
     * 8 android.view.Display.getRealSize(Display.java:906)
     * 9 com.google.android.gms.ads.omid.library.utils.b.a(:com.google.android.gms.policy_ads_fdr_dynamite@21829003@21829003.311174395.311174395:12)
     * 10 com.google.android.gms.ads.omid.library.walking.e.run(:com.google.android.gms.policy_ads_fdr_dynamite@21829003@21829003.311174395.311174395:24)
     * 11 android.os.Handler.handleCallback(Handler.java:751)
     * 12 android.os.Handler.dispatchMessage(Handler.java:95)
     * 13 android.os.Looper.loop(Looper.java:154)
     * 14 android.app.ActivityThread.main(ActivityThread.java:6816)
     * 15 java.lang.reflect.Method.invoke(Native Method)
     * 16 com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1563)
     * 17 com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1451)
     */
    @SafeVarargs
    private static boolean isCausedBy(Throwable t, Class<? extends Throwable>... causes) {
        return isCausedBy(t, new HashSet<>(Arrays.asList(causes)));
    }

    private static boolean isCausedBy(Throwable t, Set<Class<? extends Throwable>> causes) {
        if (t == null) {
            return false;
        }

        if (causes.contains(t.getClass())) {
            return true;
        }

        return isCausedBy(t.getCause(), causes);
    }

    /**
     * 出现一些系统异常情况，必须中止应用
     */
    private static boolean abort(Throwable t) {
        int pid = Process.myPid();
        String msg = "Process " + pid + " is going to be killed";

        if (t != null) {
            Log.w(TAG, msg, t);
            catchThrowableShow(t);
        } else {
            Log.w(TAG, msg);
        }
        //非正常退出
        Process.killProcess(pid);
        System.exit(1);
        return true;
    }

    /**
     * 回传Throwable
     */
    private static void catchThrowableShow(Throwable t) {
        if (catchThrowable != null)
            catchThrowable.throwable(t);
    }

    /**
     * 获取ActivityThread中的Handler对象
     */
    private static Handler getHandler(Object thread) {
        Handler handler;

        if (thread == null) {
            return null;
        }

        //通过属性变量拿
        handler = getFieldValue(thread, "mH");
        if (handler != null) {
            return handler;
        }

        //通过getHandler()方法返回值拿
        handler = invokeMethod(thread, "getHandler");
        if (handler != null) {
            return handler;
        }

        try {
            //通过内部类匹配ActivityThread中的属性拿
            handler = getFieldValue(thread, Class.forName("android.app.ActivityThread$H"));
            if (handler != null) {
                return handler;
            }
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Main thread handler is inaccessible", e);
        }

        return null;
    }

    /**
     * 获得android.app.ActivityThread对象
     */
    private static Object getActivityThread() {
        Object thread = null;
        try {
            //通过sCurrentActivityThread属性变量拿
            thread = getStaticFieldValue(Class.forName("android.app.ActivityThread"), "sCurrentActivityThread");
        } catch (Throwable t1) {
            Log.w(TAG, t1);
        }
        if (thread != null) {
            return thread;
        }

        try {
            //通过currentActivityThread()静态方法拿：兼容<api 18
            thread = invokeStaticMethod(Class.forName("android.app.ActivityThread"), "currentActivityThread");
        } catch (Throwable e) {
            Log.w(TAG, e);
        }
        if (thread != null) {
            return thread;
        }

        Log.w(TAG, "ActivityThread instance is inaccessible");
        return null;
    }

    /**
     * 给ActivityThread中的mH设置Callback
     */
    boolean hook() {
        if (this.mDelegate != null) {
            Log.w(TAG, "ActivityThread.mH.mCallback has already been hooked by " + this.mDelegate);
        }
        return setFieldValue(this.mHandler, "mCallback", this);
    }

    public void setThrowableListener(CatchThrowable catchThrowable) {
        ActivityThreadCallback.catchThrowable = catchThrowable;
    }
}
