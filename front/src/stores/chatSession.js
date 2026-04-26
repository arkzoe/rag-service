import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import {
  createSession as createSessionApi,
  getSessionList as getSessionListApi,
  deleteSession as deleteSessionApi,
  batchDeleteSessions as batchDeleteSessionsApi,
  updateSession as updateSessionApi,
} from '@/api/chatSession'
import { ElMessage } from 'element-plus'

const STORAGE_KEY = 'current_session_id'

export const useChatSessionStore = defineStore('chatSession', () => {
  // 对话列表
  const sessions = ref([])
  // 当前选中的会话ID
  const currentSessionId = ref(localStorage.getItem(STORAGE_KEY) || '')
  // 加载状态
  const loading = ref(false)

  // 当前会话
  const currentSession = computed(() => {
    return sessions.value.find(s => s.sessionId === currentSessionId.value) || null
  })

  // 设置当前会话
  const setCurrentSession = (sessionId) => {
    currentSessionId.value = sessionId
    localStorage.setItem(STORAGE_KEY, sessionId)
  }

  // 获取对话列表
  const fetchSessions = async () => {
    loading.value = true
    try {
      const data = await getSessionListApi()
      sessions.value = data || []
      return sessions.value
    } catch (error) {
      ElMessage.error('获取对话列表失败')
      return []
    } finally {
      loading.value = false
    }
  }

  // 创建新对话
  const createSession = async (title, description = '') => {
    try {
      const newSession = await createSessionApi({
        title: title || '新对话',
        description,
      })
      sessions.value.unshift(newSession)
      setCurrentSession(newSession.sessionId)
      ElMessage.success('对话创建成功')
      return newSession
    } catch (error) {
      ElMessage.error('创建对话失败')
      return null
    }
  }

  // 删除单个对话
  const deleteSession = async (sessionId) => {
    try {
      await deleteSessionApi(sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
      // 如果删除的是当前会话，切换到第一个或清空
      if (currentSessionId.value === sessionId) {
        const firstSession = sessions.value[0]
        setCurrentSession(firstSession?.sessionId || '')
      }
      ElMessage.success('删除成功')
      return true
    } catch (error) {
      ElMessage.error('删除失败')
      return false
    }
  }

  // 批量删除对话
  const batchDeleteSessions = async (sessionIds) => {
    try {
      await batchDeleteSessionsApi({ sessionIds })
      sessions.value = sessions.value.filter(s => !sessionIds.includes(s.sessionId))
      // 检查当前会话是否被删除
      if (sessionIds.includes(currentSessionId.value)) {
        const firstSession = sessions.value[0]
        setCurrentSession(firstSession?.sessionId || '')
      }
      ElMessage.success(`成功删除 ${sessionIds.length} 个对话`)
      return true
    } catch (error) {
      ElMessage.error('批量删除失败')
      return false
    }
  }

  // 清空当前会话
  const clearCurrentSession = () => {
    currentSessionId.value = ''
    localStorage.removeItem(STORAGE_KEY)
  }

  // 更新会话信息
  const updateSessionInfo = async (sessionId, data) => {
    try {
      await updateSessionApi(sessionId, data)
      // 更新本地数据
      const index = sessions.value.findIndex(s => s.sessionId === sessionId)
      if (index !== -1) {
        sessions.value[index] = { ...sessions.value[index], ...data }
      }
      ElMessage.success('修改成功')
      return true
    } catch (error) {
      ElMessage.error('修改失败')
      return false
    }
  }

  return {
    sessions,
    currentSessionId,
    currentSession,
    loading,
    setCurrentSession,
    fetchSessions,
    createSession,
    deleteSession,
    batchDeleteSessions,
    clearCurrentSession,
    updateSessionInfo,
  }
})
