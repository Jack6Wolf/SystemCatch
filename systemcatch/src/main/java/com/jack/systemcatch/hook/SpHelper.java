package com.jack.systemcatch.hook;

import android.os.Message;
import android.util.Log;

import static com.jack.systemcatch.hook.Constants.TAG;

/**
 * @author jack
 * @since 2020/8/10 14:50
 */
public class SpHelper {
    private static final int SERVICE_ARGS = 115;
    private static final int STOP_SERVICE = 116;
    private static final int SLEEPING = 137;
    private static final int STOP_ACTIVITY_SHOW = 103;
    private static final int STOP_ACTIVITY_HIDE = 104;

    public static void remove(Message msg) {
        int what = msg.what;
        switch (what) {
            case SERVICE_ARGS:
            case STOP_SERVICE:
            case SLEEPING:
            case STOP_ACTIVITY_SHOW:
            case STOP_ACTIVITY_HIDE:
                Log.d(TAG, "handle what：" + what);
                SpBlockHelper.beforeSPBlock();
                break;
            default:
        }
    }


}
