package com.jack.systemcatch.hook;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jack.systemcatch.hook.Constants.TAG;

/**
 * author heyang
 * email  512573717@qq.com
 * created 2019/6/13  上午9:04.
 */
public class SpBlockHelper {
    private static String CLASS_QUEUED_WORK = "android.app.QueuedWork";
    /**
     * 防止重复反射
     */
    private static volatile boolean init = false;
    /**
     * 8.0 以后是这个
     */
    private static String FIELD_PENDING_FINISHERS_VERSION8 = "sFinishers";
    private static LinkedList<Runnable> sPendingWorkFinishersVersion8 = null;

    /**
     * 8.0以前是这个
     */
    private static String FIELD_PENDING_FINISHERS = "sPendingWorkFinishers";
    private static ConcurrentLinkedQueue<Runnable> sPendingWorkFinishers = null;


    public static void beforeSPBlock() {
        if (!init) {
            getPendingWorkFinishers();
            init = true;
        }
        if (sPendingWorkFinishers != null) {
            sPendingWorkFinishers.clear();
            Log.d(TAG, "sPendingWorkFinishers clear Success!");
        }
        if (sPendingWorkFinishersVersion8 != null) {
            sPendingWorkFinishersVersion8.clear();
            Log.d(TAG, "sFinishers clear Success!");
        }

    }


    private static void getPendingWorkFinishers() {
        try {
            Class<?> clazz = Class.forName(CLASS_QUEUED_WORK);
            Field field = clazz.getDeclaredField(Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? FIELD_PENDING_FINISHERS : FIELD_PENDING_FINISHERS_VERSION8);
            field.setAccessible(true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                sPendingWorkFinishers = (ConcurrentLinkedQueue<Runnable>) field.get(null);
                Log.d(TAG, "sPendingWorkFinishers get Success!");
            } else {
                sPendingWorkFinishersVersion8 = (LinkedList<Runnable>) field.get(null);
                Log.d(TAG, "sFinishers get Success!");
            }
        } catch (Throwable e) {
            Log.w(TAG, e);
        }

    }
}
