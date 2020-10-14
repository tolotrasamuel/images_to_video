package com.tolotra.images_to_video

import android.content.ContentValues
import android.util.Log
import androidx.annotation.NonNull
import com.tolotra.images_to_video.lib.ScreenRecorderWrapper
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MethodCallHandlerImpl(screenRecorder: ScreenRecorderWrapper) : MethodChannel.MethodCallHandler {
    private val screenRecorder = screenRecorder;
    private var channel: MethodChannel? = null

    companion object {
        const val METHOD_CHANNEL_NAME = "images_to_video"
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        Log.d(ContentValues.TAG, "Version 0.0.10")
        if (call.method == "setup") {
            val outputPath: String? = call.argument("outputPath")
            screenRecorder.setup()
        } else if (call.method == "addToVideo") {
            val frame: ByteArray? = call.argument("frame")
            val timestamp: Long? = call.argument("timestamp")
            screenRecorder.addToVideo(frame!!, timestamp!!)
        } else if (call.method == "updateTouchPosition") {
            val x: Int? = call.argument("x")
            val y: Int? = call.argument("y")
            val timestamp: Long? = call.argument("timestamp")
            screenRecorder.setTouchPosition(x!!, y!!, timestamp!!)
        } else if (call.method == "stop") {
            screenRecorder.stop()
        } else {
            result.notImplemented()
        }
    }

    /**
     * Registers this instance as a method call handler on the given
     * `messenger`.
     */
    fun startListening(messenger: BinaryMessenger?) {
        if (channel != null) {
            Log.wtf(ContentValues.TAG, "Setting a method call handler before the last was disposed.")
            stopListening()
        }
        channel = MethodChannel(messenger, METHOD_CHANNEL_NAME)
        channel!!.setMethodCallHandler(this)
    }

    /**
     * Clears this instance from listening to method calls.
     */
    fun stopListening() {
        if (channel == null) {
            Log.d(ContentValues.TAG, "Tried to stop listening when no MethodChannel had been initialized.")
            return
        }
        channel!!.setMethodCallHandler(null)
        channel = null
    }

}