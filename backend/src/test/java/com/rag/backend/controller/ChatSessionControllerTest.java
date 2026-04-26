package com.rag.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.backend.dto.ChatSessionDTO;
import com.rag.backend.dto.request.CreateSessionRequest;
import com.rag.backend.dto.request.DeleteSessionRequest;
import com.rag.backend.service.ChatSessionService;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 对话会话控制器集成测试
 */
@WebMvcTest(ChatSessionController.class)
class ChatSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatSessionService chatSessionService;

    private ChatSessionDTO testSessionDTO;

    @BeforeEach
    void setUp() {
        testSessionDTO = new ChatSessionDTO();
        testSessionDTO.setSessionId(UUID.randomUUID().toString());
        testSessionDTO.setTitle("测试对话");
        testSessionDTO.setDescription("测试描述");
        testSessionDTO.setMessageCount(0);
        testSessionDTO.setCreatedAt(LocalDateTime.now());
        testSessionDTO.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createSession_Success() throws Exception {
        // Given
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("新对话");
        request.setDescription("新描述");

        when(chatSessionService.createSession(anyLong(), any())).thenReturn(testSessionDTO);

        // When & Then
        mockMvc.perform(post("/api/chat/session/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.title").value("测试对话"));

        verify(chatSessionService).createSession(anyLong(), any());
    }

    @Test
    void createSession_ValidationError() throws Exception {
        // Given - 空标题
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("");

        // When & Then
        mockMvc.perform(post("/api/chat/session/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserSessions_Success() throws Exception {
        // Given
        when(chatSessionService.getUserSessions(anyLong()))
                .thenReturn(Arrays.asList(testSessionDTO, testSessionDTO));

        // When & Then
        mockMvc.perform(get("/api/chat/session/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(chatSessionService).getUserSessions(anyLong());
    }

    @Test
    void getSessionDetail_Success() throws Exception {
        // Given
        String sessionId = UUID.randomUUID().toString();
        when(chatSessionService.getSessionDetail(sessionId)).thenReturn(testSessionDTO);

        // When & Then
        mockMvc.perform(get("/api/chat/session/detail/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value(testSessionDTO.getSessionId()));

        verify(chatSessionService).getSessionDetail(sessionId);
    }

    @Test
    void getSessionDetail_NotFound() throws Exception {
        // Given
        String sessionId = UUID.randomUUID().toString();
        when(chatSessionService.getSessionDetail(sessionId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/chat/session/detail/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("会话不存在"));
    }

    @Test
    void deleteSession_Success() throws Exception {
        // Given
        String sessionId = UUID.randomUUID().toString();
        doNothing().when(chatSessionService).deleteSession(anyLong(), eq(sessionId));

        // When & Then
        mockMvc.perform(post("/api/chat/session/delete/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatSessionService).deleteSession(anyLong(), eq(sessionId));
    }

    @Test
    void batchDeleteSessions_Success() throws Exception {
        // Given
        DeleteSessionRequest request = new DeleteSessionRequest();
        request.setSessionIds(Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ));

        doNothing().when(chatSessionService).batchDeleteSessions(anyLong(), anyList());

        // When & Then
        mockMvc.perform(post("/api/chat/session/delete/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedCount").value(2));

        verify(chatSessionService).batchDeleteSessions(anyLong(), anyList());
    }

    @Test
    void batchDeleteSessions_ValidationError() throws Exception {
        // Given - 空列表
        DeleteSessionRequest request = new DeleteSessionRequest();
        request.setSessionIds(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/chat/session/delete/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
