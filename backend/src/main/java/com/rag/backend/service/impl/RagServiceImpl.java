package com.rag.backend.service.impl;

import com.rag.backend.common.config.ChatConfiguration;
import com.rag.backend.entity.ChatHistory;
import com.rag.backend.mapper.ChatHistoryMapper;
import com.rag.backend.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public String chat(String question, List<String> userRoles, Long userId, String sessionId) {
        log.info("RAG问答 - 用户ID: {}, 会话ID: {}, 问题: {}", userId, sessionId, question);

        // 1. 执行向量检索（不带权限过滤）
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(20)
                .build();

        List<Document> allDocuments = vectorStore.similaritySearch(searchRequest);
        log.info("检索到 {} 条原始文档", allDocuments.size());

        // 2. 手动进行权限过滤
        List<Document> documents = filterByRoles(allDocuments, userRoles);
        log.info("权限过滤后剩余 {} 条文档", documents.size());

        // 3. 构建上下文
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        if (context.isEmpty()) {
            context = "未在知识库中找到相关信息。";
        }

        // 4. 构建系统提示词
        String systemPrompt = buildSystemPrompt(context);

        // 5. 调用LLM生成回答
        Prompt prompt = new Prompt(systemPrompt + "\n用户问题: " + question);
        String answer = chatClient.prompt(prompt).call().content();

        log.info("RAG问答完成 - 用户ID: {}", userId);

        // 6. 异步保存聊天记录，使用传入的sessionId
        saveHistoryAsync(userId, sessionId, question, answer, documents.size());

        return answer;
    }

    /**
     * 根据用户角色过滤文档
     */
    private List<Document> filterByRoles(List<Document> documents, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return documents;
        }

        return documents.stream()
                .filter(doc -> {
                    Object allowedRolesObj = doc.getMetadata().get("allowed_roles");
                    if (allowedRolesObj == null) {
                        return true; // 如果没有设置权限，默认允许访问
                    }

                    String allowedRoles = allowedRolesObj.toString();
                    log.debug("文档权限: {}, 用户角色: {}", allowedRoles, userRoles);

                    // 检查用户是否有任一角色匹配
                    return userRoles.stream()
                            .anyMatch(role -> allowedRoles.contains(role));
                })
                .limit(5) // 限制返回5条
                .collect(Collectors.toList());
    }

    /**
     * 构建系统提示词 - 合并默认提示词和上下文
     */
    private String buildSystemPrompt(String context) {
        return ChatConfiguration.DEFAULT_SYSTEM_PROMPT + """

            参考资料:
            %s
            """.formatted(context);
    }

    /**
     * 异步保存聊天记录
     */
    @Async("taskExecutor")
    public void saveHistoryAsync(Long userId, String sessionId, String query, String answer, int sourceCount) {
        try {
            ChatHistory history = new ChatHistory();
            // 使用传入的sessionId，如果没有则生成新的
            history.setSessionId(sessionId != null && !sessionId.isEmpty() ? sessionId : UUID.randomUUID().toString());
            history.setUserId(userId);
            history.setQuery(query);
            history.setAnswer(answer);
            history.setSourceCount(sourceCount);
            chatHistoryMapper.insert(history);
            log.debug("聊天记录已异步保存 - 用户ID: {}, 会话ID: {}", userId, history.getSessionId());
        } catch (Exception e) {
            log.error("保存聊天记录失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
    }

    @Override
    public List<ChatHistory> getSessionHistory(String sessionId, Long userId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return List.of();
        }
        return chatHistoryMapper.findBySessionId(sessionId, userId);
    }
}
