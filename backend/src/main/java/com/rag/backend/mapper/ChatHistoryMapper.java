package com.rag.backend.mapper;

import com.rag.backend.entity.ChatHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Acer
* @description 针对表【chat_history(会话历史记录表)】的数据库操作Mapper
* @Entity com.rag.backend.enetity.ChatHistory
*/
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 根据会话ID查询历史记录
     */
    @Select("SELECT * FROM chat_history WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY create_time ASC")
    List<ChatHistory> findBySessionId(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}




