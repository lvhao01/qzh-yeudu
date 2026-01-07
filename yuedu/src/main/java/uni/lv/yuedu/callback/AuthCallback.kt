package uni.lv.yuedu.callback

/**
 * 授权认证回调接口
 */
interface AuthCallback {
    /**
     * 授权成功
     * @param license 授权凭证
     */
    fun onAuthSuccess(license: String)

    /**
     * 授权失败
     * @param code 错误码
     * @param message 错误信息
     */
    fun onAuthFail(code: Int, message: String)
}



