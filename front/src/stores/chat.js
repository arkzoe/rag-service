import { ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY_PREFIX = 'chat_messages_'
const MAX_MESSAGES = 100 // 每个会话最多保存100条消息

export const useChatStore = defineStore('chat', () => {
  // 消息存储（按会话ID分组）
  const messagesMap = ref(new Map())

  // 获取指定会话的消息
  const getSessionMessages = (sessionId) => {
    if (!sessionId) return []
    // 如果内存中没有，尝试从 localStorage 加载
    if (!messagesMap.value.has(sessionId)) {
      const stored = loadMessagesFromStorage(sessionId)
      messagesMap.value.set(sessionId, stored)
    }
    return messagesMap.value.get(sessionId) || []
  }

  // 从localStorage加载指定会话的消息
  const loadMessagesFromStorage = (sessionId) => {
    if (!sessionId) return []
    try {
      const stored = localStorage.getItem(STORAGE_KEY_PREFIX + sessionId)
      if (stored) {
        return JSON.parse(stored)
      }
    } catch (error) {
      console.error('加载聊天记录失败:', error)
    }
    return []
  }

  // 保存指定会话的消息到localStorage
  const saveMessages = (sessionId) => {
    if (!sessionId) return
    try {
      const sessionMessages = messagesMap.value.get(sessionId) || []
      // 只保留最近的消息
      const messagesToSave = sessionMessages.slice(-MAX_MESSAGES)
      localStorage.setItem(STORAGE_KEY_PREFIX + sessionId, JSON.stringify(messagesToSave))
    } catch (error) {
      console.error('保存聊天记录失败:', error)
    }
  }

  // 添加消息到指定会话
  const addMessage = (sessionId, message) => {
    if (!sessionId) return
    const sessionMessages = messagesMap.value.get(sessionId) || []
    sessionMessages.push(message)
    messagesMap.value.set(sessionId, sessionMessages)
    saveMessages(sessionId)
  }

  // 清空指定会话的消息
  const clearSessionMessages = (sessionId) => {
    if (!sessionId) return
    messagesMap.value.set(sessionId, [])
    localStorage.removeItem(STORAGE_KEY_PREFIX + sessionId)
  }

  // 初始化会话消息（如果没有消息，添加欢迎消息）
  const initSessionMessages = (sessionId) => {
    if (!sessionId) return
    const sessionMessages = messagesMap.value.get(sessionId)
    // 如果还没有加载过，从 localStorage 加载
    if (sessionMessages === undefined) {
      const stored = loadMessagesFromStorage(sessionId)
      if (stored.length === 0) {
        // 没有历史消息，添加欢迎消息
        messagesMap.value.set(sessionId, [
          {
            id: Date.now(),
            role: 'assistant',
            content: '您好！我是RAG智能助手，有什么可以帮助您的吗？',
            timestamp: Date.now(),
          },
        ])
        saveMessages(sessionId)
      } else {
        messagesMap.value.set(sessionId, stored)
      }
    }
  }

  // 删除会话的消息
  const deleteSessionMessages = (sessionId) => {
    messagesMap.value.delete(sessionId)
    localStorage.removeItem(STORAGE_KEY_PREFIX + sessionId)
  }

  return {
    getSessionMessages,
    addMessage,
    clearSessionMessages,
    initSessionMessages,
    deleteSessionMessages,
  }
})
