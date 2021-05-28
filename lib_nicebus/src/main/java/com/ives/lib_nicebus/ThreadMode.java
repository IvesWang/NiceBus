package com.ives.lib_nicebus;

/**
 * @author wangziguang
 * @date 2021/5/28
 * @description 执行的线程，可以在订阅端声明，也支持在发送端决定，两端都设置则以发送端为准。
 * NiceBus实现该需求只需在main线程之外再提供两个线程池：一个执行并行消息，一个执行阻塞消息。
 */
public enum ThreadMode {
    ASYNC,// 与当前线程异步，多个消息立即执行
    MAIN,
    POST,// 非主线程执行，多个消息立即执行
    ASYNC_ORDER,// 多个消息阻塞执行
    POST_ORDER// 多个消息阻塞执行
}
