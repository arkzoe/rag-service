package com.rag.backend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 删除对话会话请求
 */
@Data
public class DeleteSessionRequest {

    /**
     * 要删除的会话ID列表
     */
    @NotEmpty(message = "会话ID列表不能为空")
    private List<String> sessionIds;
}
