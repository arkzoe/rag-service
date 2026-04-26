package com.rag.backend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建对话会话请求
 */
@Data
public class CreateSessionRequest {

    /**
     * 对话标题
     */
    @NotBlank(message = "对话标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    /**
     * 对话描述
     */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}
