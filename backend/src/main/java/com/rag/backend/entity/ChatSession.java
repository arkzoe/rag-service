package com.rag.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话会话表
 * @TableName chat_session
 */
@TableName(value = "chat_session")
@Data
public class ChatSession implements Serializable {

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
     * 对话标题
     */
    private String title;

    /**
     * 对话描述
     */
    private String description;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 最后一条消息内容预览
     */
    private String lastMessagePreview;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否删除 0-未删除 1-已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
