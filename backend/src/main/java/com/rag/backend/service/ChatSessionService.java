package com.rag.backend.service;

import com.rag.backend.dto.ChatSessionDTO;
import com.rag.backend.dto.request.CreateSessionRequest;
import com.rag.backend.dto.request.UpdateSessionRequest;
import com.rag.backend.entity.ChatSession;

import java.util.List;

/**
 * 对话会话服务接口
 */
public interface ChatSessionService {

    /**
     * 创建对话会话
     *
     * @param userId  用户ID
     * @param request 创建请求
     * @return 会话DTO
     */
    ChatSessionDTO createSession(Long userId, CreateSessionRequest request);

    /**
     * 获取用户的对话列表
     *
     * @param userId 用户ID
     * @return 对话列表
     */
    List<ChatSessionDTO> getUserSessions(Long userId);

    /**
     * 获取会话详情
     *
     * @param sessionId 会话ID
     * @return 会话DTO
     */
    ChatSessionDTO getSessionDetail(String sessionId);

    /**
     * 删除对话会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    void deleteSession(Long userId, String sessionId);

    /**
     * 批量删除对话会话
     *
     * @param userId     用户ID
     * @param sessionIds 会话ID列表
     */
    void batchDeleteSessions(Long userId, List<String> sessionIds);

    /**
     * 更新会话信息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param request   更新请求
     */
    ChatSessionDTO updateSession(Long userId, String sessionId, UpdateSessionRequest request);

    /**
     * 更新会话最后消息
     *
     * @param sessionId  会话ID
     * @param preview    消息预览
     * @param increment  消息数增量
     */
    void updateLastMessage(String sessionId, String preview, int increment);
}
