package com.rag.backend.service;

import com.rag.backend.entity.ChatHistory;

import java.util.List;

public interface RagService {
    /**
     * 执行RAG问答流程
     * @param question 用户问题
     * @param userRoles 用户角色列表
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return AI回答
     */
    String chat(String question, List<String> userRoles, Long userId, String sessionId);

    /**
     * 获取会话历史记录
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 历史记录列表
     */
    List<ChatHistory> getSessionHistory(String sessionId, Long userId);
}
