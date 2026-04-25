import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'chat_messages'
const MAX_MESSAGES = 100 // 最多保存100条消息

export const useChatStore = defineStore('chat', () => {
  // 从localStorage加载消息
  const loadMessagesFromStorage = () => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        return JSON.parse(stored)
      }
    } catch (error) {
      console.error('加载聊天记录失败:', error)
    }
    return []
  }

  const messages = ref(loadMessagesFromStorage())

  // 保存消息到localStorage
  const saveMessages = () => {
    try {
      // 只保留最近的消息
      const messagesToSave = messages.value.slice(-MAX_MESSAGES)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(messagesToSave))
    } catch (error) {
      console.error('保存聊天记录失败:', error)
    }
  }

  // 添加消息
  const addMessage = (message) => {
    messages.value.push(message)
    saveMessages()
  }

  // 清空消息
  const clearMessages = () => {
    messages.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  // 获取所有消息
  const getMessages = computed(() => messages.value)

  // 初始化（如果没有消息，添加欢迎消息）
  const initMessages = () => {
    if (messages.value.length === 0) {
      messages.value = [
        {
          id: Date.now(),
          role: 'assistant',
          content: '您好！我是RAG智能助手，有什么可以帮助您的吗？',
          timestamp: Date.now(),
        },
      ]
      saveMessages()
    }
  }

  return {
    messages,
    getMessages,
    addMessage,
    clearMessages,
    initMessages,
  }
})
