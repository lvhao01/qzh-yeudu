package uni.lv.yuedu.callback

/**
 * 播放状态回调接口
 */
interface AudioStateCallback {
    /**
     * 音频开始播放
     * @param type 音频类型（例如：VTAudioCtrl.TYPE_PAGE_READING_AUDIO表示页面朗读音频）
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

