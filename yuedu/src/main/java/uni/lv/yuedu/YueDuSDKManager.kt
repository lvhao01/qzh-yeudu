package uni.lv.yuedu

import android.content.Context
import android.util.Log
import uni.lv.yuedu.callback.AuthCallback
import uni.lv.yuedu.callback.DeviceLogoutCallback
import uni.lv.yuedu.callback.RecognizeCallback
import uni.lv.yuedu.callback.DownloadStateCallback
import uni.lv.yuedu.callback.AudioStateCallback
import uni.lv.yuedu.camera.CameraCtrl
import uni.lv.yuedu.model.BookInfo
import uni.lv.yuedu.model.RecognizeResult
import com.visiontalk.basesdk.VTBaseSDKManagerExt
import com.visiontalk.basesdk.api.mode.VTBRBookDataModel
import com.visiontalk.vtbrsdk.VTBRSDKManager
import com.visiontalk.vtbrsdk.listener.IRecognizeListener
import com.visiontalk.vtbrsdk.listener.IDownloadListener
import com.visiontalk.vtbrsdk.listener.IAudioStateListener
import com.visiontalk.vtbrsdk.audio.VTAudioCtrl
import com.visiontalk.vtloginsdk.login.callback.QRCodeAuthCallback

/**
 * 阅读SDK管理器
 * 提供授权认证、识别、朗读等核心功能
 */
object YueDuSDKManager {
    private const val TAG = "YueDuSDKManager"
    
    // SDK初始化标志
    private var isInitialized = false
    
    // 识别SDK管理器实例
    private var vtbrSDKManager: VTBRSDKManager? = null
    
    // 当前上下文
    private var context: Context? = null
    
    // 当前识别回调
    private var currentRecognizeCallback: RecognizeCallback? = null
    
    // 当前相机控制器
    private var currentCameraCtrl: CameraCtrl? = null
    
    // 设备被挤下线回调
    private var deviceLogoutCallback: DeviceLogoutCallback? = null

    // 下载状态回调
    private var downloadStateCallback: DownloadStateCallback? = null

    // 播放状态回调
    private var audioStateCallback: AudioStateCallback? = null
    
    // 识别SDK初始化状态标志
    private var isRecognizeSDKInitialized = false
    
    // 待执行的识别操作（在初始化完成后执行）
    private var pendingRecognizeOperation: (() -> Unit)? = null

    // 当前播放的书本/页面信息（用于重新播放）
    private var currentBookId: Int = -1
    private var currentPageId: Int = -1
    private var currentPageType: Int = -1

    /**
     * 初始化SDK
     * 注意：此方法会立即执行，但初始化操作本身可能较耗时
     * 建议在后台线程调用，或使用异步初始化方法
     * @param context 上下文
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            // 使用applicationContext避免内存泄漏
            this.context = context.applicationContext
            // 初始化基础SDK（这个操作可能较耗时，但必须在主线程调用）
            VTBaseSDKManagerExt.getInstance().initialize(context)
            isInitialized = true
            Log.d(TAG, "SDK初始化完成")
        }
    }
    
    /**
     * 异步初始化SDK
     * 在后台线程执行初始化，避免阻塞主线程
     * @param context 上下文
     * @param callback 初始化完成回调（在主线程执行），参数为(成功标志, 错误信息)
     */
    fun initializeAsync(context: Context, callback: ((Boolean, String?) -> Unit)? = null) {
        if (isInitialized) {
            // 已经初始化，直接回调成功
            callback?.invoke(true, null)
            return
        }
        
        // 在后台线程执行初始化
        Thread {
            try {
                // 使用applicationContext避免内存泄漏
                this.context = context.applicationContext
                // 初始化基础SDK
                VTBaseSDKManagerExt.getInstance().initialize(context)
                isInitialized = true
                Log.d(TAG, "SDK异步初始化完成")
                
                // 在主线程执行回调
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback?.invoke(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SDK异步初始化失败", e)
                // 在主线程执行回调，传递错误信息
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback?.invoke(false, e.message)
                }
            }
        }.start()
    }

    /**
     * 初始化识别SDK
     * 需要先调用initialize方法初始化基础SDK
     * @param license 授权凭证（从授权接口获取）
     */
    private fun initializeRecognizeSDK(license: String) {
        context?.let { ctx ->
            if (vtbrSDKManager == null) {
                // 创建识别SDK管理器实例，640x480是预览分辨率
                vtbrSDKManager = VTBRSDKManager(ctx, 640, 480)
                
                // 初始化识别SDK
                vtbrSDKManager?.init()
                
                // 设置下载监听器
                vtbrSDKManager?.setDownloadListener(object : IDownloadListener {
                    override fun onDownloadPrepare(downloadId: Int, isForeground: Boolean) {
                        Log.d(TAG, "下载准备: downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onDownloadPrepare(downloadId, isForeground)
                    }

                    override fun onDownloadStart(downloadId: Int, isForeground: Boolean) {
                        Log.d(TAG, "下载开始: downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onDownloadStart(downloadId, isForeground)
                    }

                    override fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean) {
                        Log.d(TAG, "下载中: progress=$progress%, downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onDownloading(progress, downloadId, isForeground)
                    }

                    override fun onDownloadEnd(downloadId: Int, isForeground: Boolean) {
                        Log.d(TAG, "下载结束: downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onDownloadEnd(downloadId, isForeground)
                    }

                    override fun onDownloadFail(downloadId: Int, isForeground: Boolean) {
                        Log.w(TAG, "下载失败: downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onDownloadFail(downloadId, isForeground)
                    }

                    override fun onUnzipStart(downloadId: Int, isForeground: Boolean) {
                        Log.d(TAG, "解压开始: downloadId=$downloadId, isForeground=$isForeground")
                        downloadStateCallback?.onUnzipStart(downloadId, isForeground)
                    }

                    override fun onUnzipComplete(downloadId: Int, isForeground: Boolean): Boolean {
                        Log.d(TAG, "解压完成: downloadId=$downloadId, isForeground=$isForeground")
                        return downloadStateCallback?.onUnzipComplete(downloadId, isForeground) ?: false
                    }

                    override fun onUnzipError(downloadId: Int, errMsg: String, isForeground: Boolean) {
                        Log.e(TAG, "解压错误: downloadId=$downloadId, errMsg=$errMsg, isForeground=$isForeground")
                        downloadStateCallback?.onUnzipError(downloadId, errMsg, isForeground)
                    }
                })
                
                // 设置播放状态监听器
                vtbrSDKManager?.setAudioStateListener(object : IAudioStateListener {
                    override fun onAudioStart(type: Int, id: Int) {
                        Log.d(TAG, "音频开始播放: type=$type, id=$id")
                        audioStateCallback?.onAudioStart(type, id)
                    }

                    override fun onAudioComplete(type: Int, id: Int) {
                        Log.d(TAG, "音频播放完成: type=$type, id=$id")
                        audioStateCallback?.onAudioComplete(type, id)
                    }
                })
                
                // 设置识别监听器
                vtbrSDKManager?.setRecognizeListener(object : IRecognizeListener {
                    /**
                     * 识别成功回调
                     * @param data 识别结果数据
                     * @return 是否消费结果（返回false表示继续处理）
                     */
                    override fun onRecognizeSuccess(data: VTBRBookDataModel): Boolean {
                        Log.i(TAG, "识别成功: bookId=${data.bookId}, pageId=${data.pageId}")
                        
                        // 将SDK的数据模型转换为我们的数据模型
                        val bookInfo = data.bookInfo?.let { bookInfoEntity ->
                            BookInfo(
                                bookId = bookInfoEntity.bookId,
                                bookName = bookInfoEntity.bookName ?: "",
                                isbn = bookInfoEntity.isbn ?: "",
                                publisher = bookInfoEntity.publisher ?: "",
                                author = bookInfoEntity.author ?: "",
                                description = bookInfoEntity.description ?: "",
                                coverImage = "",  // BookInfoEntity可能没有coverImage字段
                                thumbnailCoverImage = bookInfoEntity.thumbnailCoverImage ?: ""
                            )
                        }
                        
                        val result = RecognizeResult(
                            bookId = data.bookId,
                            pageId = data.pageId,
                            pageType = data.pageType,
                            elapsedTime = data.elapsedTime,
                            bookInfo = bookInfo
                        )
                        
                        // 保存当前识别到的书本和页面信息（用于重新播放）
                        currentBookId = data.bookId
                        currentPageId = data.pageId
                        currentPageType = data.pageType
                        
                        // 回调给外部
                        currentRecognizeCallback?.onRecognizeSuccess(result)
                        
                        // 返回false表示不消费结果，继续处理
                        return false
                    }

                    /**
                     * 识别失败回调
                     * @param code 错误码
                     * @param baseBookId 基础书本ID
                     * @param msg 错误信息
                     */
                    override fun onRecognizeFail(code: Int, baseBookId: Int, msg: String) {
                        Log.w(TAG, "识别失败: code=$code, baseBookId=$baseBookId, msg=$msg")
                        
                        // 检查是否是设备被挤下线（错误码1002表示授权码正在被其他设备使用）
                        if (code == 1002) {
                            Log.w(TAG, "检测到设备被另一台机器挤下线")
                            // 触发设备被挤下线回调
                            deviceLogoutCallback?.onDeviceLogout()
                        }
                        
                        // 回调给外部识别失败回调
                        currentRecognizeCallback?.onRecognizeFail(code, baseBookId, msg)
                    }
                    
                    /**
                     * 获取书本信息成功回调
                     * @param bookInfo 书本信息实体
                     * @param needUpdate 是否需要更新
                     * @return 是否消费结果
                     */
                    override fun onGetBookInfoSuccess(
                        bookInfo: com.visiontalk.basesdk.service.basecloud.entity.BookInfoEntity?,
                        needUpdate: Boolean
                    ): Boolean {
                        // 可以在这里处理书本信息获取成功的回调
                        return false
                    }
                    
                    /**
                     * 获取书本信息失败回调
                     * @param code 错误码
                     * @param msg 错误信息
                     */
                    override fun onGetBookInfoFail(code: Int, msg: String) {
                        Log.w(TAG, "获取书本信息失败: code=$code, msg=$msg")
                    }
                    
                    /**
                     * 获取页面音频回调
                     * @param pageAudio 页面音频对象
                     * @return 是否消费结果
                     */
                    override fun onGetPageAudio(pageAudio: com.visiontalk.vtbrsdk.audio.base.PageAudio?): Boolean {
                        // 可以在这里处理页面音频获取的回调
                        // 返回false表示不消费结果，继续处理
                        return false
                    }
                })
                
                // 使用授权凭证初始化识别SDK
                vtbrSDKManager?.initialize(license, object : com.visiontalk.vtbrsdk.listener.IInitializeListener {
                    override fun onInitSuccess() {
                        Log.d(TAG, "识别SDK初始化成功")
                        isRecognizeSDKInitialized = true
                        
                        // 如果有待执行的识别操作，现在执行它
                        pendingRecognizeOperation?.invoke()
                        pendingRecognizeOperation = null
                    }

                    override fun onInitFail(errCode: Int, msg: String) {
                        Log.e(TAG, "识别SDK初始化失败: code=$errCode, msg=$msg")
                        isRecognizeSDKInitialized = false
                        pendingRecognizeOperation = null
                        currentRecognizeCallback?.onRecognizeFail(errCode, -1, "识别SDK初始化失败: $msg")
                    }
                })
                
                Log.d(TAG, "识别SDK管理器创建并初始化完成")
            }
        } ?: run {
            Log.e(TAG, "Context为空，无法初始化识别SDK")
        }
    }

    /**
     * 执行授权认证
     * @param qrCode 二维码内容
     * @param callback 授权回调
     */
    fun authorize(qrCode: String, callback: AuthCallback) {
        if (!isInitialized) {
            callback.onAuthFail(-1, "SDK未初始化，请先调用initialize方法")
            return
        }

        // 处理二维码内容，提取code（参考example项目的处理方式）
        var code = qrCode.trim()
        if (code.contains("=")) {
            val parts = code.split("=")
            if (parts.isNotEmpty()) {
                code = parts.last()
            }
        }

        if (code.isEmpty()) {
            callback.onAuthFail(-2, "二维码内容不能为空")
            return
        }

        // 调用basesdk的授权方法
        VTBaseSDKManagerExt.getInstance().getLicense(code, object : QRCodeAuthCallback {
            override fun onQRCodeAuthSuccess(license: String?) {
                val licenseStr = license ?: ""
                Log.d(TAG, "授权成功，license长度: ${licenseStr.length}")
                callback.onAuthSuccess(licenseStr)
            }

            override fun onQRCodeAuthFail(code: Int, errMsg: String?) {
                Log.e(TAG, "授权失败: code=$code, msg=$errMsg")
                callback.onAuthFail(code, errMsg ?: "授权失败")
            }
        })
    }

    /**
     * 开始识别
     * 打开相机并开始书本识别
     * @param surfaceHolder SurfaceHolder（使用方提供，用于相机预览）
     * @param license 授权凭证（如果识别SDK还未初始化，需要提供license）
     * @param callback 识别回调
     */
    fun startRecognize(surfaceHolder: android.view.SurfaceHolder, license: String?, callback: RecognizeCallback) {
        if (!isInitialized) {
            callback.onRecognizeFail(-1, -1, "SDK未初始化，请先调用initialize方法")
            return
        }

        // 保存回调
        currentRecognizeCallback = callback
        
        // 创建相机控制器并设置 SurfaceHolder
        if (currentCameraCtrl == null) {
            context?.let { ctx ->
                currentCameraCtrl = CameraCtrl(ctx)
            } ?: run {
                callback.onRecognizeFail(-1, -1, "Context为空，无法创建相机控制器")
                return
            }
        }
        currentCameraCtrl?.setSurfaceHolder(surfaceHolder)

        // 如果识别SDK还未初始化，需要先初始化
        if (vtbrSDKManager == null || !isRecognizeSDKInitialized) {
            if (license.isNullOrEmpty()) {
                callback.onRecognizeFail(-2, -1, "识别SDK未初始化，需要提供授权凭证")
                return
            }
            
            // 重置初始化状态
            isRecognizeSDKInitialized = false
            
            // 设置待执行的识别操作（在初始化完成后执行）
            pendingRecognizeOperation = {
                startRecognizeInternal()
            }
            
            // 开始初始化识别SDK
            initializeRecognizeSDK(license)
            
            // 如果初始化是同步完成的（不太可能），直接执行
            if (isRecognizeSDKInitialized) {
                pendingRecognizeOperation = null
                startRecognizeInternal()
            }
        } else {
            // 识别SDK已初始化，直接开始识别
            startRecognizeInternal()
        }
    }
    
    /**
     * 内部方法：执行实际的识别操作
     * 打开相机并开始识别
     */
    private fun startRecognizeInternal() {
        val ctrl = currentCameraCtrl
        if (ctrl == null) {
            currentRecognizeCallback?.onRecognizeFail(-3, -1, "相机控制器未创建")
            return
        }
        vtbrSDKManager?.let { manager ->
            try {
                // 先打开相机（根据example项目的流程，需要先打开相机再开始识别）
                manager.openCamera(currentCameraCtrl ?: return)
                Log.d(TAG, "相机已打开")
                
                // 然后开始识别
                manager.startRecognize()
                Log.d(TAG, "开始识别")
            } catch (e: Exception) {
                Log.e(TAG, "开始识别失败", e)
                currentRecognizeCallback?.onRecognizeFail(-3, -1, "开始识别失败: ${e.message}")
            }
        } ?: run {
            currentRecognizeCallback?.onRecognizeFail(-4, -1, "识别SDK管理器未初始化")
        }
    }

    /**
     * 停止识别
     * 停止书本识别
     */
    fun stopRecognize() {
        vtbrSDKManager?.let { manager ->
            try {
                manager.stopRecognize()
                Log.d(TAG, "停止识别")
            } catch (e: Exception) {
                Log.e(TAG, "停止识别失败", e)
            }
        }
        
        // 清除回调和待执行的操作
        currentRecognizeCallback = null
        pendingRecognizeOperation = null
    }

    /**
     * 打开相机
     * 通过相机控制器打开相机预览
     * @param cameraCtrl 相机控制器（如果为null，则使用当前保存的相机控制器）
     */
    fun openCamera(cameraCtrl: CameraCtrl? = null) {
        val ctrl = cameraCtrl ?: currentCameraCtrl
        if (ctrl == null) {
            Log.w(TAG, "相机控制器为空，无法打开相机")
            return
        }
        
        vtbrSDKManager?.let { manager ->
            try {
                manager.openCamera(ctrl)
                Log.d(TAG, "打开相机")
            } catch (e: Exception) {
                Log.e(TAG, "打开相机失败", e)
            }
        } ?: run {
            Log.w(TAG, "识别SDK管理器未初始化，无法打开相机")
        }
    }

    /**
     * 关闭相机
     * 关闭相机预览并释放资源
     */
    fun closeCamera() {
        vtbrSDKManager?.let { manager ->
            try {
                manager.closeCamera()
                Log.d(TAG, "关闭相机")
            } catch (e: Exception) {
                Log.e(TAG, "关闭相机失败", e)
            }
        }
    }

    /**
     * 设置设备被挤下线回调
     * 当授权码被另一台设备使用时，会触发此回调
     * @param callback 设备被挤下线回调接口
     */
    fun setDeviceLogoutCallback(callback: DeviceLogoutCallback?) {
        this.deviceLogoutCallback = callback
        Log.d(TAG, "设置设备被挤下线回调")
    }

    /**
     * 设置下载状态回调
     * @param callback 下载状态回调接口
     */
    fun setDownloadStateCallback(callback: DownloadStateCallback?) {
        this.downloadStateCallback = callback
        Log.d(TAG, "设置下载状态回调")
    }

    /**
     * 设置播放状态回调
     * @param callback 播放状态回调接口
     */
    fun setAudioStateCallback(callback: AudioStateCallback?) {
        this.audioStateCallback = callback
        Log.d(TAG, "设置播放状态回调")
    }

    /**
     * 暂停播放
     * 暂停当前正在播放的所有音频
     */
    fun pauseAudio() {
        vtbrSDKManager?.let { manager ->
            try {
                manager.pauseAllAudio()
                Log.d(TAG, "暂停播放")
            } catch (e: Exception) {
                Log.e(TAG, "暂停播放失败", e)
            }
        } ?: run {
            Log.w(TAG, "识别SDK管理器未初始化，无法暂停播放")
        }
    }

    /**
     * 恢复播放
     * 恢复之前暂停的音频播放
     */
    fun resumeAudio() {
        vtbrSDKManager?.let { manager ->
            try {
                manager.resumeAllAudio()
                Log.d(TAG, "恢复播放")
            } catch (e: Exception) {
                Log.e(TAG, "恢复播放失败", e)
            }
        } ?: run {
            Log.w(TAG, "识别SDK管理器未初始化，无法恢复播放")
        }
    }

    /**
     * 重新播放
     * 重新播放当前页面的音频
     */
    fun replayAudio() {
        if (currentBookId > 0 && currentPageId > 0) {
            try {
                VTAudioCtrl.getInstance().replayPageAudio()
                Log.d(TAG, "重新播放: bookId=$currentBookId, pageId=$currentPageId")
            } catch (e: Exception) {
                Log.e(TAG, "重新播放失败", e)
            }
        } else {
            Log.w(TAG, "没有可播放的页面，请先识别到书本")
        }
    }

    /**
     * 播放页面音频
     * @param bookId 书本ID
     * @param pageId 页面ID
     * @param pageType 页面类型
     */
    fun playPageAudio(bookId: Int, pageId: Int, pageType: Int) {
        vtbrSDKManager?.let { manager ->
            try {
                manager.playPageAudio(bookId, pageId, pageType)
                // 更新当前播放信息
                currentBookId = bookId
                currentPageId = pageId
                currentPageType = pageType
                Log.d(TAG, "播放页面音频: bookId=$bookId, pageId=$pageId, pageType=$pageType")
            } catch (e: Exception) {
                Log.e(TAG, "播放页面音频失败", e)
            }
        } ?: run {
            Log.w(TAG, "识别SDK管理器未初始化，无法播放页面音频")
        }
    }

    /**
     * 检查SDK是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}



