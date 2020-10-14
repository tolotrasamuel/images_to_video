//https://www.sisik.eu/images-to-video

package com.tolotra.images_to_video

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.media.*
import android.opengl.*
import android.util.Log
import android.util.TimingLogger
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*


class VideoBuilder(applicationContext: Context) {


    private lateinit var outputStream: FileOutputStream
    private var frameId: Long = 0
    private lateinit var muxer: MediaMuxer
    private lateinit var glTool: OverlayRenderer
    private lateinit var encoder: MediaCodec
    private lateinit var outputFileName: String
    private var context = applicationContext
    private var trackIndex: Int = 0
    private lateinit var bufferInfo: MediaCodec.BufferInfo
    private var eglContext: EGLContext? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglSurface: EGLSurface? = null
    private lateinit var surface: Surface


    val timeoutUs = 10000L
    val frameRate = 5
    var presentationTimeUs: Long = 0


    fun setup() {
        encoder = createEncoder()
        initInputSurface(encoder)
        encoder.start()

        val now = Date().time
        outputFileName = "tolotra-screen-recoder-$now.mp4"
        val outVideoFilePath = getScreenshotPath(outputFileName)
//        val outVideoFilePathH264 = getScreenshotPath("tolotra-screen-recoder-h264-$now.h264")
//        outputStream = FileOutputStream(outVideoFilePathH264);
        muxer = MediaMuxer(outVideoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//        muxer = MediaMuxer(outVideoFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)

        glTool = OverlayRenderer()
        glTool.initGl()
    }

    /**
     * Laspse is the duration between the current frame and the previous frame
     */
    fun feed(bitmap: Bitmap, timelapse: Long) {

        frameId++
        Log.d("FEED_PROFILE", "feed frame:$frameId")
        val timings = TimingLogger("FEED_PROFILE", "feed frame:$frameId")
        // Get encoded data and feed it to muxer
        drainEncoder(encoder, muxer, false, timelapse)

        timings.addSplit("drainEncoder done");
        // Render the bitmap/texture with OpenGL here
        glTool.render(bitmap)
        timings.addSplit("render done");

        // Set timestamp with EGL extension
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeUs * 1000)

        // Feed encoder with next frame produced by OpenGL
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        timings.dumpToLog();
    }

    fun finish() {
        Log.d(TAG, "Finishing...")

        // Drain last encoded data and finalize the video file
        drainEncoder(encoder, muxer, true, 0)
        Log.d(TAG, "cleaning up")
        _cleanUp(encoder, muxer)
        Log.d(TAG, "moving file from temp to permanent")

        var outVideoFilePath = getScreenshotPath(outputFileName)
        val file = File(outVideoFilePath)

        val file_size = (file.length() / 1024).toString().toInt()
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(outVideoFilePath)
        val width =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val rotation =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

        val bitRate =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        val duration =
                java.lang.Long.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000

        Log.d("Result", "bitrate $bitRate duration $duration  fileSize $file_size ")

        val newOutVideoFilePath = getScreenshotPath(outputFileName, false)
        val newFile = File(newOutVideoFilePath)
        file.renameTo(newFile)
    }

    fun getScreenshotPath(fileName: String, temp: Boolean = true): String {
        val f = context.externalCacheDir
        val externalDir: String = f!!.path;
        var sDir: String;

        if (temp) {
            sDir = externalDir + File.separator + "Screen Recorder" + File.separator + "temp";
        } else {
            sDir = externalDir + File.separator + "Screen Recorder" ;

        }
        val dir = File(sDir);
        val dirPath: String;
        if (dir.exists() || dir.mkdir()) {
            dirPath = sDir + File.separator + fileName;
        } else {
            dirPath = externalDir + File.separator + fileName
        }
        Log.d("Mp4 file path", "Path: $dirPath")

        return dirPath;
    } //


    fun createEncoder(): MediaCodec {

        bufferInfo = MediaCodec.BufferInfo()
        val MIME = "video/avc"
//        val MIME = "video/mpeg2"
        val encoder = MediaCodec.createEncoderByType(MIME)
        val width = 320
        val heigh = 512
        val format = MediaFormat.createVideoFormat(MIME, width, heigh)
        format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 350_000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 45)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        trackIndex = -1;
        return encoder
    }

    fun drainEncoder(
            encoder: MediaCodec,
            muxer: MediaMuxer,
            endOfStream: Boolean,
            timelapseUs: Long
    ) {
        if (endOfStream)
            encoder.signalEndOfInputStream()

        while (true) {
            val outBufferId = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)

            if (outBufferId >= 0) {
//                encoder.queueInputBuffer(outBufferId, 0, 0, presentationTimeUs, 0)
                val encodedBuffer = encoder.getOutputBuffer(outBufferId)

                // MediaMuxer is ignoring KEY_FRAMERATE, so I set it manually here
                // to achieve the desired frame rate
                bufferInfo.presentationTimeUs = presentationTimeUs
                if (encodedBuffer != null) {
                    muxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)

                    // h264
//                    _saveH264(encodedBuffer)
                    // end h264
                }

                presentationTimeUs += timelapseUs

                encoder.releaseOutputBuffer(outBufferId, false)

                // Are we finished here?
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    break
            } else if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream)
                    break

                // End of stream, but still no output available. Try again.
            } else if (outBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(encoder.outputFormat)
                muxer.start()
            }
        }
    }

    private fun _saveH264(encodedBuffer: ByteBuffer) {
        encodedBuffer.position(bufferInfo.offset)
        encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)

        val data = ByteArray(bufferInfo.size)
        encodedBuffer[data]
        encodedBuffer.position(bufferInfo.offset)
        outputStream.write(data)
    }

    private fun initInputSurface(encoder: MediaCodec) {

        val surface = encoder.createInputSurface()

        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY)
            throw RuntimeException(
                    "eglDisplay == EGL14.EGL_NO_DISPLAY: "
                            + GLUtils.getEGLErrorString(EGL14.eglGetError())
            )

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1))
            throw RuntimeException("eglInitialize(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))

        val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val nConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, nConfigs, 0)

        var err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val ctxAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        )
        val eglContext =
                EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)

        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        val surfaceAttribs = intArrayOf(
                EGL14.EGL_NONE
        )
        val eglSurface =
                EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
        err = EGL14.eglGetError()
        if (err != EGL14.EGL_SUCCESS)
            throw RuntimeException(GLUtils.getEGLErrorString(err))

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext))
            throw RuntimeException("eglMakeCurrent(): " + GLUtils.getEGLErrorString(EGL14.eglGetError()))


        this.eglSurface = eglSurface
        this.eglDisplay = eglDisplay
        this.eglContext = eglContext
        this.surface = surface
    }

    private fun _cleanUp(encoder: MediaCodec, muxer: MediaMuxer) {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay);
        }
        surface?.release();
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE

        Log.d(TAG, "Releasing EGL...")

        encoder.stop()
        encoder.release()

        Log.d(TAG, "Encoder stopped...")

        muxer.stop()
        muxer.release()

        Log.d(TAG, "Muxer stopped...")

        // h264
//        outputStream.close()
        // end h264
    }


}

class OverlayRenderer() {

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private val vertexShaderCode =
            "precision highp float;\n" +
                    "attribute vec3 vertexPosition;\n" +
                    "attribute vec2 uvs;\n" +
                    "varying vec2 varUvs;\n" +
                    "uniform mat4 mvp;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "\tvarUvs = uvs;\n" +
                    "\tgl_Position = mvp * vec4(vertexPosition, 1.0);\n" +
                    "}"

    private val fragmentShaderCode =
            "precision mediump float;\n" +
                    "\n" +
                    "varying vec2 varUvs;\n" +
                    "uniform sampler2D texSampler;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\t\n" +
                    "\tgl_FragColor = texture2D(texSampler, varUvs);\n" +
                    "}"

    private var vertices = floatArrayOf(
            // x, y, z, u, v
            -1.0f, -1.0f, 0.0f, 0f, 0f,
            -1.0f, 1.0f, 0.0f, 0f, 1f,
            1.0f, 1.0f, 0.0f, 1f, 1f,
            1.0f, -1.0f, 0.0f, 1f, 0f
    )

    private var indices = intArrayOf(
            2, 1, 0, 0, 3, 2
    )

    private var program: Int = 0
    private var vertexHandle: Int = 0
    private var bufferHandles = IntArray(2)
    private var uvsHandle: Int = 0
    private var mvpHandle: Int = 0
    private var samplerHandle: Int = 0
    private val textureHandle = IntArray(1)


    val viewportWidth = 320
    val viewportHeight = 486


    var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
    }

    var indexBuffer: IntBuffer = ByteBuffer.allocateDirect(indices.size * 4).run {
        order(ByteOrder.nativeOrder())
        asIntBuffer().apply {
            put(indices)
            position(0)
        }
    }

    fun render(bitmap: Bitmap) {

        Log.d("Bitmap", "width ${bitmap.width} height ${bitmap.height}")


// Prepare some transformations
        val mvp = FloatArray(16)
        Matrix.setIdentityM(mvp, 0)
        Matrix.scaleM(mvp, 0, 1f, -1f, 1f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(0f, 0f, 0f, 0f)

        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)

        GLES20.glUseProgram(program)

// Pass transformations to shader
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)

// Prepare texture for drawing
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)

// Pass the Bitmap to OpenGL here
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
        )

// Prepare buffers with vertices and indices & draw
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandles[0])
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferHandles[1])

        GLES20.glEnableVertexAttribArray(vertexHandle)
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 4 * 5, 0)

        GLES20.glEnableVertexAttribArray(uvsHandle)
        GLES20.glVertexAttribPointer(uvsHandle, 2, GLES20.GL_FLOAT, false, 4 * 5, 3 * 4)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0)
    }


    fun initGl() {
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also { shader ->
            GLES20.glShaderSource(shader, vertexShaderCode)
            GLES20.glCompileShader(shader)
        }

        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also { shader ->
            GLES20.glShaderSource(shader, fragmentShaderCode)
            GLES20.glCompileShader(shader)
        }

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            vertexHandle = GLES20.glGetAttribLocation(it, "vertexPosition")
            uvsHandle = GLES20.glGetAttribLocation(it, "uvs")
            mvpHandle = GLES20.glGetUniformLocation(it, "mvp")
            samplerHandle = GLES20.glGetUniformLocation(it, "texSampler")
        }

        // Initialize buffers
        GLES20.glGenBuffers(2, bufferHandles, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandles[0])
        GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                vertices.size * 4,
                vertexBuffer,
                GLES20.GL_DYNAMIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferHandles[1])
        GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indices.size * 4,
                indexBuffer,
                GLES20.GL_DYNAMIC_DRAW
        )

        // Init texture handle
        GLES20.glGenTextures(1, textureHandle, 0)

        // Ensure I can draw transparent stuff that overlaps properly
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
}