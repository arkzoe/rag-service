package com.rag.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过2000字符")
    private String question;

    // 会话ID，可选，为空时会创建新会话
    private String sessionId;
}
