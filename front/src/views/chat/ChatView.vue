<script setup>
import { ref, nextTick, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Delete, ChatDotRound, UserFilled, Loading, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { sendMessage, getChatHistory } from '@/api/chat'
import { useChatStore } from '@/stores/chat'
import { useChatSessionStore } from '@/stores/chatSession'
import ChatSessionList from '@/components/ChatSessionList.vue'

const chatStore = useChatStore()
const chatSessionStore = useChatSessionStore()

const messageListRef = ref(null)
const inputRef = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const showSessionList = ref(true)

// 当前会话ID - 使用 chatSessionStore 的
const currentSessionId = computed({
  get: () => chatSessionStore.currentSessionId,
  set: (val) => chatSessionStore.setCurrentSession(val)
})

// 消息列表 - 从 chatStore 获取当前会话的消息
const messages = computed(() => {
  const sessionId = chatSessionStore.currentSessionId
  if (!sessionId) return []
  return chatStore.getSessionMessages(sessionId)
})

// 初始化 Markdown 解析器
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 渲染 Markdown
const renderMarkdown = (content) => {
  // 确保内容是字符串类型
  if (!content) return ''
  
  // 处理响应式对象（Proxy）
  let rawContent = content
  if (typeof content === 'object') {
    // 如果是 Proxy 对象或普通对象，尝试提取 data 字段或转为字符串
    rawContent = content.data || content.content || JSON.stringify(content)
  }
  
  if (typeof rawContent !== 'string') {
    console.warn('renderMarkdown received non-string content:', content)
    rawContent = String(rawContent || '')
  }
  
  return md.render(rawContent)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const handleSend = async () => {
  const message = inputMessage.value.trim()
  if (!message || loading.value) return

  console.log('发送消息 - 当前会话ID:', chatSessionStore.currentSessionId)

  // 如果没有当前会话，先创建一个
  if (!chatSessionStore.currentSessionId) {
    const newSession = await chatSessionStore.createSession(message.slice(0, 20))
    if (!newSession) return
    // 初始化新会话的消息
    chatStore.initSessionMessages(newSession.sessionId)
  }

  // 保存当前会话ID，防止在请求过程中切换会话导致消息添加到错误的会话
  const targetSessionId = chatSessionStore.currentSessionId
  console.log('发送消息 - 目标会话ID:', targetSessionId)

  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: message,
    timestamp: Date.now(),
  }

  chatStore.addMessage(targetSessionId, userMessage)
  inputMessage.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const response = await sendMessage(message, targetSessionId)

    // 处理响应数据，确保是字符串
    let responseContent = response
    if (typeof response === 'object' && response !== null) {
      // 如果响应是对象，尝试提取 data 或 content 字段
      responseContent = response.data || response.content || JSON.stringify(response)
    }
    if (typeof responseContent !== 'string') {
      responseContent = String(responseContent || '')
    }

    const assistantMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: responseContent,
      timestamp: Date.now(),
    }

    chatStore.addMessage(targetSessionId, assistantMessage)
    scrollToBottom()
  } catch (error) {
    const errorMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，我遇到了一些问题，请稍后重试。',
      timestamp: Date.now(),
      isError: true,
    }
    chatStore.addMessage(targetSessionId, errorMessage)
    scrollToBottom()
  } finally {
    loading.value = false
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

const clearMessages = () => {
  const sessionId = chatSessionStore.currentSessionId
  if (sessionId) {
    chatStore.clearSessionMessages(sessionId)
    chatStore.initSessionMessages(sessionId)
  }
  ElMessage.success('对话已清空')
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const handleSessionSelect = async (session) => {
  // 切换到选中会话
  chatSessionStore.setCurrentSession(session.sessionId)
  // 加载历史消息
  await loadSessionHistory(session.sessionId)
  scrollToBottom()
}

// 加载会话历史消息
const loadSessionHistory = async (sessionId) => {
  console.log('加载历史消息 - 会话ID:', sessionId)
  if (!sessionId) return
  
  try {
    const history = await getChatHistory(sessionId)
    console.log('加载历史消息结果 - 会话ID:', sessionId, '记录数:', history?.length || 0)
    console.log('加载历史消息:', sessionId, history)
    if (history && history.length > 0) {
      // 将历史记录转换为消息格式
      const messages = []
      history.forEach(record => {
        // 处理 createTime 可能是数组格式 [year, month, day, hour, minute, second]
        let timestamp = Date.now()
        if (record.createTime) {
          if (Array.isArray(record.createTime)) {
            const [year, month, day, hour, minute, second] = record.createTime
            timestamp = new Date(year, month - 1, day, hour, minute, second).getTime()
          } else {
            timestamp = new Date(record.createTime).getTime()
          }
        }
        // 用户消息
        messages.push({
          id: record.id * 2,
          role: 'user',
          content: record.query,
          timestamp: timestamp,
        })
        // AI回复
        messages.push({
          id: record.id * 2 + 1,
          role: 'assistant',
          content: record.answer,
          timestamp: timestamp + 1,
        })
      })
      chatStore.setSessionMessages(sessionId, messages)
    } else {
      // 没有历史记录，初始化空消息列表
      chatStore.initSessionMessages(sessionId)
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    chatStore.initSessionMessages(sessionId)
  }
}

const handleSessionCreate = (session) => {
  // 创建新会话后，初始化消息
  chatStore.initSessionMessages(session.sessionId)
}

onMounted(async () => {
  await chatSessionStore.fetchSessions()
  // 如果有当前会话，加载历史消息
  if (chatSessionStore.currentSessionId) {
    await loadSessionHistory(chatSessionStore.currentSessionId)
  }
  scrollToBottom()
})
</script>

<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="session-sidebar" :class="{ 'sidebar-collapsed': !showSessionList }">
      <div class="session-list-wrapper">
        <ChatSessionList
          v-model="currentSessionId"
          @select="handleSessionSelect"
          @create="handleSessionCreate"
        />
      </div>
      <!-- 折叠按钮 -->
      <div class="sidebar-toggle" @click.stop="showSessionList = !showSessionList">
        <el-icon>
          <ArrowLeft v-if="showSessionList" />
          <ArrowRight v-else />
        </el-icon>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-container">
      <el-card class="chat-card" :body-style="{ padding: 0, height: '100%' }">
        <div class="chat-layout">
          <div class="chat-header">
            <div class="header-left">
              <el-icon class="chat-icon"><ChatDotRound /></el-icon>
              <span class="chat-title">
                {{ chatSessionStore.currentSession?.title || '智能对话' }}
              </span>
            </div>
            <div class="header-right">
              <el-button type="danger" link @click="clearMessages">
                <el-icon><Delete /></el-icon>
                清空对话
              </el-button>
            </div>
          </div>

          <div ref="messageListRef" class="message-list">
            <div v-if="messages.length === 0" class="empty-state">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>开始您的对话吧</p>
            </div>

            <template v-else>
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-wrapper"
              >
                <div
                  class="message"
                  :class="[`message-${message.role}`, { 'message-error': message.isError }]"
                >
                  <div class="message-avatar">
                    <el-avatar
                      :size="40"
                      :icon="message.role === 'user' ? UserFilled : ChatDotRound"
                      :class="message.role"
                    />
                  </div>

                  <div class="message-content">
                    <div class="message-header">
                      <span class="message-role">
                        {{ message.role === 'user' ? '我' : 'AI助手' }}
                      </span>
                      <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                    </div>
                    <div class="message-bubble">
                      <!-- 用户消息纯文本显示 -->
                      <div v-if="message.role === 'user'" class="message-text">{{ message.content }}</div>
                      <!-- AI消息使用Markdown渲染 -->
                      <div
                        v-else
                        class="message-markdown"
                        v-html="renderMarkdown(message.content)"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="loading" class="message message-assistant">
                <div class="message-avatar">
                  <el-avatar :size="40" :icon="ChatDotRound" class="assistant" />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="message-role">AI助手</span>
                  </div>
                  <div class="message-bubble">
                    <div class="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <div class="chat-input-area">
            <div class="input-wrapper">
              <el-input
                ref="inputRef"
                v-model="inputMessage"
                type="textarea"
                :rows="3"
                placeholder="输入消息，按 Enter 发送，Shift + Enter 换行..."
                resize="none"
                :disabled="loading"
                @keydown="handleKeyDown"
              />
              <el-button
                type="primary"
                class="send-button"
                :loading="loading"
                :disabled="!inputMessage.trim()"
                @click="handleSend"
              >
                <el-icon><Promotion /></el-icon>
                发送
              </el-button>
            </div>
            <div class="input-tips">
              <span>按 Enter 发送，Shift + Enter 换行</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  height: calc(100vh - 60px);
}

.session-sidebar {
  width: 300px;
  flex-shrink: 0;
  position: relative;
  transition: width 0.3s;
  display: flex;

  &.sidebar-collapsed {
    width: 0;

    .session-list-wrapper {
      overflow: hidden;
    }
  }

  .session-list-wrapper {
    flex: 1;
    overflow: hidden;
  }

  .sidebar-toggle {
    position: absolute;
    right: -20px;
    top: 50%;
    transform: translateY(-50%);
    width: 20px;
    height: 60px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-left: none;
    border-radius: 0 4px 4px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    z-index: 100;
    box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);

    &:hover {
      background: #f5f7fa;
    }
  }
}

.chat-container {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.chat-card {
  height: 100%;

  :deep(.el-card__body) {
    height: 100%;
  }
}

.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .chat-icon {
      font-size: 24px;
      color: #409eff;
    }

    .chat-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #909399;

    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
    }

    p {
      font-size: 16px;
    }
  }
}

.message-wrapper {
  margin-bottom: 20px;
}

.message {
  display: flex;
  gap: 12px;

  &.message-user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-header {
      flex-direction: row-reverse;
    }

    .message-bubble {
      background: #409eff;
      color: #fff;
      border-radius: 12px 12px 2px 12px;
    }
  }

  &.message-assistant {
    .message-bubble {
      background: #fff;
      border: 1px solid #ebeef5;
      border-radius: 12px 12px 12px 2px;
    }
  }

  &.message-error {
    .message-bubble {
      background: #fef0f0;
      border-color: #fde2e2;
      color: #f56c6c;
    }
  }
}

.message-avatar {
  flex-shrink: 0;

  .el-avatar {
    background: #409eff;

    &.user {
      background: #67c23a;
    }
  }
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;

  .message-role {
    color: #606266;
    font-weight: 500;
  }

  .message-time {
    color: #909399;
  }
}

.message-bubble {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.message-text {
  white-space: pre-wrap;
}

// Markdown 样式
.message-markdown {
  :deep(p) {
    margin: 0 0 8px 0;
    line-height: 1.6;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(pre) {
    margin: 8px 0;
    padding: 12px;
    background: #f6f8fa;
    border-radius: 6px;
    overflow-x: auto;

    code {
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
      line-height: 1.5;
    }
  }

  :deep(code) {
    font-family: 'Consolas', 'Monaco', monospace;
    background: #f6f8fa;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 13px;
  }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 20px;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 8px 0;
    font-weight: 600;
  }

  :deep(h1) { font-size: 18px; }
  :deep(h2) { font-size: 16px; }
  :deep(h3) { font-size: 14px; }

  :deep(blockquote) {
    margin: 8px 0;
    padding: 8px 12px;
    border-left: 4px solid #dfe2e5;
    background: #f6f8fa;
    color: #6a737d;
  }

  :deep(table) {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;

    th, td {
      border: 1px solid #dfe2e5;
      padding: 6px 12px;
      text-align: left;
    }

    th {
      background: #f6f8fa;
      font-weight: 600;
    }
  }

  :deep(a) {
    color: #0366d6;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 8px;
    height: 8px;
    background: #909399;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input-area {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #ebeef5;

  .input-wrapper {
    display: flex;
    gap: 12px;

    .el-textarea {
      flex: 1;

      :deep(.el-textarea__inner) {
        border-radius: 8px;
        resize: none;
      }
    }

    .send-button {
      height: auto;
      padding: 0 24px;
      border-radius: 8px;
    }
  }

  .input-tips {
    margin-top: 8px;
    text-align: center;
    font-size: 12px;
    color: #909399;
  }
}

@media (max-width: 768px) {
  .session-sidebar {
    position: fixed;
    left: 0;
    top: 60px;
    bottom: 0;
    z-index: 100;
    background: #fff;

    &.sidebar-collapsed {
      width: 0;
    }
  }

  .chat-container {
    padding: 10px;
  }

  .message-content {
    max-width: 85% !important;
  }

  .input-wrapper {
    flex-direction: column;

    .send-button {
      height: 40px !important;
    }
  }
}
</style>
