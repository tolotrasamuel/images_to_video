package com.tolotra.images_to_video.lib

import android.app.Activity
import android.util.Log
import com.tolotra.images_to_video.ScreenRecorder
import com.tolotra.images_to_video.ScreenRecorderStatus
import java.util.*

class ScreenRecorderWrapper {
    private var isDebug: Boolean = true;
    private var screenRecorder: ScreenRecorder? = null
    private var activity: Activity? = null

    fun setup(debug: Boolean) {
        isDebug = debug
        val screenRecorder = this.getCurrentScreenRecorder()
        screenRecorder.setup(isDebug)
    }

    fun addToVideo(bytes: ByteArray, timestamp: Long) {
        if(isDebug){
            Log.d("ArrayByteTransfer", "From flutter to Android took ${Date().time - timestamp} ms")
        }
        val screenRecorder = this.getCurrentScreenRecorder()
        screenRecorder.feed(bytes, timestamp)
    }

    fun stop() {
        val screenRecorder = this.getCurrentScreenRecorder()
        screenRecorder.stop()
    }

    fun setTouchPosition(x: Int, y: Int, timestamp: Long) {
        if (screenRecorder?.getStatus() != ScreenRecorderStatus.RECORDING) return
        val screenRecorder = this.getCurrentScreenRecorder()
        screenRecorder.setTouchPosition(x, y, timestamp)
    }


    private fun getCurrentScreenRecorder(): ScreenRecorder {
        if (screenRecorder == null) {
            createNewRecorder()
        }
        if (screenRecorder!!.getStatus() == ScreenRecorderStatus.ENDED) {
            createNewRecorder()
        }
        return screenRecorder!!
    }

    private fun createNewRecorder() {
        screenRecorder = ScreenRecorder(activity!!)
        screenRecorder!!.setup(isDebug)
    }

    fun setActivity(activity: Activity?) {
        this.activity = activity;
    }
}