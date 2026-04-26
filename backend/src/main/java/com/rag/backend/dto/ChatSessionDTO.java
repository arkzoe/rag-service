package com.rag.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话会话DTO
 */
@Data
public class ChatSessionDTO {

    /**
     * 会话ID
     */
    private String sessionId;

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
     * 最后一条消息预览
     */
    private String lastMessagePreview;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
