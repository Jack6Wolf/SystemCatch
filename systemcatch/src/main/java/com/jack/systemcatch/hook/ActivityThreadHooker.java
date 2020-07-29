package com.jack.systemcatch.hook;

import android.util.Log;

import static com.jack.systemcatch.hook.Constants.TAG;

/**
 * ActivityThread$H是继承自Handler.而Handler支持通过Handler.Callback来改变其自行的行为，
 * 所以，只要通过反射为ActivityThread.mH.mCallback设置一个新的Handler.Callback
 * 然后在这个Handler.Callback中将系统异常catch住就行了。
 *
 * @author jack
 * @since 2020/7/28 16:50
 */
public class ActivityThreadHooker {

    private static volatile boolean hooked;
    private static ActivityThreadCallback callback;

    /**
     * 全局反射try-catch ActivityThread用于捕获由系统引起的异常
     * eg:com.startimes.hooktest.MainActivity,com.startimes.hook.
     * (只要一个异常的所有堆栈信息，都以这2个字符串中的一个开头，则不抛异常会被try-catch)
     *
     * @param ignorePackages 白名单设置(全路径)，多个路径用逗号隔离
     */
    public static void hook(final String ignorePackages) {
        if (hooked) {
            return;
        }

        try {
            final String pkgs = null == ignorePackages ? "" : ignorePackages.trim();
            callback = new ActivityThreadCallback(pkgs.split("\\s*,\\s*"));
            //核心给mH设置自定义的Callback
            if (!(hooked = callback.hook())) {
                Log.i(TAG, "Hook ActivityThread.mH.mCallback failed");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Hook ActivityThread.mH.mCallback failed", t);
        }

        if (hooked) {
            Log.i(TAG, "Hook ActivityThread.mH.mCallback success!");
        }
    }

    /**
     * 添加Throwable监听
     */
    public static void addThrowableListener(CatchThrowable catchThrowable) {
        if (callback != null)
            callback.setThrowableListener(catchThrowable);
        else
            Log.w(TAG, "Add ThrowableListener failed!");
    }
}
