package com.rag.backend.service;

import com.rag.backend.entity.ChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Acer
* @description 针对表【chat_history(会话历史记录表)】的数据库操作Service
*/
public interface ChatHistoryService extends IService<ChatHistory> {

    String chat(String question);
}
