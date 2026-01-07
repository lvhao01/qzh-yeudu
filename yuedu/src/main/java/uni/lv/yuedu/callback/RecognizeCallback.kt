package uni.lv.yuedu.callback

import uni.lv.yuedu.model.RecognizeResult

/**
 * 书本识别回调接口
 */
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


