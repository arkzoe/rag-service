package com.rag.backend.controller;

import com.rag.backend.common.result.Result;
import com.rag.backend.dto.ChatSessionDTO;
import com.rag.backend.dto.request.CreateSessionRequest;
import com.rag.backend.dto.request.DeleteSessionRequest;
import com.rag.backend.dto.request.UpdateSessionRequest;
import com.rag.backend.service.ChatSessionService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话会话管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/session")
@RequiredArgsConstructor
@Validated
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 创建对话会话
     */
    @PostMapping("/create")
    public Result<ChatSessionDTO> createSession(@RequestBody @Validated CreateSessionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("创建对话会话请求 - 用户ID: {}", userId);

        ChatSessionDTO session = chatSessionService.createSession(userId, request);
        return Result.success(session);
    }

    /**
     * 获取用户对话列表
     */
    @GetMapping("/list")
    public Result<List<ChatSessionDTO>> getUserSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("获取用户对话列表 - 用户ID: {}", userId);

        List<ChatSessionDTO> sessions = chatSessionService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/detail/{sessionId}")
    public Result<ChatSessionDTO> getSessionDetail(@PathVariable String sessionId) {
        log.info("获取会话详情 - SessionID: {}", sessionId);

        ChatSessionDTO session = chatSessionService.getSessionDetail(sessionId);
        if (session == null) {
            return Result.error("会话不存在");
        }
        return Result.success(session);
    }

    /**
     * 删除单个对话会话
     */
    @PostMapping("/delete/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("删除对话会话 - 用户ID: {}, SessionID: {}", userId, sessionId);

        chatSessionService.deleteSession(userId, sessionId);
        return Result.success(null);
    }

    /**
     * 批量删除对话会话
     */
    @PostMapping("/delete/batch")
    public Result<Map<String, Object>> batchDeleteSessions(@RequestBody @Validated DeleteSessionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("批量删除对话会话 - 用户ID: {}, 数量: {}", userId, request.getSessionIds().size());

        chatSessionService.batchDeleteSessions(userId, request.getSessionIds());

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", request.getSessionIds().size());
        return Result.success(result);
    }

    /**
     * 更新对话会话信息
     */
    @PostMapping("/update/{sessionId}")
    public Result<ChatSessionDTO> updateSession(
            @PathVariable String sessionId,
            @RequestBody @Validated UpdateSessionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("更新对话会话 - 用户ID: {}, SessionID: {}", userId, sessionId);

        ChatSessionDTO session = chatSessionService.updateSession(userId, sessionId, request);
        return Result.success(session);
    }
}
