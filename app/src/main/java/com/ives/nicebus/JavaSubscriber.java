package com.ives.nicebus;

import android.util.Log;

import com.ives.lib_nicebus.ThreadMode;
import com.ives.lib_nicebus.annotation.NiceEvent;

/**
 * @author wangziguang
 * @date 2021/5/28
 * @description
 */
public class JavaSubscriber {
    private char aChar = 2;

    @NiceEvent(threadMode = ThreadMode.ASYNC, events = {EventConstant.EVENT_1,EventConstant.EVENT_2})
    void subscribeFunctionWithParameter(String event, int age){
        Log.i("JavaSubscriber", "subscribeFunctionWithParameter 收到事件:"+event+",参数:"+age);
    }
}
