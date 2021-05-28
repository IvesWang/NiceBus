package com.ives.nicebus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.ives.lib_nicebus.NiceBus
import com.ives.lib_nicebus.ThreadMode
import com.ives.lib_nicebus.annotation.NiceEvent

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    val javaSubscriber = JavaSubscriber()
    fun registerCurrent(view: View) {
        NiceBus.getDefault().register(this)
        NiceBus.getDefault().register(javaSubscriber)
    }
    fun unregisterCurrent(view: View) {
        NiceBus.getDefault().unregister(this)
        NiceBus.getDefault().unregister(javaSubscriber)
    }
    fun sendEvent1(view: View) {
        NiceBus.getDefault().post(EventConstant.EVENT_1)
    }
    fun sendEvent2(view: View) {
        NiceBus.getDefault().post(EventConstant.EVENT_2)
    }
    fun sendEventWithParameter(view: View) {
        NiceBus.getDefault().post(EventConstant.EVENT_2, 3)
    }



    @NiceEvent(threadMode = ThreadMode.ASYNC, events = [EventConstant.EVENT_1])
    fun subscribeFunction1(event : String){
        Log.i(TAG, "subscribeFunction1 收到事件:$event")
        runOnUiThread {
            Toast.makeText(baseContext, "subscribeFunction1 收到事件:$event", Toast.LENGTH_SHORT).show()
        }
    }

    @NiceEvent(threadMode = ThreadMode.ASYNC, events = [EventConstant.EVENT_1, EventConstant.EVENT_2])
    fun subscribeFunction2(event : String){
        Log.i(TAG, "subscribeFunction2 收到事件:$event")
        runOnUiThread {
            Toast.makeText(baseContext, "subscribeFunction2 收到事件:$event", Toast.LENGTH_SHORT).show()
        }
    }

    @NiceEvent(threadMode = ThreadMode.ASYNC, events = [EventConstant.EVENT_1, EventConstant.EVENT_2])
    fun subscribeFunctionWithParameter(event : String, age : Int?){
        Log.i(TAG, "subscribeFunctionWithParameter 收到事件:$event,参数:$age")
        runOnUiThread {
            Toast.makeText(baseContext, "收到事件$event,参数:$age", Toast.LENGTH_SHORT).show()
        }
    }
}
