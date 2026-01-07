package uni.lv.yuedu.model

import java.io.Serializable

/**
 * 书本信息数据模型
 * 用于封装识别到的书本详细信息
 */
data class BookInfo(
    /**
     * 书本ID
     */
    val bookId: Int,
    
    /**
     * 书本名称
     */
    val bookName: String,
    
    /**
     * ISBN号码
     */
    val isbn: String,
    
    /**
     * 出版社
     */
    val publisher: String,
    
    /**
     * 作者
     */
    val author: String,
    
    /**
     * 书本描述
     */
    val description: String,
    
    /**
     * 封面图片URL
     */
    val coverImage: String,
    
    /**
     * 缩略图封面图片URL
     */
    val thumbnailCoverImage: String
) : Serializable



