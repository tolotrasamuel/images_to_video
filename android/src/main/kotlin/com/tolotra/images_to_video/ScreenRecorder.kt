package com.tolotra.images_to_video

import android.app.Activity
import android.content.ContentValues.TAG
import android.graphics.*
import android.util.Log
import android.util.TimingLogger
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


enum class ScreenRecorderStatus {
    ENDED,
    NOT_STARTED,
    RECORDING,
    PAUSED
}

class ScreenRecorder(activity: Activity) {
    private var touchPositions: MutableList<TouchPosition> = mutableListOf()

    // see https://stackoverflow.com/questions/64083313/how-to-use-opengl-with-timer/64083771#64083771
    private var tpe: ExecutorService? = null
    private var i: Int = 0
    private lateinit var imageFiles: List<String>

    private lateinit var recodrer: VideoBuilder
    private var timer: Timer? = null
    private var fps: Int? = null;
    private var activity = activity;
    private var recording: Boolean = false
    private var lastTimestamp: Long? = null
    private var status: ScreenRecorderStatus = ScreenRecorderStatus.NOT_STARTED
    fun getStatus(): ScreenRecorderStatus {
        return status
    }

    fun setup() {
        tpe = Executors.newSingleThreadExecutor()
        tpe?.submit(Runnable {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            this.status = ScreenRecorderStatus.RECORDING
            this.recording = true;
            recodrer = VideoBuilder(activity.applicationContext)
            recodrer.setup();
        })
    }


    fun feed(bytes: ByteArray, timestamp: Long) {
        tpe?.submit(Runnable {

            val bitmap = getRawBitmapFlutter(bytes)
//            this.drawTouch(canvas, timestamp)

            Log.d(TAG, "New Frame to record ${bitmap.width}  ${bitmap.height}")
//            val time = Date().time
            val laspe = this.getTimelapseUs(timestamp)
            recodrer.feed(bitmap, laspe)
        })
    }


    private fun getRawBitmapFlutter(imageData: ByteArray): Bitmap {
        val timings = TimingLogger("FEED_PROFILE", "reading bitmap frame")
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size);
        timings.addSplit("converting bytes to bitmap done")
        timings.dumpToLog()
        return bitmap;
    }

    private fun getRawBitmapFlutterWithCanvas(imageData: ByteArray): Pair<Bitmap, Canvas> {
        val timings = TimingLogger("FEED_PROFILE", "reading bitmap frame")
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size);
        timings.addSplit("converting bytes to bitmap done")
        timings.addSplit("reading bitmap done")
        timings.dumpToLog()
        Log.d(TAG, "Bitmap otained  ${bitmap?.width} ${bitmap?.height} ${bitmap != null}")
        val bmOverlay = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bitmap, Matrix(), null)
//        val canvas = Canvas(bitmap)
        return Pair(bmOverlay, canvas)
    }

    private fun drawTouch(canvas: Canvas, timestamp: Long) {
        touchPositions = cleanTouchPositions(timestamp)
        Log.d(TAG, "Touch position added now count is before draw ${touchPositions.size}");
        val defaultRadius = 50
//        val visibility = touchPositions.map { it.getVisibility() }
//        val timestamps = touchPositions.map { it.timestamp }
//        Log.d(TAG, "Touch position $visibility")
//        Log.d(TAG, "Touch timestamp $timestamp -> $timestamps")

        for (touch in touchPositions) {
            val paint = Paint()
            paint.color = Color.parseColor("yellow");
            paint.alpha = (128 * touch.getVisibility()).toInt()

            val radius = defaultRadius * touch.getVisibility()
            canvas.drawCircle(touch.x, touch.y, radius, paint);
        }
    }

    private fun cleanTouchPositions(now: Long): MutableList<TouchPosition> {
        val newTouchPositions = mutableListOf<TouchPosition>()
//        val now = Date().time
        for (touch in touchPositions) {
            val visibility = touch.updateVisibility(now, 250)
            if (visibility > 0) {
                newTouchPositions.add(touch)
            }
        }
        return newTouchPositions
    }


    fun getTimelapseUs(timestamp: Long): Long {
//        Log.d(TAG, "getting timelpase lastTimestamp=$lastTimestamp timestamp $timestamp")
        if (lastTimestamp == null) lastTimestamp = timestamp
        val elapseMs = timestamp - lastTimestamp!!
        lastTimestamp = timestamp
        return 1_000 * elapseMs
    }


    fun pause() {

    }


    fun stop() {
        Log.d(TAG, "Truing to stop recorder");
        tpe?.submit(Runnable {
            status = ScreenRecorderStatus.ENDED
            recording = false
            timer?.cancel();
            timer?.purge();
            recodrer.finish()
        })
    }

    fun setTouchPosition(x: Int, y: Int, timestamp: Long) {
        this.touchPositions.add(TouchPosition(x.toFloat(), y.toFloat(), timestamp))
//        touchPositions = cleanTouchPositions(timestamp)
        Log.d(TAG, "Touch position added now count is ${touchPositions.size}");

    }
}

class TouchPosition(x: Float, y: Float, timestamp: Long) {
    fun updateVisibility(now: Long, durationMs: Long): Float {
        val elapse = now - timestamp
        val visibility = ((durationMs - elapse).toFloat() / durationMs)
        this.visibility = visibility
        return visibility
    }

    fun getVisibility(): Float {
        // because the touch is added but the frame rendered is in the past
        // so we hide the touch of the future momentarily until the frame catches up
        if (visibility > 1.0) {
            return 0f
        }
        return visibility
    }

    private var visibility: Float = 1.0f
    val y = y
    val x = x
    val timestamp = timestamp

}
