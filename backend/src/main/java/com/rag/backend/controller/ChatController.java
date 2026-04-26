package com.rag.backend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.rag.backend.common.result.Result;
import com.rag.backend.dto.ChatRequest;
import com.rag.backend.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    @PostMapping("/send")
    @SaCheckPermission("chat:ask")
    public Result<?> send(@Valid @RequestBody ChatRequest chatRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<String> userRoles = StpUtil.getRoleList();

        log.info("聊天请求 - 用户ID: {}, 角色: {}, 问题: {}", userId, userRoles, chatRequest.getQuestion());

        String answer = ragService.chat(chatRequest.getQuestion(), userRoles, userId);
        return Result.success(answer);
    }
}