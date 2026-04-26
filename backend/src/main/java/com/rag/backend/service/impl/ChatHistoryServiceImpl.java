package com.rag.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rag.backend.common.config.ChatConfiguration;
import com.rag.backend.entity.ChatHistory;
import com.rag.backend.mapper.ChatHistoryMapper;
import com.rag.backend.infrastructure.QdrantFilterBuilder;
import com.rag.backend.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* @author Acer
* @description 针对表【chat_history(会话历史记录表)】的数据库操作Service实现
*/
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
    implements ChatHistoryService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public String chat(String question) {
        // 1. 获取当前用户角色
        List<String> userRoles = StpUtil.getRoleList();

        // 2. 构建过滤器
        String filterExpression = QdrantFilterBuilder.buildRoleFilterString("allowed_roles", userRoles);

        // 3. 构建检索请求
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(question)
                .topK(3);
        
        if (filterExpression != null) {
            searchRequestBuilder.filterExpression(filterExpression);
        }
        
        SearchRequest searchRequest = searchRequestBuilder.build();

        // 4. 执行检索
        // 这里的 Document 明确指向 org.springframework.ai.document.Document
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // 5. 构建上下文
        // 使用 .map(Document::getContent) 将 Document 流转换为 String 流，再收集
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        if (context.isEmpty()) {
            context = "未在知识库中找到相关信息。";
        }

        // 6. 调用 LLM - 使用统一的系统提示词
        String systemPrompt = ChatConfiguration.DEFAULT_SYSTEM_PROMPT + """

                参考资料:
                %s
                """.formatted(context);

        Prompt prompt = new Prompt(systemPrompt + "\n用户问题: " + question);
        String answer = chatClient.prompt(prompt).call().content();

        // 7. 异步保存历史 (修正字段名)
        saveHistoryAsync(question, answer);

        return answer;
    }

    @Async
    public void saveHistoryAsync(String query, String response) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(UUID.randomUUID().toString());
        history.setUserId(StpUtil.getLoginIdAsLong());
        history.setQuery(query);
        history.setAnswer(response);
        chatHistoryMapper.insert(history);
    }
}