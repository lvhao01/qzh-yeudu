# YueDu 阅读识别插件 API 文档

## 📖 插件简介

**插件名称**：lv-yuedu-plugin  
**Maven 坐标**：`io.github.lvhao01:lv-yuedu-plugin:1.0.1`  
**版本**：1.0.1

YueDu 插件是一个专为 uni-app x 设计的阅读识别插件，提供书本识别、授权认证、音频播放等核心功能。

### 主要能力

- ✅ **SDK 初始化**：同步/异步初始化基础 SDK 和识别 SDK
- ✅ **授权认证**：通过二维码内容获取授权凭证（license）
- ✅ **书本识别**：实时识别书本页面，返回书本 ID、页面 ID、页面类型等信息
- ✅ **相机控制**：自动管理相机预览，支持打开/关闭相机
- ✅ **音频播放**：播放页面音频，支持暂停、恢复、重新播放
- ✅ **状态回调**：下载状态、播放状态、设备挤下线等事件通知
- ✅ **OpenID 获取**：获取授权后的 OpenID 信息

---

## 🚀 快速开始

### 1. 添加依赖

在 uni-app x 项目的 `app` 模块 `build.gradle` 中添加：

```gradle
dependencies {
    implementation "io.github.lvhao01:lv-yuedu-plugin:1.0.1"
}
```

### 2. 权限配置

插件已自动声明以下权限，但需要在运行时动态申请：

- `android.permission.INTERNET` - 网络权限
- `android.permission.ACCESS_NETWORK_STATE` - 网络状态权限
- `android.permission.CAMERA` - 相机权限（**需要运行时申请**）
- `android.permission.MANAGE_EXTERNAL_STORAGE` - 管理外部存储权限（用于存储书本资源文件）

### 3. 重要说明

⚠️ **插件不提供 UI**：插件只提供逻辑功能和回调接口，所有 UI（识别预览页面、扫码页面、授权输入页面等）需要由使用方（uni-app x）自行实现。

---

## 📚 API 文档

### 初始化相关

#### `initialize(context: Context)`

同步初始化基础 SDK。

**注意**：此方法会立即执行，但初始化操作本身可能较耗时，建议在后台线程调用，或使用异步初始化方法。

**参数**：
- `context: Context` - Android 上下文

**示例**：
```kotlin
YueDuSDKManager.initialize(context)
```

---

#### `initializeAsync(context: Context, callback: ((Boolean, String?) -> Unit)?)`

异步初始化基础 SDK，在后台线程执行初始化，避免阻塞主线程。

**参数**：
- `context: Context` - Android 上下文
- `callback: ((Boolean, String?) -> Unit)?` - 初始化完成回调（在主线程执行）
  - 参数1：`Boolean` - 成功标志
  - 参数2：`String?` - 错误信息（失败时）

**示例**：
```kotlin
YueDuSDKManager.initializeAsync(context) { success, error ->
    if (success) {
        Log.d(TAG, "SDK 初始化成功")
    } else {
        Log.e(TAG, "SDK 初始化失败: $error")
    }
}
```

---

#### `isInitialized(): Boolean`

检查 SDK 是否已初始化。

**返回值**：
- `Boolean` - true 表示已初始化，false 表示未初始化

**示例**：
```kotlin
if (YueDuSDKManager.isInitialized()) {
    // SDK 已初始化，可以继续操作
}
```

---

### 授权认证

#### `authorize(qrCode: String, callback: AuthCallback)`

执行授权认证，通过二维码内容获取授权凭证（license）。

**参数**：
- `qrCode: String` - 二维码内容（支持完整二维码字符串，如果包含 `=` 号，会自动提取最后一段作为授权码）
- `callback: AuthCallback` - 授权回调接口

**回调接口 `AuthCallback`**：
```kotlin
interface AuthCallback {
    /**
     * 授权成功
     * @param license 授权凭证（需要保存，后续识别时需要）
     */
    fun onAuthSuccess(license: String)
    
    /**
     * 授权失败
     * @param code 错误码
     * @param message 错误信息
     */
    fun onAuthFail(code: Int, message: String)
}
```

**示例**：
```kotlin
YueDuSDKManager.authorize(qrCodeString, object : AuthCallback {
    override fun onAuthSuccess(license: String) {
        // 保存 license 到本地存储
        AuthPrefsUtil.saveLicense(license)
        // 更新 UI，跳转到识别页面
    }
    
    override fun onAuthFail(code: Int, message: String) {
        // 显示错误提示
        Log.e(TAG, "授权失败: code=$code, message=$message")
    }
})
```

**错误码说明**：
- `-1`：SDK 未初始化
- `-2`：二维码内容为空
- 其他：服务端返回的错误码（如授权码无效、网络错误等）

**相关方法**：
- 授权成功后，可以使用 `getOpenID()` 方法获取 OpenID（需要先调用 `startRecognize` 初始化识别 SDK）

---

### 识别功能

#### `startRecognize(surfaceHolder: android.view.SurfaceHolder, license: String?, callback: RecognizeCallback)`

开始识别，打开相机并开始书本识别。

**参数**：
- `surfaceHolder: android.view.SurfaceHolder` - 相机预览的 SurfaceHolder（由使用方提供）
- `license: String?` - 授权凭证（如果识别 SDK 还未初始化，需要提供 license；如果识别 SDK 已初始化，可以传 null）
- `callback: RecognizeCallback` - 识别回调接口

**工作流程**：
1. 如果识别 SDK 还未初始化，会先使用 `license` 初始化识别 SDK（异步操作）
2. 识别 SDK 初始化成功后，会自动打开相机并开始识别
3. 如果识别 SDK 已初始化，会直接打开相机并开始识别

**注意**：
- 首次调用此方法时，必须提供有效的 `license`
- 识别 SDK 初始化成功后，后续调用可以传 `null` 作为 `license` 参数
- 识别 SDK 初始化是异步的，如果初始化失败，会通过 `RecognizeCallback.onRecognizeFail` 回调通知

**回调接口 `RecognizeCallback`**：
```kotlin
interface RecognizeCallback {
    /**
     * 识别成功
     * @param result 识别结果数据
     */
    fun onRecognizeSuccess(result: RecognizeResult)
    
    /**
     * 识别失败
     * @param code 错误码
     * @param baseBookId 基础书本ID
     * @param message 错误信息
     */
    fun onRecognizeFail(code: Int, baseBookId: Int, message: String)
}
```

**识别结果 `RecognizeResult`**：
```kotlin
data class RecognizeResult(
    val bookId: Int,              // 书本ID
    val pageId: Int,               // 页面ID
    val pageType: Int,             // 页面类型
    val elapsedTime: Long,         // 识别耗时（毫秒）
    val bookInfo: BookInfo?        // 书本详细信息（可能为 null）
)
```

**书本信息 `BookInfo`**：
```kotlin
data class BookInfo(
    val bookId: Int,                    // 书本ID
    val bookName: String,               // 书本名称
    val isbn: String,                   // ISBN号码
    val publisher: String,              // 出版社
    val author: String,                 // 作者
    val description: String,           // 书本描述
    val coverImage: String,            // 封面图片URL
    val thumbnailCoverImage: String,   // 缩略图封面图片URL
    val supportFingerRead: Boolean     // 是否支持手指点读（指读）
)
```

**示例**：
```kotlin
// 在 Activity 中，准备好 SurfaceView 的 SurfaceHolder
val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
val surfaceHolder = surfaceView.holder

// 获取保存的 license
val license = AuthPrefsUtil.getLicense()

// 开始识别
YueDuSDKManager.startRecognize(surfaceHolder, license, object : RecognizeCallback {
    override fun onRecognizeSuccess(result: RecognizeResult) {
        // 更新 UI，显示识别结果
        Log.d(TAG, "识别成功: bookId=${result.bookId}, pageId=${result.pageId}")
        
        // 检查是否支持指读
        if (result.bookInfo?.supportFingerRead == true) {
            Log.d(TAG, "当前书本支持指读功能")
        }
        
        // 可以自动播放页面音频
        YueDuSDKManager.playPageAudio(
            result.bookId,
            result.pageId,
            result.pageType
        )
    }
    
    override fun onRecognizeFail(code: Int, baseBookId: Int, message: String) {
        // 显示识别失败提示
        Log.e(TAG, "识别失败: code=$code, message=$message")
        
        // 如果错误码是 1002，表示设备被挤下线
        if (code == 1002) {
            // 会触发 DeviceLogoutCallback
        }
    }
})
```

**错误码说明**：
- `-1`：SDK 未初始化
- `-2`：识别 SDK 未初始化，需要提供授权凭证
- `-3`：相机控制器未创建
- `-4`：识别 SDK 管理器未初始化
- `1002`：授权码正在被其他设备使用（会触发设备挤下线回调）

---

#### `stopRecognize()`

停止识别，停止书本识别并清除相关回调。

**示例**：
```kotlin
YueDuSDKManager.stopRecognize()
```

---

### 相机控制

#### `openCamera(cameraCtrl: CameraCtrl? = null)`

打开相机预览。

**参数**：
- `cameraCtrl: CameraCtrl?` - 相机控制器（如果为 null，则使用当前保存的相机控制器）

**注意**：
- 通常不需要手动调用此方法，`startRecognize` 内部会自动打开相机
- 如果需要在识别开始前单独打开相机预览，可以调用此方法
- 相机控制器会在 `startRecognize` 时自动创建，也可以手动传入自定义的 `CameraCtrl`

**示例**：
```kotlin
YueDuSDKManager.openCamera()
```

---

#### `closeCamera()`

关闭相机预览并释放资源。

**注意**：
- 在 Activity 的 `onPause()` 或 `onDestroy()` 中应该调用此方法释放相机资源
- 调用此方法后，相机预览会停止，但不会影响已保存的识别结果

**示例**：
```kotlin
YueDuSDKManager.closeCamera()
```

---

### 音频播放控制

#### `playPageAudio(bookId: Int, pageId: Int, pageType: Int)`

播放指定页面的音频。

**参数**：
- `bookId: Int` - 书本ID（从识别结果中获取）
- `pageId: Int` - 页面ID（从识别结果中获取）
- `pageType: Int` - 页面类型（从识别结果中获取）

**注意**：
- 此方法会自动下载和解压音频资源（如果还未下载）
- 下载进度会通过 `DownloadStateCallback` 回调通知
- 播放状态会通过 `AudioStateCallback` 回调通知

**示例**：
```kotlin
YueDuSDKManager.playPageAudio(bookId, pageId, pageType)
```

---

#### `pauseAudio()`

暂停当前正在播放的所有音频。

**注意**：
- 暂停后可以使用 `resumeAudio()` 恢复播放
- 暂停后可以使用 `replayAudio()` 重新播放

**示例**：
```kotlin
YueDuSDKManager.pauseAudio()
```

---

#### `resumeAudio()`

恢复之前暂停的音频播放。

**注意**：
- 只有在调用 `pauseAudio()` 暂停后，才能使用此方法恢复播放
- 如果没有暂停的音频，此方法不会产生任何效果

**示例**：
```kotlin
YueDuSDKManager.resumeAudio()
```

---

#### `replayAudio()`

重新播放当前页面的音频（基于最近一次识别或播放的书本和页面）。

**注意**：只有在成功识别到书本或调用过 `playPageAudio` 后，才能使用此方法重新播放。

**示例**：
```kotlin
YueDuSDKManager.replayAudio()
```

---

### 其他功能

#### `getOpenID(): String`

获取 OpenID。授权成功后，可以通过此方法获取 OpenID。

**返回值**：
- `String` - OpenID 字符串。如果 SDK 未初始化、识别 SDK 未初始化或授权未成功，返回空字符串。

**注意**：
- 只有在识别 SDK 初始化成功后（即调用 `startRecognize` 并成功初始化后），才能获取到有效的 OpenID
- 如果识别 SDK 还未初始化，此方法会返回空字符串

**示例**：
```kotlin
val openId = YueDuSDKManager.getOpenID()
if (openId.isNotEmpty()) {
    Log.d(TAG, "OpenID: $openId")
} else {
    Log.w(TAG, "OpenID 为空，可能还未授权或识别SDK未初始化")
}
```

---

### 状态回调设置

#### `setDownloadStateCallback(callback: DownloadStateCallback?)`

设置下载状态回调，用于监听书本资源的下载和解压进度。

**回调接口 `DownloadStateCallback`**：
```kotlin
interface DownloadStateCallback {
    /**
     * 下载准备
     */
    fun onDownloadPrepare(downloadId: Int, isForeground: Boolean)
    
    /**
     * 下载开始
     */
    fun onDownloadStart(downloadId: Int, isForeground: Boolean)
    
    /**
     * 下载中
     * @param progress 下载进度（0-100）
     */
    fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean)
    
    /**
     * 下载结束
     */
    fun onDownloadEnd(downloadId: Int, isForeground: Boolean)
    
    /**
     * 下载失败
     */
    fun onDownloadFail(downloadId: Int, isForeground: Boolean)
    
    /**
     * 解压开始
     */
    fun onUnzipStart(downloadId: Int, isForeground: Boolean)
    
    /**
     * 解压完成
     * @return 是否消费事件（返回 true 表示已处理，不再继续处理）
     */
    fun onUnzipComplete(downloadId: Int, isForeground: Boolean): Boolean
    
    /**
     * 解压错误
     */
    fun onUnzipError(downloadId: Int, errMsg: String, isForeground: Boolean)
}
```

**示例**：
```kotlin
YueDuSDKManager.setDownloadStateCallback(object : DownloadStateCallback {
    override fun onDownloadPrepare(downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 显示"准备下载"提示
            Toast.makeText(context, "准备下载书本资源...", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDownloadStart(downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 显示下载进度条
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
        }
    }
    
    override fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 更新下载进度
            progressBar.progress = progress
            progressText.text = "下载中: $progress%"
        }
    }
    
    override fun onDownloadEnd(downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 下载完成，准备解压
            progressText.text = "下载完成，正在解压..."
        }
    }
    
    override fun onDownloadFail(downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 显示下载失败提示
            progressBar.visibility = View.GONE
            Toast.makeText(context, "下载失败，请检查网络连接", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onUnzipStart(downloadId: Int, isForeground: Boolean) {
        runOnUiThread {
            // 显示"正在解压"提示
            progressText.text = "正在解压..."
        }
    }
    
    override fun onUnzipComplete(downloadId: Int, isForeground: Boolean): Boolean {
        runOnUiThread {
            // 解压完成，隐藏进度条
            progressBar.visibility = View.GONE
            progressText.text = ""
        }
        // 返回 false 表示不消费事件，继续处理
        return false
    }
    
    override fun onUnzipError(downloadId: Int, errMsg: String, isForeground: Boolean) {
        runOnUiThread {
            // 显示解压错误提示
            progressBar.visibility = View.GONE
            Toast.makeText(context, "解压失败: $errMsg", Toast.LENGTH_LONG).show()
        }
    }
})
```

---

#### `setAudioStateCallback(callback: AudioStateCallback?)`

设置播放状态回调，用于监听音频播放的开始和完成事件。

**回调接口 `AudioStateCallback`**：
```kotlin
interface AudioStateCallback {
    /**
     * 音频开始播放
     * @param type 音频类型（例如：VTAudioCtrl.TYPE_PAGE_READING_AUDIO 表示页面朗读音频）
     * @param id 音频ID
     */
    fun onAudioStart(type: Int, id: Int)
    
    /**
     * 音频播放完成
     * @param type 音频类型
     * @param id 音频ID
     */
    fun onAudioComplete(type: Int, id: Int)
}
```

**示例**：
```kotlin
YueDuSDKManager.setAudioStateCallback(object : AudioStateCallback {
    override fun onAudioStart(type: Int, id: Int) {
        runOnUiThread {
            // 更新 UI，显示"正在播放"状态
            playButton.text = "播放中..."
            playButton.isEnabled = false
        }
    }
    
    override fun onAudioComplete(type: Int, id: Int) {
        runOnUiThread {
            // 更新 UI，显示"播放完成"状态
            playButton.text = "播放"
            playButton.isEnabled = true
        }
    }
})
```

---

#### `setDeviceLogoutCallback(callback: DeviceLogoutCallback?)`

设置设备被挤下线回调，当授权码被另一台设备使用时，会触发此回调。

**回调接口 `DeviceLogoutCallback`**：
```kotlin
interface DeviceLogoutCallback {
    /**
     * 设备被另一台机器挤下线
     * 当授权码正在被其他设备使用时，会调用此方法
     */
    fun onDeviceLogout()
}
```

**示例**：
```kotlin
YueDuSDKManager.setDeviceLogoutCallback(object : DeviceLogoutCallback {
    override fun onDeviceLogout() {
        // 清理本地保存的 license
        AuthPrefsUtil.clearLicense()
        
        // 停止识别
        YueDuSDKManager.stopRecognize()
        
        // 提示用户并跳转到授权页面
        AlertDialog.Builder(this@MainActivity)
            .setTitle("提示")
            .setMessage("您的账号已在其他设备登录，请重新授权")
            .setPositiveButton("确定") { _, _ ->
                // 跳转到授权页面
                startActivity(Intent(this@MainActivity, AuthActivity::class.java))
            }
            .show()
    }
})
```

---

## 🔄 典型使用流程

### 1. 初始化 SDK

在 App 启动时（如 `Application.onCreate()` 或主 Activity 的 `onCreate()`）调用：

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 异步初始化 SDK
        YueDuSDKManager.initializeAsync(applicationContext) { success, error ->
            if (success) {
                Log.d(TAG, "SDK 初始化成功")
                // 可以继续后续操作
            } else {
                Log.e(TAG, "SDK 初始化失败: $error")
                // 提示用户稍后重试
            }
        }
    }
}
```

### 2. 授权认证

在授权页面（扫码或手动输入授权码）：

```kotlin
class AuthActivity : AppCompatActivity() {
    private fun performAuth(qrCode: String) {
        YueDuSDKManager.authorize(qrCode, object : AuthCallback {
            override fun onAuthSuccess(license: String) {
                // 保存 license
                AuthPrefsUtil.saveLicense(license)
                
                // 返回主页面
                setResult(Activity.RESULT_OK)
                finish()
            }
            
            override fun onAuthFail(code: Int, message: String) {
                // 显示错误提示
                Toast.makeText(this@AuthActivity, "授权失败: $message", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
```

### 3. 开始识别

在识别页面（需要相机预览）：

```kotlin
class RecognizeActivity : AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognize)
        
        // 初始化 SurfaceView
        surfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        
        // 设置状态回调
        setupCallbacks()
        
        // 获取保存的 license
        val license = AuthPrefsUtil.getLicense()
        
        // 开始识别
        startRecognize(license)
    }
    
    private fun setupCallbacks() {
        // 设置下载状态回调
        YueDuSDKManager.setDownloadStateCallback(object : DownloadStateCallback {
            override fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean) {
                runOnUiThread {
                    progressBar.progress = progress
                }
            }
            // ... 其他回调方法
        })
        
        // 设置播放状态回调
        YueDuSDKManager.setAudioStateCallback(object : AudioStateCallback {
            override fun onAudioStart(type: Int, id: Int) {
                runOnUiThread {
                    playButton.text = "播放中..."
                }
            }
            
            override fun onAudioComplete(type: Int, id: Int) {
                runOnUiThread {
                    playButton.text = "播放"
                }
            }
        })
        
        // 设置设备挤下线回调
        YueDuSDKManager.setDeviceLogoutCallback(object : DeviceLogoutCallback {
            override fun onDeviceLogout() {
                runOnUiThread {
                    handleDeviceLogout()
                }
            }
        })
    }
    
    private fun startRecognize(license: String?) {
        YueDuSDKManager.startRecognize(surfaceHolder, license, object : RecognizeCallback {
            override fun onRecognizeSuccess(result: RecognizeResult) {
                runOnUiThread {
                    // 更新 UI，显示识别结果
                    bookNameTextView.text = result.bookInfo?.bookName ?: "未知"
                    pageIdTextView.text = "页面 ID: ${result.pageId}"
                    
                    // 检查是否支持指读
                    if (result.bookInfo?.supportFingerRead == true) {
                        // 显示指读功能提示
                    }
                    
                    // 可以自动播放音频
                    YueDuSDKManager.playPageAudio(
                        result.bookId,
                        result.pageId,
                        result.pageType
                    )
                }
            }
            
            override fun onRecognizeFail(code: Int, baseBookId: Int, message: String) {
                runOnUiThread {
                    Toast.makeText(this@RecognizeActivity, "识别失败: $message", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
    
    private fun handleDeviceLogout() {
        // 清理本地 license
        AuthPrefsUtil.clearLicense()
        
        // 停止识别
        YueDuSDKManager.stopRecognize()
        
        // 提示并返回
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("您的账号已在其他设备登录，请重新授权")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止识别并关闭相机
        YueDuSDKManager.stopRecognize()
        YueDuSDKManager.closeCamera()
    }
}
```

### 4. 播放控制

在识别页面添加播放控制按钮：

```kotlin
// 播放按钮
playButton.setOnClickListener {
    val result = currentRecognizeResult // 保存的识别结果
    if (result != null) {
        YueDuSDKManager.playPageAudio(
            result.bookId,
            result.pageId,
            result.pageType
        )
    }
}

// 暂停按钮
pauseButton.setOnClickListener {
    YueDuSDKManager.pauseAudio()
}

// 恢复按钮
resumeButton.setOnClickListener {
    YueDuSDKManager.resumeAudio()
}

// 重新播放按钮
replayButton.setOnClickListener {
    YueDuSDKManager.replayAudio()
}
```

---

## ⚠️ 注意事项

### 1. 初始化顺序

- 必须先调用 `initialize()` 或 `initializeAsync()` 初始化基础 SDK
- 在调用 `authorize()` 或 `startRecognize()` 之前，确保基础 SDK 已初始化
- `startRecognize()` 内部会自动初始化识别 SDK（如果还未初始化），但需要提供有效的 `license`
- 识别 SDK 初始化成功后，后续调用 `startRecognize()` 时可以传 `null` 作为 `license` 参数

### 2. License 管理

- 授权成功后，务必保存 `license` 到本地（如 SharedPreferences）
- 首次调用 `startRecognize()` 时，必须提供有效的 `license` 用于初始化识别 SDK
- 识别 SDK 初始化成功后，后续调用 `startRecognize()` 时可以传 `null` 作为 `license` 参数
- 如果收到设备挤下线回调，需要清理本地保存的 `license`，并重新授权

### 3. 相机权限

- 必须在运行时动态申请相机权限
- 在调用 `startRecognize()` 之前，确保已获得相机权限

### 4. 生命周期管理

- 在 Activity 的 `onDestroy()` 中调用 `stopRecognize()` 和 `closeCamera()`，避免资源泄漏
- 在页面切换时，及时停止识别和关闭相机

### 5. 线程安全

- 所有回调方法可能在不同线程执行，更新 UI 时需要使用 `runOnUiThread()` 或 `Handler`

### 6. 错误处理

- 错误码 `1002` 表示授权码正在被其他设备使用，会触发 `DeviceLogoutCallback`
- 识别失败时，检查错误码和错误信息，给用户友好的提示

### 7. OpenID 获取

- `getOpenID()` 方法只有在识别 SDK 初始化成功后（即调用 `startRecognize` 并成功初始化后）才能获取到有效的 OpenID
- 如果识别 SDK 还未初始化，此方法会返回空字符串

---

## 📝 数据模型

### RecognizeResult（识别结果）

```kotlin
data class RecognizeResult(
    val bookId: Int,              // 书本ID
    val pageId: Int,              // 页面ID
    val pageType: Int,            // 页面类型
    val elapsedTime: Long,        // 识别耗时（毫秒）
    val bookInfo: BookInfo?       // 书本详细信息（可能为 null）
)
```

### BookInfo（书本信息）

```kotlin
data class BookInfo(
    val bookId: Int,                    // 书本ID
    val bookName: String,               // 书本名称
    val isbn: String,                   // ISBN号码
    val publisher: String,              // 出版社
    val author: String,                 // 作者
    val description: String,           // 书本描述
    val coverImage: String,            // 封面图片URL
    val thumbnailCoverImage: String,   // 缩略图封面图片URL
    val supportFingerRead: Boolean     // 是否支持手指点读（指读）
)
```

---

## 🔍 错误码参考

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| -1 | SDK 未初始化 | 先调用 `initialize()` 或 `initializeAsync()` |
| -2 | 二维码内容为空 / 识别 SDK 未初始化且未提供 license | 检查输入参数 |
| -3 | 相机控制器未创建 | 检查 Context 是否正确 |
| -4 | 识别 SDK 管理器未初始化 | 检查初始化流程 |
| 1002 | 授权码正在被其他设备使用 | 触发设备挤下线回调，清理本地 license |

---

## 📞 技术支持

如有问题，请联系技术支持或查看项目 README。

---

## 📄 更新日志

### v1.0.1
- 初始版本发布
- 支持 SDK 初始化、授权认证、书本识别、音频播放等功能
- 支持获取 OpenID
- BookInfo 增加 `supportFingerRead` 字段，用于标识书本是否支持指读功能

