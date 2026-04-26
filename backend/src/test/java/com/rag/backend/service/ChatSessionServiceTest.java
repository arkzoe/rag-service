package com.rag.backend.service;

import com.rag.backend.dto.ChatSessionDTO;
import com.rag.backend.dto.request.CreateSessionRequest;
import com.rag.backend.entity.ChatSession;
import com.rag.backend.infrastructure.RedisUtil;
import com.rag.backend.mapper.ChatSessionMapper;
import com.rag.backend.service.impl.ChatSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 对话会话服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private ChatSessionServiceImpl chatSessionService;

    private Long testUserId;
    private String testSessionId;
    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testSessionId = UUID.randomUUID().toString();

        testSession = new ChatSession();
        testSession.setId(1L);
        testSession.setSessionId(testSessionId);
        testSession.setUserId(testUserId);
        testSession.setTitle("测试对话");
        testSession.setDescription("测试描述");
        testSession.setMessageCount(0);
        testSession.setCreatedAt(LocalDateTime.now());
        testSession.setUpdatedAt(LocalDateTime.now());
        testSession.setIsDeleted(0);
    }

    @Test
    void createSession_Success() {
        // Given
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("新对话");
        request.setDescription("新描述");

        when(chatSessionMapper.insert(any(ChatSession.class))).thenReturn(1);
        when(redisUtil.delete(anyString())).thenReturn(true);
        when(redisUtil.set(anyString(), any(), anyLong(), any())).thenReturn(true);

        // When
        ChatSessionDTO result = chatSessionService.createSession(testUserId, request);

        // Then
        assertNotNull(result);
        assertEquals("新对话", result.getTitle());
        assertEquals("新描述", result.getDescription());
        assertNotNull(result.getSessionId());

        verify(chatSessionMapper).insert(any(ChatSession.class));
        verify(redisUtil).delete(anyString());
        verify(redisUtil).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getUserSessions_FromCache() {
        // Given
        List<ChatSessionDTO> cachedList = Arrays.asList(new ChatSessionDTO(), new ChatSessionDTO());
        when(redisUtil.get(anyString())).thenReturn(cachedList);

        // When
        List<ChatSessionDTO> result = chatSessionService.getUserSessions(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(chatSessionMapper, never()).selectByUserId(anyLong());
    }

    @Test
    void getUserSessions_FromDatabase() {
        // Given
        when(redisUtil.get(anyString())).thenReturn(null);
        when(chatSessionMapper.selectByUserId(testUserId)).thenReturn(Arrays.asList(testSession));
        when(redisUtil.set(anyString(), any(), anyLong(), any())).thenReturn(true);

        // When
        List<ChatSessionDTO> result = chatSessionService.getUserSessions(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试对话", result.get(0).getTitle());

        verify(chatSessionMapper).selectByUserId(testUserId);
        verify(redisUtil).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getSessionDetail_FromCache() {
        // Given
        when(redisUtil.get(anyString())).thenReturn(testSession);

        // When
        ChatSessionDTO result = chatSessionService.getSessionDetail(testSessionId);

        // Then
        assertNotNull(result);
        assertEquals("测试对话", result.getTitle());
        verify(chatSessionMapper, never()).selectBySessionId(anyString());
    }

    @Test
    void getSessionDetail_FromDatabase() {
        // Given
        when(redisUtil.get(anyString())).thenReturn(null);
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(testSession);
        when(redisUtil.set(anyString(), any(), anyLong(), any())).thenReturn(true);

        // When
        ChatSessionDTO result = chatSessionService.getSessionDetail(testSessionId);

        // Then
        assertNotNull(result);
        assertEquals("测试对话", result.getTitle());
        verify(chatSessionMapper).selectBySessionId(testSessionId);
    }

    @Test
    void getSessionDetail_NotFound() {
        // Given
        when(redisUtil.get(anyString())).thenReturn(null);
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(null);

        // When
        ChatSessionDTO result = chatSessionService.getSessionDetail(testSessionId);

        // Then
        assertNull(result);
    }

    @Test
    void deleteSession_Success() {
        // Given
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(testSession);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);
        when(redisUtil.delete(anyString())).thenReturn(true);

        // When
        chatSessionService.deleteSession(testUserId, testSessionId);

        // Then
        verify(chatSessionMapper).updateById(any(ChatSession.class));
        verify(redisUtil, times(2)).delete(anyString());
    }

    @Test
    void deleteSession_NotFound() {
        // Given
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatSessionService.deleteSession(testUserId, testSessionId);
        });
        assertEquals("会话不存在或无权删除", exception.getMessage());
    }

    @Test
    void deleteSession_Unauthorized() {
        // Given
        testSession.setUserId(999L); // 不同用户
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(testSession);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatSessionService.deleteSession(testUserId, testSessionId);
        });
        assertEquals("会话不存在或无权删除", exception.getMessage());
    }

    @Test
    void batchDeleteSessions_Success() {
        // Given
        List<String> sessionIds = Arrays.asList(testSessionId, UUID.randomUUID().toString());

        when(chatSessionMapper.selectBySessionId(anyString())).thenReturn(testSession);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);
        when(redisUtil.delete(anyString())).thenReturn(true);

        // When
        chatSessionService.batchDeleteSessions(testUserId, sessionIds);

        // Then
        verify(chatSessionMapper, times(2)).selectBySessionId(anyString());
        verify(chatSessionMapper, times(2)).updateById(any(ChatSession.class));
    }

    @Test
    void updateLastMessage_Success() {
        // Given
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(testSession);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);
        when(redisUtil.set(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(redisUtil.delete(anyString())).thenReturn(true);

        // When
        chatSessionService.updateLastMessage(testSessionId, "最后消息预览", 1);

        // Then
        verify(chatSessionMapper).updateById(any(ChatSession.class));
        verify(redisUtil).set(anyString(), any(), anyLong(), any());
        verify(redisUtil).delete(anyString());
    }

    @Test
    void updateLastMessage_SessionNotFound() {
        // Given
        when(chatSessionMapper.selectBySessionId(testSessionId)).thenReturn(null);

        // When
        chatSessionService.updateLastMessage(testSessionId, "最后消息预览", 1);

        // Then
        verify(chatSessionMapper, never()).updateById(any(ChatSession.class));
    }
}
