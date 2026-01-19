package uni.lv.readsdk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import uni.lv.yuedu.YueDuSDKManager
import uni.lv.yuedu.callback.RecognizeCallback
import uni.lv.yuedu.callback.DownloadStateCallback
import uni.lv.yuedu.callback.AudioStateCallback
import uni.lv.yuedu.model.RecognizeResult

/**
 * 书本识别Activity（使用方实现）
 * 使用插件提供的 YueDuSDKManager 进行识别，自己实现所有 UI
 */
class RecognizeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RecognizeActivity"
        
        // Activity结果码
        const val RESULT_RECOGNIZE_SUCCESS = Activity.RESULT_OK
        const val RESULT_RECOGNIZE_FAIL = Activity.RESULT_FIRST_USER + 10
        
        // Intent传递的额外数据键
        const val EXTRA_LICENSE = "extra_license"
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_PAGE_ID = "extra_page_id"
        const val EXTRA_ERROR_CODE = "extra_error_code"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    // 相机预览SurfaceView（使用方自己实现）
    private lateinit var surfaceView: SurfaceView
    
    // 控制按钮（使用方自己实现）
    private lateinit var btnStartRecognize: Button
    private lateinit var btnStopRecognize: Button
    private lateinit var btnPauseAudio: Button
    private lateinit var btnResumeAudio: Button
    private lateinit var btnReplayAudio: Button
    
    // 状态提示文本（使用方自己实现）
    private lateinit var tvOpenId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRecognizeInfo: TextView
    private lateinit var tvDownloadStatus: TextView
    private lateinit var tvPlayStatus: TextView
    
    // 授权凭证
    private var license: String? = null
    
    // Surface是否已创建
    private var isSurfaceCreated = false
    
    // 是否正在识别
    private var isRecognizing = false
    
    // 待执行的识别操作（当Surface准备好时执行）
    private var pendingRecognizeOperation: (() -> Unit)? = null
    
    // 当前识别结果
    private var currentRecognizeResult: RecognizeResult? = null
    
    // 播放状态
    private var isPlaying = false
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognize)

        // 获取传递的授权凭证
        license = intent.getStringExtra(EXTRA_LICENSE)
        if (license.isNullOrEmpty()) {
            Log.e(TAG, "未接收到授权凭证，无法启动识别")
            Toast.makeText(this, "未接收到授权凭证，无法启动识别", Toast.LENGTH_SHORT).show()
            setResult(RESULT_RECOGNIZE_FAIL, Intent().apply {
                putExtra(EXTRA_ERROR_MESSAGE, "未提供授权凭证")
            })
            finish()
            return
        }

        initViews()
        setupViews()
        setupSDKCallbacks()

        // 异步初始化SDK，避免阻塞主线程
        if (!YueDuSDKManager.isInitialized()) {
            YueDuSDKManager.initializeAsync(this) { success, error ->
                if (success) {
                    Log.d(TAG, "SDK初始化完成")
                } else {
                    Log.e(TAG, "SDK初始化失败: $error")
                    Toast.makeText(this, "SDK初始化失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 检查并申请相机权限
        checkCameraPermission()
        
        // 更新OpenID显示
        updateOpenIdDisplay()
    }
    
    /**
     * 更新OpenID显示
     */
    private fun updateOpenIdDisplay() {
        val openId = YueDuSDKManager.getOpenID()
        if (openId.isNotEmpty()) {
            tvOpenId.text = "OpenID: $openId"
            Log.d(TAG, "OpenID已更新: $openId")
        } else {
            tvOpenId.text = "OpenID: 未获取（需要先完成授权）"
            Log.d(TAG, "OpenID为空，可能还未授权")
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        // 使用方自己实现的SurfaceView
        surfaceView = findViewById(R.id.surface_view)
        
        // 使用方自己实现的按钮和状态文本
        btnStartRecognize = findViewById(R.id.btn_start_recognize)
        btnStopRecognize = findViewById(R.id.btn_stop_recognize)
        btnPauseAudio = findViewById(R.id.btn_pause_audio)
        btnResumeAudio = findViewById(R.id.btn_resume_audio)
        btnReplayAudio = findViewById(R.id.btn_replay_audio)
        
        tvOpenId = findViewById(R.id.tv_openid)
        tvStatus = findViewById(R.id.tv_status)
        tvRecognizeInfo = findViewById(R.id.tv_recognize_info)
        tvDownloadStatus = findViewById(R.id.tv_download_status)
        tvPlayStatus = findViewById(R.id.tv_play_status)
    }

    /**
     * 设置视图事件和回调
     */
    private fun setupViews() {
        // 设置SurfaceHolder回调
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Surface已创建")
                isSurfaceCreated = true
                // Surface 已创建，如果有待执行的识别操作，现在执行
                pendingRecognizeOperation?.invoke()
                pendingRecognizeOperation = null
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "Surface已变化: width=$width, height=$height")
                // Surface 已变化，确保相机预览正常显示
                isSurfaceCreated = true
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Surface已销毁")
                isSurfaceCreated = false
                pendingRecognizeOperation = null
                stopRecognize()
            }
        })

        // 开始识别按钮点击事件
        btnStartRecognize.setOnClickListener {
            startRecognize()
        }

        // 停止识别按钮点击事件
        btnStopRecognize.setOnClickListener {
            stopRecognize()
        }
        
        // 暂停播放按钮点击事件
        btnPauseAudio.setOnClickListener {
            pauseAudio()
        }
        
        // 恢复播放按钮点击事件
        btnResumeAudio.setOnClickListener {
            resumeAudio()
        }
        
        // 重新播放按钮点击事件
        btnReplayAudio.setOnClickListener {
            replayAudio()
        }
    }
    
    /**
     * 设置SDK回调
     */
    private fun setupSDKCallbacks() {
        // 设置下载状态回调
        YueDuSDKManager.setDownloadStateCallback(object : DownloadStateCallback {
            override fun onDownloadPrepare(downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("下载准备中 (bookId: $downloadId)")
                    Log.d(TAG, "下载准备: downloadId=$downloadId, isForeground=$isForeground")
                }
            }

            override fun onDownloadStart(downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("下载开始 (bookId: $downloadId)")
                    Log.d(TAG, "下载开始: downloadId=$downloadId, isForeground=$isForeground")
                }
            }

            override fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("下载中: $progress% (bookId: $downloadId)")
                    Log.d(TAG, "下载中: progress=$progress%, downloadId=$downloadId")
                }
            }

            override fun onDownloadEnd(downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("下载完成 (bookId: $downloadId)")
                    Log.d(TAG, "下载完成: downloadId=$downloadId")
                }
            }

            override fun onDownloadFail(downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("下载失败 (bookId: $downloadId)")
                    Log.w(TAG, "下载失败: downloadId=$downloadId")
                }
            }

            override fun onUnzipStart(downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("解压开始 (bookId: $downloadId)")
                    Log.d(TAG, "解压开始: downloadId=$downloadId")
                }
            }

            override fun onUnzipComplete(downloadId: Int, isForeground: Boolean): Boolean {
                runOnUiThread {
                    updateDownloadStatus("解压完成 (bookId: $downloadId)")
                    Log.d(TAG, "解压完成: downloadId=$downloadId")
                }
                return false
            }

            override fun onUnzipError(downloadId: Int, errMsg: String, isForeground: Boolean) {
                runOnUiThread {
                    updateDownloadStatus("解压错误: $errMsg (bookId: $downloadId)")
                    Log.e(TAG, "解压错误: downloadId=$downloadId, errMsg=$errMsg")
                }
            }
        })
        
        // 设置播放状态回调
        YueDuSDKManager.setAudioStateCallback(object : AudioStateCallback {
            override fun onAudioStart(type: Int, id: Int) {
                runOnUiThread {
                    isPlaying = true
                    isPaused = false
                    updatePlayStatus("播放中 (type: $type, id: $id)")
                    updatePlayButtons()
                    Log.d(TAG, "音频开始播放: type=$type, id=$id")
                }
            }

            override fun onAudioComplete(type: Int, id: Int) {
                runOnUiThread {
                    isPlaying = false
                    isPaused = false
                    updatePlayStatus("播放完成 (type: $type, id: $id)")
                    updatePlayButtons()
                    Log.d(TAG, "音频播放完成: type=$type, id=$id")
                }
            }
        })
    }
    
    /**
     * 更新下载状态显示
     */
    private fun updateDownloadStatus(status: String) {
        tvDownloadStatus.text = "下载状态: $status"
    }
    
    /**
     * 更新播放状态显示
     */
    private fun updatePlayStatus(status: String) {
        tvPlayStatus.text = "播放状态: $status"
    }
    
    /**
     * 更新播放按钮状态
     */
    private fun updatePlayButtons() {
        btnPauseAudio.isEnabled = isPlaying && !isPaused
        btnResumeAudio.isEnabled = isPaused
        btnReplayAudio.isEnabled = currentRecognizeResult != null
    }
    
    /**
     * 暂停播放
     */
    private fun pauseAudio() {
        YueDuSDKManager.pauseAudio()
        isPaused = true
        isPlaying = false
        updatePlayStatus("已暂停")
        updatePlayButtons()
        Log.d(TAG, "暂停播放")
    }
    
    /**
     * 恢复播放
     */
    private fun resumeAudio() {
        YueDuSDKManager.resumeAudio()
        isPaused = false
        isPlaying = true
        updatePlayStatus("恢复播放中")
        updatePlayButtons()
        Log.d(TAG, "恢复播放")
    }
    
    /**
     * 重新播放
     */
    private fun replayAudio() {
        currentRecognizeResult?.let { result ->
            YueDuSDKManager.replayAudio()
            isPaused = false
            isPlaying = true
            updatePlayStatus("重新播放中")
            updatePlayButtons()
            Log.d(TAG, "重新播放: bookId=${result.bookId}, pageId=${result.pageId}")
        } ?: run {
            Toast.makeText(this, "没有可播放的内容，请先识别到书本", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查相机权限
     * 如果没有权限则申请
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.CAMERA), 
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d(TAG, "相机权限已授予")
            tvStatus.text = "相机权限已授予，等待识别"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "相机权限已授予")
                tvStatus.text = "相机权限已授予，可以开始识别"
            } else {
                Log.e(TAG, "相机权限被拒绝")
                tvStatus.text = "相机权限被拒绝，无法使用识别功能"
                Toast.makeText(this, "需要相机权限才能使用识别功能", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 开始识别
     * 调用插件的 YueDuSDKManager.startRecognize() 方法
     */
    private fun startRecognize() {
        if (isRecognizing) {
            Log.w(TAG, "识别已在进行中")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予相机权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (license.isNullOrEmpty()) {
            Toast.makeText(this, "授权凭证为空，无法开始识别", Toast.LENGTH_SHORT).show()
            return
        }

        // 如果 Surface 还没准备好，保存操作待 Surface 准备好后执行
        if (!isSurfaceCreated || !surfaceView.holder.surface.isValid) {
            Log.d(TAG, "Surface未准备好，等待Surface创建后执行")
            Toast.makeText(this, "相机预览未准备好，请稍候", Toast.LENGTH_SHORT).show()
            
            // 保存待执行的操作
            pendingRecognizeOperation = {
                executeRecognize()
            }
            return
        }

        executeRecognize()
    }

    /**
     * 执行识别操作
     * 确保 Surface 已准备好后调用
     */
    private fun executeRecognize() {
        if (isRecognizing) {
            Log.w(TAG, "识别已在进行中")
            return
        }

        if (!isSurfaceCreated || !surfaceView.holder.surface.isValid) {
            Log.w(TAG, "Surface仍未准备好")
            return
        }

        isRecognizing = true
        btnStartRecognize.isEnabled = false
        btnStopRecognize.isEnabled = true
        tvStatus.text = "正在识别中..."

        // 调用插件的识别方法，传递 SurfaceHolder（插件内部会创建和管理相机控制器）
        YueDuSDKManager.startRecognize(surfaceView.holder, license, object : RecognizeCallback {
            override fun onRecognizeSuccess(result: RecognizeResult) {
                runOnUiThread {
                    handleRecognizeSuccess(result)
                }
            }

            override fun onRecognizeFail(code: Int, baseBookId: Int, message: String) {
                runOnUiThread {
                    handleRecognizeFail(code, baseBookId, message)
                }
            }
        })

        Log.d(TAG, "开始识别")
    }

    /**
     * 停止识别
     * 调用插件的 YueDuSDKManager.stopRecognize() 方法
     */
    private fun stopRecognize() {
        if (!isRecognizing) {
            Log.w(TAG, "识别未在进行")
            return
        }

        isRecognizing = false
        btnStartRecognize.isEnabled = true
        btnStopRecognize.isEnabled = false
        tvStatus.text = "识别已停止"
        
        // 重置识别信息
        currentRecognizeResult = null
        tvRecognizeInfo.text = "识别信息: 无"
        updatePlayButtons()

        // 调用插件的停止识别方法
        YueDuSDKManager.stopRecognize()

        Log.d(TAG, "停止识别")
    }

    /**
     * 处理识别成功
     * @param result 识别结果数据（插件提供的数据模型）
     */
    private fun handleRecognizeSuccess(result: RecognizeResult) {
        // 保存当前识别结果
        currentRecognizeResult = result
        
        // 打印识别内容
        val bookInfo = result.bookInfo
        val recognizeInfo = buildString {
            append("识别成功\n")
            append("书本ID: ${result.bookId}\n")
            append("页面ID: ${result.pageId}\n")
            append("页面类型: ${result.pageType}\n")
            append("耗时: ${result.elapsedTime}ms\n")
            bookInfo?.let {
                append("书名: ${it.bookName}\n")
                append("ISBN: ${it.isbn}\n")
                append("出版社: ${it.publisher}\n")
                append("作者: ${it.author}\n")
                if (it.description.isNotEmpty()) {
                    append("描述: ${it.description}\n")
                }
                append("是否支持指读: ${if (it.supportFingerRead) "是" else "否"}\n")
            }
        }
        
        Log.i(TAG, "========== 识别成功 ==========")
        Log.i(TAG, recognizeInfo)
        Log.i(TAG, "=============================")
        
        // 更新UI显示
        tvRecognizeInfo.text = recognizeInfo.trim()
        
        val bookName = bookInfo?.bookName ?: "未知书本"
        tvStatus.text = "识别成功: $bookName (ID: ${result.bookId})"
        // 移除Toast提示，避免遮挡按钮，信息已显示在界面上
        
        // 更新播放按钮状态
        updatePlayButtons()

        val resultIntent = Intent().apply {
            putExtra(EXTRA_BOOK_ID, result.bookId)
            putExtra(EXTRA_PAGE_ID, result.pageId)
        }
        setResult(RESULT_RECOGNIZE_SUCCESS, resultIntent)
        // 不调用finish()，让用户可以继续识别
    }

    /**
     * 处理识别失败
     * @param code 错误码
     * @param baseBookId 基础书本ID
     * @param message 错误信息
     */
    private fun handleRecognizeFail(code: Int, baseBookId: Int, message: String) {
        val failInfo = "识别失败\n错误码: $code\n基础书本ID: $baseBookId\n错误信息: $message"
        
        Log.w(TAG, "========== 识别失败 ==========")
        Log.w(TAG, failInfo)
        Log.w(TAG, "=============================")
        
        // 更新UI显示
        tvRecognizeInfo.text = failInfo
        tvStatus.text = "识别失败: $message"
        // 移除Toast提示，避免遮挡按钮，信息已显示在界面上

        val resultIntent = Intent().apply {
            putExtra(EXTRA_ERROR_CODE, code)
            putExtra(EXTRA_ERROR_MESSAGE, message)
        }
        setResult(RESULT_RECOGNIZE_FAIL, resultIntent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        // 暂停时停止识别
        if (isRecognizing) {
            stopRecognize()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        // 确保在Activity销毁时停止识别
        if (isRecognizing) {
            stopRecognize()
        }
    }
}
