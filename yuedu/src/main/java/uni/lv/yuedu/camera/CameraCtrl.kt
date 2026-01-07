package uni.lv.yuedu.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import android.util.Log
import com.visiontalk.vtbrsdk.base.AbstractVTCameraCtrl

/**
 * 相机控制类
 * 实现AbstractVTCameraCtrl接口，为识别SDK提供相机控制能力
 */
class CameraCtrl(private val context: Context) : AbstractVTCameraCtrl() {

    companion object {
        private const val TAG = "CameraCtrl"
    }

    // 相机回调对象，负责实际的相机操作
    private val mCameraCallBack = CameraCallBack(context)

    // 预览SurfaceTexture（用于TextureView）
    private var mSurfaceTexture: SurfaceTexture = SurfaceTexture(CameraCallBack.MAGIC_TEXTURE_ID)

    // 预览SurfaceHolder（用于SurfaceView）
    private var mSurfaceHolder: SurfaceHolder? = null

    /**
     * 设置预览SurfaceTexture
     * @param surfaceTexture SurfaceTexture对象
     */
    fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        this.mSurfaceTexture = surfaceTexture
    }

    /**
     * 设置预览SurfaceHolder
     * @param holder SurfaceHolder对象
     */
    fun setSurfaceHolder(holder: SurfaceHolder?) {
        this.mSurfaceHolder = holder
    }

    /**
     * 打开相机
     * SDK会调用此方法来打开相机并开始预览
     * @param cameraId 相机ID（0为后置，1为前置）
     * @param previewWidth 预览宽度
     * @param previewHeight 预览高度
     * @param previewCallback 预览帧回调接口，SDK通过此接口接收相机预览帧数据
     */
    override fun openCamera(
        cameraId: Int,
        previewWidth: Int,
        previewHeight: Int,
        previewCallback: ICameraPreviewCallback
    ) {
        synchronized(this) {
            Log.d(TAG, "openCamera: cameraId=$cameraId, width=$previewWidth, height=$previewHeight")
            
            // 根据是否有SurfaceHolder来决定使用哪种预览方式
            if (mSurfaceHolder != null) {
                // 使用SurfaceView（SurfaceHolder）
                mCameraCallBack.openCamera(
                    cameraId,
                    previewWidth,
                    previewHeight,
                    previewCallback,
                    mSurfaceHolder!!
                )
            } else {
                // 使用TextureView（SurfaceTexture）
                mCameraCallBack.openCamera(
                    cameraId,
                    previewWidth,
                    previewHeight,
                    previewCallback,
                    mSurfaceTexture
                )
            }
        }
    }

    /**
     * 获取自定义预览宽度
     * 返回0表示使用SDK默认的预览宽度
     * @return 自定义预览宽度，0表示使用默认值
     */
    override fun getCustomPreviewWidth(): Int {
        return 0
    }

    /**
     * 获取自定义预览高度
     * 返回0表示使用SDK默认的预览高度
     * @return 自定义预览高度，0表示使用默认值
     */
    override fun getCustomPreviewHeight(): Int {
        return 0
    }

    /**
     * 关闭相机
     * SDK会调用此方法来关闭相机并释放资源
     */
    override fun closeCamera() {
        synchronized(this) {
            Log.d(TAG, "closeCamera")
            mCameraCallBack.closeCamera()
        }
    }
}



