package uni.lv.yuedu.callback

/**
 * 下载状态回调接口
 */
interface DownloadStateCallback {
    /**
     * 下载准备
     * @param downloadId 下载ID（通常是bookId）
     * @param isForeground 是否前台下载
     */
    fun onDownloadPrepare(downloadId: Int, isForeground: Boolean)

    /**
     * 下载开始
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     */
    fun onDownloadStart(downloadId: Int, isForeground: Boolean)

    /**
     * 下载中
     * @param progress 下载进度（0-100）
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     */
    fun onDownloading(progress: Int, downloadId: Int, isForeground: Boolean)

    /**
     * 下载结束
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     */
    fun onDownloadEnd(downloadId: Int, isForeground: Boolean)

    /**
     * 下载失败
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     */
    fun onDownloadFail(downloadId: Int, isForeground: Boolean)

    /**
     * 解压开始
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     */
    fun onUnzipStart(downloadId: Int, isForeground: Boolean)

    /**
     * 解压完成
     * @param downloadId 下载ID
     * @param isForeground 是否前台下载
     * @return 是否消费事件（返回true表示已处理，不再继续处理）
     */
    fun onUnzipComplete(downloadId: Int, isForeground: Boolean): Boolean

    /**
     * 解压错误
     * @param downloadId 下载ID
     * @param errMsg 错误信息
     * @param isForeground 是否前台下载
     */
    fun onUnzipError(downloadId: Int, errMsg: String, isForeground: Boolean)
}

