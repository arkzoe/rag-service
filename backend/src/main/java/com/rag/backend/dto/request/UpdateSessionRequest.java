package com.rag.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新会话请求
 */
@Data
public class UpdateSessionRequest {

    /**
     * 会话标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    /**
     * 会话描述
     */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
}
