package com.rag.backend.service;

import java.util.List;

public interface RagService {
    /**
     * 执行RAG问答流程
     * @param question 用户问题
     * @param userRoles 用户角色列表
     * @param userId 用户ID
     * @return AI回答
     */
    String chat(String question, List<String> userRoles, Long userId);
}
