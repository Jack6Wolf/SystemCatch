package com.jack.systemcatch.hook;

/**
 * 通过回调方式返回抓取的异常信息
 *
 * @author jack
 * @since 2020/7/28 16:46
 */
public interface CatchThrowable {
    /**
     * 返回异常信息
     *
     * @param throwable 异常
     */
    void throwable(Throwable throwable);
}
