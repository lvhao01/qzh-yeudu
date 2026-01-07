package uni.lv.readsdk.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 认证状态管理工具类
 * 使用SharedPreferences保存和读取认证状态和授权凭证
 */
object AuthPrefsUtil {
    private const val PREFS_NAME = "readsdk_auth_prefs"
    private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    private const val KEY_LICENSE = "license"

    /**
     * 获取SharedPreferences实例
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存认证状态和授权凭证
     * @param context 上下文
     * @param license 授权凭证
     */
    fun saveAuth(context: Context, license: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putBoolean(KEY_IS_AUTHENTICATED, true)
            .putString(KEY_LICENSE, license)
            .apply()
    }

    /**
     * 清除认证状态和授权凭证
     * @param context 上下文
     */
    fun clearAuth(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putBoolean(KEY_IS_AUTHENTICATED, false)
            .remove(KEY_LICENSE)
            .apply()
    }

    /**
     * 检查是否已认证
     * @param context 上下文
     * @return true表示已认证，false表示未认证
     */
    fun isAuthenticated(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false)
    }

    /**
     * 获取保存的授权凭证
     * @param context 上下文
     * @return 授权凭证，如果未保存则返回null
     */
    fun getLicense(context: Context): String? {
        val prefs = getSharedPreferences(context)
        val license = prefs.getString(KEY_LICENSE, null)
        // 如果license为空字符串，返回null
        return if (license.isNullOrEmpty()) null else license
    }

    /**
     * 检查是否有有效的授权凭证
     * @param context 上下文
     * @return true表示有有效的授权凭证，false表示没有
     */
    fun hasValidLicense(context: Context): Boolean {
        return isAuthenticated(context) && !getLicense(context).isNullOrEmpty()
    }
}


