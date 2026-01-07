package uni.lv.readsdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import uni.lv.readsdk.utils.AuthPrefsUtil
import uni.lv.yuedu.YueDuSDKManager
import uni.lv.yuedu.callback.DeviceLogoutCallback

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartAuth: Button
    private lateinit var btnScanQRCode: Button
    private lateinit var btnStartRecognize: Button
    private lateinit var btnClearAuth: Button
    private lateinit var tvStatus: TextView
    
    // 保存授权凭证，用于识别功能
    private var savedLicense: String? = null

    companion object {
        private const val TAG = "RecognizeActivity"
    }

    // 使用新的 Activity Result API，处理二维码扫描结果
    private val qrCodeScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resultCode = result.resultCode
        
        Log.d(TAG, "========== 二维码扫描结果 ==========")
        Log.d(TAG, "ResultCode: $resultCode")
        Log.d(TAG, "Data: $data")
        
        when (resultCode) {
            QRCodeScanActivity.RESULT_SCAN_SUCCESS -> {
                // 扫描成功，获取二维码内容
                val qrCodeContent = data?.getStringExtra(QRCodeScanActivity.EXTRA_SCAN_RESULT) ?: ""
                Log.d(TAG, "扫描成功 - 二维码内容: $qrCodeContent")
                
                if (qrCodeContent.isNotEmpty()) {
                    // 使用扫描到的二维码内容进行授权
                    performAuth(qrCodeContent)
                } else {
                    Toast.makeText(this, "二维码内容为空", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeScanActivity.RESULT_SCAN_CANCELED -> {
                // 用户取消扫描
                Log.d(TAG, "用户取消扫描")
                Toast.makeText(this, "已取消扫描", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.w(TAG, "未知的结果码: $resultCode")
            }
        }
        Log.d(TAG, "=================================")
    }

    // 使用新的 Activity Result API，处理授权回调结果
    private val authActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resultCode = result.resultCode
        
        // 打印回调结果
        Log.d(TAG, "========== 授权回调结果 ==========")
        Log.d(TAG, "ResultCode: $resultCode")
        Log.d(TAG, "ResultCode常量: ${getResultCodeName(resultCode)}")
        Log.d(TAG, "Data: $data")
        
        when (resultCode) {
            AuthActivity.RESULT_AUTH_SUCCESS -> {
                // 授权成功
                val license = data?.getStringExtra(AuthActivity.EXTRA_LICENSE) ?: ""
                Log.d(TAG, "授权成功 - License: $license")
                Log.d(TAG, "License长度: ${license.length}")
                
                // 保存授权凭证到内存
                savedLicense = license
                
                // 保存认证状态和授权凭证到SharedPreferences，下次启动时自动加载
                AuthPrefsUtil.saveAuth(this, license)
                Log.d(TAG, "认证状态已保存")
                
                // 更新UI：隐藏认证按钮，启用识别按钮
                updateUIForAuthenticated()
                
                tvStatus.text = "授权状态: 已授权"
                Toast.makeText(this, "授权成功，可以开始识别了", Toast.LENGTH_SHORT).show()
            }
            AuthActivity.RESULT_AUTH_FAIL -> {
                // 授权失败
                val errorCode = data?.getIntExtra(AuthActivity.EXTRA_ERROR_CODE, -1) ?: -1
                val errorMessage = data?.getStringExtra(AuthActivity.EXTRA_ERROR_MESSAGE) ?: "未知错误"
                Log.e(TAG, "授权失败 - ErrorCode: $errorCode, ErrorMessage: $errorMessage")
                
                // 清除保存的认证状态
                AuthPrefsUtil.clearAuth(this)
                savedLicense = null
                
                // 更新UI：显示认证按钮，禁用识别按钮
                updateUIForUnauthenticated()
                
                tvStatus.text = "授权状态: 授权失败"
                Toast.makeText(this, "授权失败: $errorMessage", Toast.LENGTH_LONG).show()
            }
            AuthActivity.RESULT_INPUT_EMPTY -> {
                // 输入为空
                val message = data?.getStringExtra(AuthActivity.EXTRA_ERROR_MESSAGE) ?: "请输入授权二维码内容"
                Log.w(TAG, "输入为空 - Message: $message")
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.w(TAG, "未知的结果码: $resultCode")
            }
        }
        Log.d(TAG, "=================================")
    }
    
    // 使用新的 Activity Result API，处理识别回调结果
    private val recognizeActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resultCode = result.resultCode
        
        // 打印回调结果
        Log.d(TAG, "========== 识别回调结果 ==========")
        Log.d(TAG, "ResultCode: $resultCode")
        Log.d(TAG, "Data: $data")
        
        when (resultCode) {
            RecognizeActivity.RESULT_RECOGNIZE_SUCCESS -> {
                // 识别成功
                val bookId = data?.getIntExtra(RecognizeActivity.EXTRA_BOOK_ID, -1) ?: -1
                val pageId = data?.getIntExtra(RecognizeActivity.EXTRA_PAGE_ID, -1) ?: -1
                Log.d(TAG, "识别成功 - BookID: $bookId, PageID: $pageId")
                tvStatus.text = "识别状态: 识别成功 (书本ID: $bookId, 页面ID: $pageId)"
                Toast.makeText(this, "识别成功: 书本ID=$bookId, 页面ID=$pageId", Toast.LENGTH_SHORT).show()
            }
            RecognizeActivity.RESULT_RECOGNIZE_FAIL -> {
                // 识别失败
                val errorCode = data?.getIntExtra(RecognizeActivity.EXTRA_ERROR_CODE, -1) ?: -1
                val errorMessage = data?.getStringExtra(RecognizeActivity.EXTRA_ERROR_MESSAGE) ?: "未知错误"
                Log.e(TAG, "识别失败 - ErrorCode: $errorCode, ErrorMessage: $errorMessage")
                tvStatus.text = "识别状态: 识别失败 - $errorMessage"
                Toast.makeText(this, "识别失败: $errorMessage", Toast.LENGTH_SHORT).show()
            }
            RESULT_CANCELED -> {
                // 用户取消
                Log.d(TAG, "用户取消识别")
                tvStatus.text = "识别状态: 已取消"
            }
            else -> {
                Log.w(TAG, "未知的结果码: $resultCode")
            }
        }
        Log.d(TAG, "=================================")
    }
    
    private fun getResultCodeName(resultCode: Int): String {
        // 先检查自定义的结果码
        return when (resultCode) {
            AuthActivity.RESULT_AUTH_SUCCESS -> "RESULT_AUTH_SUCCESS"
            AuthActivity.RESULT_AUTH_FAIL -> "RESULT_AUTH_FAIL"
            AuthActivity.RESULT_INPUT_EMPTY -> "RESULT_INPUT_EMPTY"
            RESULT_CANCELED -> "RESULT_CANCELED"
            // 注意：RESULT_AUTH_SUCCESS == RESULT_OK，所以上面已经处理了
            // 只有当不是自定义常量时，才检查是否是RESULT_OK
            else -> when (resultCode) {
                RESULT_OK -> "RESULT_OK"
            else -> "UNKNOWN($resultCode)"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        
        // 检查是否已认证，如果已认证则自动加载
        checkAndLoadAuthStatus()
        
        setupViews()
    }
    
    /**
     * 检查并加载认证状态
     * 如果之前已认证过，则自动加载保存的授权凭证
     */
    private fun checkAndLoadAuthStatus() {
        if (AuthPrefsUtil.hasValidLicense(this)) {
            // 已认证过，加载保存的授权凭证
            savedLicense = AuthPrefsUtil.getLicense(this)
            Log.d(TAG, "检测到已认证状态，自动加载授权凭证")
            Log.d(TAG, "License长度: ${savedLicense?.length}")
            
            // 更新UI：隐藏认证按钮，启用识别按钮
            updateUIForAuthenticated()
            
            tvStatus.text = "授权状态: 已授权（自动加载）"
        } else {
            // 未认证，显示认证按钮
            Log.d(TAG, "未检测到认证状态，需要重新认证")
            updateUIForUnauthenticated()
            tvStatus.text = "授权状态: 未授权"
        }
    }
    
    /**
     * 更新UI为已认证状态
     * 隐藏认证按钮，启用识别按钮，显示清除认证按钮
     */
    private fun updateUIForAuthenticated() {
        btnScanQRCode.visibility = android.view.View.GONE
        btnStartAuth.visibility = android.view.View.GONE
        btnStartRecognize.isEnabled = true
        btnClearAuth.visibility = android.view.View.VISIBLE
    }
    
    /**
     * 更新UI为未认证状态
     * 显示认证按钮，禁用识别按钮，隐藏清除认证按钮
     */
    private fun updateUIForUnauthenticated() {
        btnScanQRCode.visibility = android.view.View.VISIBLE
        btnStartAuth.visibility = android.view.View.VISIBLE
        btnStartRecognize.isEnabled = false
        btnClearAuth.visibility = android.view.View.GONE
    }

    private fun initViews() {
        btnStartAuth = findViewById(R.id.btn_start_auth)
        btnScanQRCode = findViewById(R.id.btn_scan_qrcode)
        btnStartRecognize = findViewById(R.id.btn_start_recognize)
        btnClearAuth = findViewById(R.id.btn_clear_auth)
        tvStatus = findViewById(R.id.tv_status)
    }
    
    /**
     * 执行授权操作
     * 使用二维码内容调用插件的授权接口
     * @param qrCodeContent 二维码内容（授权字符串）
     */
    private fun performAuth(qrCodeContent: String) {
        // 确保SDK已初始化
        if (!YueDuSDKManager.isInitialized()) {
            try {
                YueDuSDKManager.initialize(this)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "SDK初始化失败", e)
                tvStatus.text = "SDK初始化失败: ${e.message}"
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // 启动授权界面，传入二维码内容
        val intent = Intent(this, AuthActivity::class.java).apply {
            putExtra("qr_code_content", qrCodeContent)
        }
        authActivityLauncher.launch(intent)
    }

    private fun setupViews() {
        // 扫描二维码按钮（推荐方式）
        btnScanQRCode.setOnClickListener {
            // 启动二维码扫描Activity
            val intent = Intent(this, QRCodeScanActivity::class.java)
            qrCodeScanLauncher.launch(intent)
        }
        
        // 手动输入授权码按钮（备用方式）
        btnStartAuth.setOnClickListener {
            // 延迟初始化SDK，只在需要时初始化
            if (!YueDuSDKManager.isInitialized()) {
                try {
                    YueDuSDKManager.initialize(this)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "SDK初始化失败", e)
                    tvStatus.text = "SDK初始化失败: ${e.message}"
                    return@setOnClickListener
                }
            }
            
            val intent = Intent(this, AuthActivity::class.java)
            authActivityLauncher.launch(intent)
        }
        
        // 启动识别界面按钮
        btnStartRecognize.setOnClickListener {
            // 检查是否已授权
            if (savedLicense.isNullOrEmpty()) {
                Toast.makeText(this, "请先完成授权认证", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 确保SDK已初始化
            if (!YueDuSDKManager.isInitialized()) {
                try {
                    YueDuSDKManager.initialize(this)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "SDK初始化失败", e)
                    tvStatus.text = "SDK初始化失败: ${e.message}"
                    return@setOnClickListener
                }
            }
            
            // 启动识别界面，通过Intent传递授权凭证
            val intent = Intent(this, RecognizeActivity::class.java).apply {
                putExtra(RecognizeActivity.EXTRA_LICENSE, savedLicense)
            }
            recognizeActivityLauncher.launch(intent)
        }
        
        // 清除认证按钮点击事件
        btnClearAuth.setOnClickListener {
            // 清除认证状态
            AuthPrefsUtil.clearAuth(this)
            savedLicense = null
            
            // 更新UI
            updateUIForUnauthenticated()
            
            tvStatus.text = "授权状态: 已清除，请重新认证"
            Toast.makeText(this, "认证状态已清除", Toast.LENGTH_SHORT).show()
            android.util.Log.d("MainActivity", "用户手动清除认证状态")
        }
    }
}


