package uni.lv.yuedu.model

import java.io.Serializable

/**
 * 识别结果数据模型
 * 用于封装书本识别成功后返回的数据
 */
data class RecognizeResult(
    /**
     * 书本ID
     */
    val bookId: Int,
    
    /**
     * 页面ID
     */
    val pageId: Int,
    
    /**
     * 页面类型
     */
    val pageType: Int,
    
    /**
     * 识别耗时（毫秒）
     */
    val elapsedTime: Long,
    
    /**
     * 书本详细信息
     */
    val bookInfo: BookInfo?
) : Serializable



