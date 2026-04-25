package com.rag.backend.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfiguration {

    /**
     * 默认系统提示词 - 作为RAG助手的基础人设
     */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个专业的智能助手，基于知识库参考资料回答用户问题。

            回答要求:
            1. 基于参考资料内容回答，保持准确性和完整性
            2. 如果参考资料中没有答案，请诚实告知"根据现有资料无法回答该问题"
            3. 回答要简洁明了，突出重点
            4. 可以适当引用参考资料中的内容
            5. 保持专业、友好的态度
            """;

    @Bean
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient
                .builder(model)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
