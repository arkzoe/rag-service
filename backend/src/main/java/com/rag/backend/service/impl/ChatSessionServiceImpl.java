package com.rag.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.backend.common.constant.RedisKeyConstants;
import com.rag.backend.dto.ChatSessionDTO;
import com.rag.backend.dto.request.CreateSessionRequest;
import com.rag.backend.dto.request.UpdateSessionRequest;
import com.rag.backend.entity.ChatSession;
import com.rag.backend.infrastructure.RedisUtil;
import com.rag.backend.mapper.ChatSessionMapper;
import com.rag.backend.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>  implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionDTO createSession(Long userId, CreateSessionRequest request) {
        log.info("创建对话会话 - 用户ID: {}, 标题: {}", userId, request.getTitle());

        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setIsDeleted(0);

        chatSessionMapper.insert(session);

        // 清除用户会话列表缓存
        String listKey = RedisKeyConstants.getSessionListKey(userId);
        redisUtil.delete(listKey);

        // 缓存新会话
        String sessionKey = RedisKeyConstants.getSessionKey(session.getSessionId());
        redisUtil.set(sessionKey, session, RedisKeyConstants.CHAT_SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

        log.info("对话会话创建成功 - SessionID: {}", session.getSessionId());
        return convertToDTO(session);
    }

    @Override
    public List<ChatSessionDTO> getUserSessions(Long userId) {
        String cacheKey = RedisKeyConstants.getSessionListKey(userId);

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<ChatSessionDTO> cachedList = (List<ChatSessionDTO>) redisUtil.get(cacheKey);
        if (cachedList != null) {
            log.debug("从缓存获取用户对话列表 - 用户ID: {}", userId);
            return cachedList;
        }

        log.info("从数据库获取用户对话列表 - 用户ID: {}", userId);
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 写入缓存
        redisUtil.set(cacheKey, dtoList, RedisKeyConstants.CHAT_SESSION_LIST_EXPIRE_HOURS, TimeUnit.HOURS);

        return dtoList;
    }

    @Override
    public ChatSessionDTO getSessionDetail(String sessionId) {
        String cacheKey = RedisKeyConstants.getSessionKey(sessionId);

        // 尝试从缓存获取
        ChatSession cachedSession = (ChatSession) redisUtil.get(cacheKey);
        if (cachedSession != null) {
            log.debug("从缓存获取会话详情 - SessionID: {}", sessionId);
            return convertToDTO(cachedSession);
        }

        log.info("从数据库获取会话详情 - SessionID: {}", sessionId);
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            return null;
        }

        // 写入缓存
        redisUtil.set(cacheKey, session, RedisKeyConstants.CHAT_SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

        return convertToDTO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, String sessionId) {
        log.info("删除对话会话 - 用户ID: {}, SessionID: {}", userId, sessionId);

        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权删除");
        }

        session.setIsDeleted(1);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 清除缓存
        redisUtil.delete(RedisKeyConstants.getSessionKey(sessionId));
        redisUtil.delete(RedisKeyConstants.getSessionListKey(userId));

        log.info("对话会话删除成功 - SessionID: {}", sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteSessions(Long userId, List<String> sessionIds) {
        log.info("批量删除对话会话 - 用户ID: {}, 数量: {}", userId, sessionIds.size());

        for (String sessionId : sessionIds) {
            try {
                deleteSession(userId, sessionId);
            } catch (Exception e) {
                log.warn("删除会话失败 - SessionID: {}, 错误: {}", sessionId, e.getMessage());
            }
        }

        log.info("批量删除对话会话完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionDTO updateSession(Long userId, String sessionId, UpdateSessionRequest request) {
        log.info("更新对话会话 - 用户ID: {}, SessionID: {}, 标题: {}", userId, sessionId, request.getTitle());

        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权修改");
        }

        session.setTitle(request.getTitle());
        if (request.getDescription() != null) {
            session.setDescription(request.getDescription());
        }
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 更新缓存
        String cacheKey = RedisKeyConstants.getSessionKey(sessionId);
        redisUtil.set(cacheKey, session, RedisKeyConstants.CHAT_SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

        // 清除用户会话列表缓存
        redisUtil.delete(RedisKeyConstants.getSessionListKey(userId));

        log.info("对话会话更新成功 - SessionID: {}", sessionId);
        return convertToDTO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(String sessionId, String preview, int increment) {
        ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
        if (session == null) {
            return;
        }

        session.setLastMessagePreview(preview);
        session.setMessageCount(session.getMessageCount() + increment);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        // 更新缓存
        String cacheKey = RedisKeyConstants.getSessionKey(sessionId);
        redisUtil.set(cacheKey, session, RedisKeyConstants.CHAT_SESSION_EXPIRE_HOURS, TimeUnit.HOURS);

        // 清除用户会话列表缓存（因为更新时间变了）
        redisUtil.delete(RedisKeyConstants.getSessionListKey(session.getUserId()));
    }

    /**
     * 转换为DTO
     */
    private ChatSessionDTO convertToDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        BeanUtils.copyProperties(session, dto);
        return dto;
    }
}
