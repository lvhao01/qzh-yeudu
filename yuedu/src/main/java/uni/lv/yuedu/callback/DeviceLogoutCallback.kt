package uni.lv.yuedu.callback

/**
 * 设备被挤下线回调接口
 * 当授权码被另一台设备使用时，会触发此回调
 */
interface DeviceLogoutCallback {
    /**
     * 设备被另一台机器挤下线
     * 当授权码正在被其他设备使用时，会调用此方法
     */
    fun onDeviceLogout()
}


