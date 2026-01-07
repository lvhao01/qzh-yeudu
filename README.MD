# 项目环境
gradle-8.9  
Gradle JDK：jbr-21（JetBrains Runtime 21.0.3，D:/Android Studio/jbr）

# yuedu 插件提供的能力
- SDK 初始化：同步/异步初始化基础 SDK、初始化识别 SDK。
- 授权认证：扫描二维码内容后获取 license。
- 识别：启动/停止识别，回调返回书本 ID、页面 ID、页面类型、耗时、书本信息。
- 相机控制：打开/关闭相机，内部管理 `CameraCtrl`。
- 播放控制：播放页面音频、暂停、恢复、重新播放当前页音频。
- 状态回调：
  - 下载状态：下载/解压进度与结果（`DownloadStateCallback`）。
  - 播放状态：音频开始、完成（`AudioStateCallback`）。
  - 设备挤下线：授权被其他设备占用时的回调（`DeviceLogoutCallback`）。

# app 使用流程（示例）
1. 初始化  
   - 启动时调用 `YueDuSDKManager.initializeAsync(context)`（或 `initialize`）。

2. 授权  
   - 通过扫码（`QRCodeScanActivity`）或手动输入，调用 `AuthActivity`。  
   - `AuthActivity` 内部调用 `YueDuSDKManager.authorize(qrCode, AuthCallback)` 获取 license 并返回给 `MainActivity`。  
   - `MainActivity` 保存 license（`AuthPrefsUtil`）并更新 UI。

3. 识别  
   - `MainActivity` 携带 license 打开 `RecognizeActivity`。  
   - `RecognizeActivity` 准备相机 `SurfaceView`，调用 `YueDuSDKManager.startRecognize(surfaceHolder, license, RecognizeCallback)`。  
   - 成功回调 `onRecognizeSuccess`：收到书本/页面信息，界面显示；失败回调 `onRecognizeFail`。

4. 播放控制  
   - `RecognizeActivity` 通过按钮调用 `YueDuSDKManager.pauseAudio()/resumeAudio()/replayAudio()`；播放状态由 `AudioStateCallback` 更新 UI。

5. 下载状态展示  
   - `RecognizeActivity` 设置 `setDownloadStateCallback`，实时显示下载/解压进度与结果。

6. 设备挤下线  
   - 如收到授权码被占用（错误码 1002），通过 `DeviceLogoutCallback` 通知上层处理。

# 流程图（Mermaid）
```mermaid
flowchart TD
    A[App 启动] --> B{SDK 已初始化?}
    B -- 否 --> C[initializeAsync / initialize]
    B -- 是 --> D[跳过]
    C --> D
    D --> E[授权入口: 扫码或手动输入]
    E --> F[AuthActivity 调用 authorize]
    F -- 成功 --> G[保存 license (AuthPrefsUtil)]
    F -- 失败 --> H[提示失败/重试]
    G --> I[进入 RecognizeActivity]
    I --> J[准备 SurfaceView / 相机权限]
    J --> K[设置回调 DownloadState / AudioState / Recognize]
    K --> L[startRecognize(holder, license, RecognizeCallback)]
    L --> M{识别结果}
    M -- 成功 --> N[更新 UI 显示书本/页面信息<br/>可 play / pause / resume / replay]
    M -- 失败 --> O[更新 UI 显示失败信息]
    N --> P[AudioStateCallback 更新播放状态]
    K --> Q[DownloadStateCallback 显示下载/解压进度]
    L --> R[DeviceLogoutCallback 处理挤下线]
```