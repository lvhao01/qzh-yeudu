package uni.lv.readsdk

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import uni.lv.yuedu.YueDuSDKManager
import uni.lv.yuedu.callback.AuthCallback

/**
 * 授权认证界面（使用方实现）
 * 使用插件提供的 YueDuSDKManager.authorize() 方法进行授权
 */
class AuthActivity : AppCompatActivity() {
    private lateinit var etQrCode: TextInputEditText
    private lateinit var btnAuthorize: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvOpenId: android.widget.TextView

    companion object {
        const val EXTRA_LICENSE = "extra_license"
        const val EXTRA_ERROR_CODE = "extra_error_code"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val RESULT_AUTH_SUCCESS = RESULT_OK
        const val RESULT_AUTH_FAIL = RESULT_FIRST_USER + 1
        const val RESULT_INPUT_EMPTY = RESULT_FIRST_USER + 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        initViews()
        setupViews()
    }

    private fun initViews() {
        etQrCode = findViewById(R.id.et_qr_code)
        btnAuthorize = findViewById(R.id.btn_authorize)
        progressBar = findViewById(R.id.progress_bar)
        tvOpenId = findViewById(R.id.tv_openid)
    }

    private fun setupViews() {
        // 初始化SDK
        if (!YueDuSDKManager.isInitialized()) {
            YueDuSDKManager.initialize(this)
        }

        // 更新OpenID显示
        updateOpenIdDisplay()
        
        // 检查是否从Intent中接收到二维码内容
        val qrCodeContent = intent.getStringExtra("qr_code_content")
        if (!qrCodeContent.isNullOrEmpty()) {
            // 如果有二维码内容，自动填充并执行授权
            etQrCode.setText(qrCodeContent)
            // 自动执行授权
            performAuth()
        }

        // 授权按钮点击事件
        btnAuthorize.setOnClickListener {
            performAuth()
        }
    }
    
    /**
     * 更新OpenID显示
     */
    private fun updateOpenIdDisplay() {
        val openId = YueDuSDKManager.getOpenID()
        if (openId.isNotEmpty()) {
            tvOpenId.text = "OpenID: $openId"
        } else {
            tvOpenId.text = "OpenID: 未获取（需要先完成授权）"
        }
    }

    /**
     * 执行授权操作
     * 调用插件的 YueDuSDKManager.authorize() 方法
     */
    private fun performAuth() {
        val qrCode = etQrCode.text?.toString()?.trim()

        if (TextUtils.isEmpty(qrCode)) {
            // 输入为空时，通过结果码传递给外层处理
            val resultIntent = Intent().apply {
                putExtra(EXTRA_ERROR_MESSAGE, "请输入授权二维码内容")
            }
            setResult(RESULT_INPUT_EMPTY, resultIntent)
            finish()
            return
        }

        // 显示加载状态
        progressBar.visibility = View.VISIBLE
        btnAuthorize.isEnabled = false

        // 调用插件的授权方法（qrCode此时已经确定不为null，使用!!断言）
        YueDuSDKManager.authorize(qrCode!!, object : AuthCallback {
            override fun onAuthSuccess(license: String) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnAuthorize.isEnabled = true
                    
                    // 更新OpenID显示（授权成功后OpenID应该可以获取到了）
                    updateOpenIdDisplay()
                    
                    // 通过Intent传递结果给外层
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_LICENSE, license)
                    }
                    setResult(RESULT_AUTH_SUCCESS, resultIntent)
                    
                    // 延迟一点再finish，让用户能看到OpenID更新
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 500)
                }
            }

            override fun onAuthFail(code: Int, message: String) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnAuthorize.isEnabled = true
                    // 通过Intent传递错误信息给外层
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_ERROR_CODE, code)
                        putExtra(EXTRA_ERROR_MESSAGE, message)
                    }
                    setResult(RESULT_AUTH_FAIL, resultIntent)
                    finish()
                }
            }
        })
    }
}


