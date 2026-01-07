@file:Suppress("DEPRECATION")

package uni.lv.yuedu.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.util.Log
import com.visiontalk.vtbrsdk.base.AbstractVTCameraCtrl
import java.io.IOException

/**
 * 相机回调类
 * 负责处理Camera API的调用和预览帧回调
 * 注意：使用了已废弃的Camera API，但为了兼容SDK需要，保留使用
 */
class CameraCallBack(private val mContext: Context) : Camera.PreviewCallback {
    
    companion object {
        private const val TAG = "CameraCallBack"
        const val MAGIC_TEXTURE_ID = 10
        
        /**
         * 设置相机显示方向
         * 根据设备方向和相机朝向调整预览画面方向
         * @param context Context实例（可以是Activity或Application）
         * @param cameraId 相机ID
         * @param camera 相机对象
         */
        fun setCameraDisplayOrientation(context: Context, cameraId: Int, camera: Camera) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, info)
            
            // 获取设备当前旋转角度
            // 通过Context获取WindowManager，兼容Activity和Application
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            val rotation = windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
            
            val degrees = when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            // 计算最终的显示角度
            val result = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 前置摄像头需要镜像翻转
                (info.orientation + degrees) % 360
            } else {
                // 后置摄像头
                (info.orientation - degrees + 360) % 360
            }
            
            Log.d(TAG, "Camera display orientation: $result")
            camera.setDisplayOrientation(result)
        }
    }

    private var mSurfaceHolder: SurfaceHolder? = null
    private var mCamera: Camera? = null
    private var mCameraId: Int = 0
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mICameraPreviewCb: AbstractVTCameraCtrl.ICameraPreviewCallback? = null
    private var mCameraThread: CameraHandlerThread? = null
    private var mPreviewBuff: ByteArray? = null
    private var mPreviewWidth: Int = 0
    private var mPreviewHeight: Int = 0

    /**
     * 使用SurfaceTexture打开相机
     * @param cameraId 相机ID（0为后置，1为前置）
     * @param previewWidth 预览宽度
     * @param previewHeight 预览高度
     * @param previewCallback 预览回调接口
     * @param surfaceTexture 预览SurfaceTexture
     * @return 是否打开成功
     */
    fun openCamera(
        cameraId: Int,
        previewWidth: Int,
        previewHeight: Int,
        previewCallback: AbstractVTCameraCtrl.ICameraPreviewCallback,
        surfaceTexture: SurfaceTexture
    ): Boolean {
        mICameraPreviewCb = previewCallback
        mSurfaceTexture = surfaceTexture
        return open(cameraId, previewWidth, previewHeight)
    }

    /**
     * 使用SurfaceHolder打开相机
     * @param cameraId 相机ID（0为后置，1为前置）
     * @param previewWidth 预览宽度
     * @param previewHeight 预览高度
     * @param previewCallback 预览回调接口
     * @param surfaceHolder 预览SurfaceHolder
     * @return 是否打开成功
     */
    fun openCamera(
        cameraId: Int,
        previewWidth: Int,
        previewHeight: Int,
        previewCallback: AbstractVTCameraCtrl.ICameraPreviewCallback,
        surfaceHolder: SurfaceHolder
    ): Boolean {
        mICameraPreviewCb = previewCallback
        mSurfaceHolder = surfaceHolder
        return open(cameraId, previewWidth, previewHeight)
    }

    /**
     * 打开相机的核心方法
     * @param cameraId 相机ID
     * @param previewWidth 预览宽度
     * @param previewHeight 预览高度
     * @return 是否打开成功
     */
    private fun open(cameraId: Int, previewWidth: Int, previewHeight: Int): Boolean {
        mCameraThread = CameraHandlerThread("Camera open thread")
        synchronized(mCameraThread!!) {
            val startTime = System.currentTimeMillis()
            
            // 检测可用相机数量，如果只有一个则强制使用ID 0
            val cameraNum = Camera.getNumberOfCameras()
            val finalCameraId = if (cameraNum == 1) 0 else cameraId
            
            mCameraId = finalCameraId
            mPreviewWidth = previewWidth
            mPreviewHeight = previewHeight
            
            Log.i(TAG, "cameraId = $finalCameraId, width = $previewWidth, height = $previewHeight")
            
            // 在后台线程打开相机
            mCameraThread!!.openCamera(finalCameraId)
            
            Log.d(TAG, "openCamera time = ${System.currentTimeMillis() - startTime}ms")
        }

        // 初始化相机参数
        val result = initializeCamera(previewWidth, previewHeight, this)
        return result
    }

    /**
     * 关闭相机
     * 释放相机资源
     */
    fun closeCamera() {
        Log.w(TAG, "closeCamera")
        
        // 停止预览并释放相机
        mCamera?.let { camera ->
            try {
                camera.stopPreview()
                camera.setPreviewCallback(null)
                camera.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mCamera = null
        }
        
        // 退出相机线程
        mCameraThread?.let { thread ->
            thread.quitSafely()
            mCameraThread = null
        }
    }

    /**
     * 相机预览帧回调
     * 当相机有新的预览帧时会回调此方法
     * @param frame 预览帧数据（NV21格式）
     * @param camera 相机对象
     */
    override fun onPreviewFrame(frame: ByteArray?, camera: Camera?) {
        // 将预览帧数据传递给识别SDK
        mICameraPreviewCb?.onPreview(frame, mPreviewWidth, mPreviewHeight)
        
        // 将缓冲区添加回相机，以便重复使用
        mPreviewBuff?.let { buff ->
            camera?.addCallbackBuffer(buff)
        }
    }

    /**
     * 相机Handler线程
     * 用于在后台线程中打开相机，避免阻塞主线程
     */
    private inner class CameraHandlerThread(name: String) : HandlerThread(name) {
        private var mHandler: Handler? = null

        init {
            start()
            mHandler = Handler(looper)
        }

        /**
         * 通知相机已打开
         */
        @Synchronized
        private fun notifyCameraOpened() {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as java.lang.Object).notify()
        }

        /**
         * 在后台线程中打开相机
         * @param cameraId 相机ID
         */
        @Synchronized
        fun openCamera(cameraId: Int) {
            mHandler?.post {
                try {
                    mCamera = Camera.open(cameraId)
                    notifyCameraOpened()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open camera", e)
                    notifyCameraOpened()
                }
            }
            
            try {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (this as java.lang.Object).wait()
            } catch (e: InterruptedException) {
                Log.w(TAG, "wait was interrupted", e)
            }
        }
    }

    /**
     * 初始化相机参数
     * 设置预览尺寸、格式、对焦模式等
     * @param previewWidth 预览宽度
     * @param previewHeight 预览高度
     * @param previewCallback 预览回调
     * @return 是否初始化成功
     */
    private fun initializeCamera(
        previewWidth: Int,
        previewHeight: Int,
        previewCallback: Camera.PreviewCallback
    ): Boolean {
        if (mCamera == null) {
            Log.e(TAG, "Camera is null, cannot initialize")
            return false
        }

        try {
            // 获取相机参数
            val params = mCamera!!.parameters
            
            // 设置预览尺寸
            params.setPreviewSize(previewWidth, previewHeight)
            
            // 设置预览格式为NV21（YUV420格式，Android相机常用格式）
            params.previewFormat = ImageFormat.NV21
            
            // 设置自动对焦模式（如果支持）
            val focusModes = params.supportedFocusModes
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            }
            
            // 应用参数
            mCamera!!.parameters = params

            Log.d(TAG, "initializeCamera --> width: ${params.previewSize.width}, height: ${params.previewSize.height}")

            // 计算预览缓冲区大小
            val bitsPerPixel = ImageFormat.getBitsPerPixel(params.previewFormat)
            mPreviewBuff = ByteArray(previewWidth * previewHeight * bitsPerPixel / 8)

            // 设置预览显示目标
            when {
                mSurfaceHolder != null -> {
                    mCamera!!.setPreviewDisplay(mSurfaceHolder)
                }
                mSurfaceTexture != null -> {
                    mSurfaceTexture?.let { texture ->
                        mCamera!!.setPreviewTexture(texture)
                    }
                }
                else -> {
                    Log.e(TAG, "No preview surface available")
                    return false
                }
            }

            // 设置相机显示方向
            setCameraDisplayOrientation(mContext, mCameraId, mCamera!!)

            // 设置预览回调（使用缓冲模式提高性能）
            mPreviewBuff?.let { buff ->
                mCamera!!.addCallbackBuffer(buff)
                mCamera!!.setPreviewCallbackWithBuffer(previewCallback)
            }

            // 开始预览
            mCamera!!.startPreview()
            
            Log.d(TAG, "Camera preview started successfully")
            return true
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize camera", e)
            return false
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to start preview", e)
            return false
        }
    }

}

