package com.tolotra.images_to_video

import android.content.ContentValues
import androidx.annotation.NonNull
import com.tolotra.images_to_video.MethodCallHandlerImpl
import com.tolotra.images_to_video.lib.ScreenRecorderWrapper
import io.flutter.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/** ImagesToVideoPlugin */
public class ImagesToVideoPlugin: FlutterPlugin, ActivityAware {
   private var activityBinding: ActivityPluginBinding? = null
    private lateinit var methodCallHandler: MethodCallHandlerImpl
    private lateinit var screenRecorder: ScreenRecorderWrapper
    private lateinit var registrar: PluginRegistry.Registrar

    private lateinit var channel: MethodChannel

    // Old way
    // must be kept https://flutter.dev/docs/development/packages-and-plugins/plugin-api-migration
    companion object {
        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val instance = ImagesToVideoPlugin()
            instance.registrar = registrar
            instance.screenRecorder = ScreenRecorderWrapper()
            instance.screenRecorder.setActivity(registrar.activity())
//            instance.setup()
            instance.initInstance(registrar.messenger())
        }
    }

    // New Way
    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        screenRecorder = ScreenRecorderWrapper()
//        screenRecorder.setRenderer(binding.flutterEngine.renderer)
        this.initInstance(binding.getBinaryMessenger())
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // Activity aware IMPL
    override fun onDetachedFromActivity() {
        detachActivity();
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        attachToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachActivity()
    }


    /// HER PRIVATE PARTS
    private fun initInstance(binaryMessenger: BinaryMessenger) {
        methodCallHandler = MethodCallHandlerImpl(screenRecorder)
        methodCallHandler.startListening(binaryMessenger)
    }


    private fun setup() {
        if (registrar != null) {
            Log.d(ContentValues.TAG, "Screen Recorder Register V1")
            // V1 embedding setup for activity listeners.
//      registrar.addActivityResultListener(screenRecorder)
        } else {
            Log.d(ContentValues.TAG, "Screen Recorder Register V2")
            // V2 embedding setup for activity listeners.
//      activityBinding.addActivityResultListener(screenRecorder)
        }
    }

    private fun attachToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        try {
            screenRecorder.setActivity(binding.activity)
            setup()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detachActivity() {
//    activityBinding.removeActivityResultListener(screenRecorder)
        activityBinding = null
    }
}
