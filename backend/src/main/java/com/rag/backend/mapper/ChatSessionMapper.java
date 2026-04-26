package com.rag.backend.mapper;

import com.rag.backend.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 对话会话Mapper接口
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 查询用户的对话列表（按时间倒序）
     */
    List<ChatSession> selectByUserId(@Param("userId") Long userId);

    /**
     * 批量删除对话（软删除）
     */
    @Update("UPDATE chat_session SET is_deleted = 1 WHERE id IN (${ids}) AND user_id = #{userId}")
    int batchDelete(@Param("ids") String ids, @Param("userId") Long userId);

    /**
     * 根据sessionId查询
     */
    ChatSession selectBySessionId(@Param("sessionId") String sessionId);
}
