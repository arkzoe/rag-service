package com.rag.backend.common.constant;

/**
 * Redis缓存Key常量
 */
public class RedisKeyConstants {

    /**
     * 对话会话缓存前缀
     */
    public static final String CHAT_SESSION_PREFIX = "chat:session:";

    /**
     * 用户对话列表缓存前缀
     */
    public static final String CHAT_SESSION_LIST_PREFIX = "chat:session:list:";

    /**
     * 对话会话缓存过期时间（小时）
     */
    public static final long CHAT_SESSION_EXPIRE_HOURS = 24;

    /**
     * 用户对话列表缓存过期时间（小时）
     */
    public static final long CHAT_SESSION_LIST_EXPIRE_HOURS = 12;

    /**
     * 获取对话会话缓存Key
     */
    public static String getSessionKey(String sessionId) {
        return CHAT_SESSION_PREFIX + sessionId;
    }

    /**
     * 获取用户对话列表缓存Key
     */
    public static String getSessionListKey(Long userId) {
        return CHAT_SESSION_LIST_PREFIX + userId;
    }
}
