package com.rag.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会话历史记录表
 * @TableName chat_history
 */
@TableName(value ="chat_history")
@Data
public class ChatHistory implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话唯一标识 (UUID)
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户提问内容
     */
    private String query;

    /**
     * AI 回答内容
     */
    private String answer;

    /**
     * 消耗 Token 数
     */
    private Integer tokensUsed;

    /**
     * 引用文档数量
     */
    private Integer sourceCount;

    /**
     * 用户反馈: 1-点赞, 0-点踩
     */
    private Integer feedback;

    /**
     * 
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}