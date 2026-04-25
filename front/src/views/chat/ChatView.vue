<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Delete, ChatDotRound, UserFilled, Loading } from '@element-plus/icons-vue'
import { sendMessage } from '@/api/chat'
import { useChatStore } from '@/stores/chat'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const chatStore = useChatStore()

// 初始化 Markdown 解析器
const md = new MarkdownIt({
  html: true,
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

const messageListRef = ref(null)
const inputRef = ref(null)

const inputMessage = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const hasMoreHistory = ref(false) // 暂时不支持加载更多

// 使用computed获取消息列表，确保响应式
const messages = computed(() => chatStore.messages)

const loadHistoryMessages = async () => {
  // 从store加载的消息已经包含在messages中
  // 这里可以添加从后端加载更多历史消息的逻辑
  if (loadingMore.value || !hasMoreHistory.value) return

  loadingMore.value = true
  try {
    // 如果有后端历史消息接口，在这里调用
    // 目前使用localStorage中的消息
    hasMoreHistory.value = false
  } catch (error) {
    ElMessage.error('加载历史消息失败')
  } finally {
    loadingMore.value = false
  }
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

  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: message,
    timestamp: Date.now(),
  }

  chatStore.addMessage(userMessage)
  inputMessage.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const response = await sendMessage(message)

    const assistantMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: response,
      timestamp: Date.now(),
    }

    chatStore.addMessage(assistantMessage)
    scrollToBottom()
  } catch (error) {
    const errorMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，我遇到了一些问题，请稍后重试。',
      timestamp: Date.now(),
      isError: true,
    }
    chatStore.addMessage(errorMessage)
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
  chatStore.clearMessages()
  chatStore.initMessages()
  ElMessage.success('对话已清空')
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatDate = (timestamp) => {
  const date = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
    })
  }
}

const shouldShowDate = (index) => {
  if (index === 0) return true
  const current = new Date(messages.value[index].timestamp).toDateString()
  const prev = new Date(messages.value[index - 1].timestamp).toDateString()
  return current !== prev
}

const handleScroll = () => {
  if (!messageListRef.value) return
  const { scrollTop } = messageListRef.value
  if (scrollTop < 50 && hasMoreHistory.value && !loadingMore.value) {
    loadHistoryMessages()
  }
}

// 渲染 Markdown 内容 - 智能处理换行
const renderMarkdown = (content) => {
  // 保留Markdown结构需要的换行，只压缩多余空行
  // 将3个及以上连续换行压缩为2个（保留段落分隔）
  const compressedContent = content.replace(/\n{3,}/g, '\n\n')
  return md.render(compressedContent)
}

onMounted(() => {
  // 初始化聊天记录（如果没有则添加欢迎消息）
  chatStore.initMessages()
  // 滚动到底部
  scrollToBottom()
})
</script>

<template>
  <div class="chat-container">
    <el-card class="chat-card" :body-style="{ padding: 0, height: '100%' }">
      <div class="chat-layout">
        <div class="chat-header">
          <div class="header-left">
            <el-icon class="chat-icon"><ChatDotRound /></el-icon>
            <span class="chat-title">智能对话</span>
          </div>
          <div class="header-right">
            <el-button type="danger" link @click="clearMessages">
              <el-icon><Delete /></el-icon>
              清空对话
            </el-button>
          </div>
        </div>

        <div ref="messageListRef" class="message-list" @scroll="handleScroll">
          <div v-if="loadingMore" class="loading-more">
            <el-icon class="is-loading"><Loading /></el-icon>
            加载历史消息...
          </div>

          <div v-if="messages.length === 0" class="empty-state">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <p>开始您的对话吧</p>
          </div>

          <template v-else>
            <div
              v-for="(message, index) in messages"
              :key="message.id"
              class="message-wrapper"
            >
              <div v-if="shouldShowDate(index)" class="date-divider">
                <span>{{ formatDate(message.timestamp) }}</span>
              </div>

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
                    <!-- 用户消息直接显示，AI消息使用Markdown渲染 -->
                    <div 
                      v-if="message.role === 'user'" 
                      class="message-text"
                    >{{ message.content }}</div>
                    <div 
                      v-else 
                      class="message-text markdown-body"
                      v-html="renderMarkdown(message.content)"
                    ></div>
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
</template>

<style scoped lang="scss">
.chat-container {
  height: calc(100vh - 100px);
  max-width: 1200px;
  margin: 0 auto;
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

  .loading-more {
    text-align: center;
    padding: 12px;
    color: #909399;
    font-size: 12px;
  }

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

.date-divider {
  text-align: center;
  margin: 20px 0;

  span {
    display: inline-block;
    padding: 4px 12px;
    background: #dcdfe6;
    color: #606266;
    font-size: 12px;
    border-radius: 12px;
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
  white-space: normal;
}

// Markdown 样式 - 紧凑间距
.markdown-body {
  line-height: 1.5;
  font-size: 14px;

  :deep(h1) {
    font-size: 1.3em;
    margin: 0.15em 0 0.1em 0;
    border-bottom: 1px solid #eaecef;
    padding-bottom: 0.1em;
  }

  :deep(h2) {
    font-size: 1.15em;
    margin: 0.15em 0 0.1em 0;
    border-bottom: 1px solid #eaecef;
    padding-bottom: 0.1em;
  }

  :deep(h3) {
    font-size: 1.05em;
    margin: 0.1em 0 0.05em 0;
  }

  :deep(p) {
    margin: 0.1em 0;
    line-height: 1.5;
  }

  :deep(ul), :deep(ol) {
    margin: 0.1em 0;
    padding-left: 1em;
  }

  :deep(li) {
    margin: 0.05em 0;
    line-height: 1.4;
  }

  :deep(li > p) {
    margin: 0;
  }

  :deep(code) {
    background: #f6f8fa;
    padding: 0 3px;
    border-radius: 3px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 0.85em;
  }

  :deep(pre) {
    background: #f6f8fa;
    padding: 6px 10px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 0.15em 0;

    code {
      background: none;
      padding: 0;
    }
  }

  :deep(blockquote) {
    border-left: 4px solid #dfe2e5;
    padding-left: 0.6em;
    margin: 0.15em 0;
    color: #6a737d;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 0.15em 0;

    th, td {
      border: 1px solid #dfe2e5;
      padding: 3px 6px;
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

  :deep(strong) {
    font-weight: 600;
  }

  :deep(em) {
    font-style: italic;
  }

  // 移除空行和多余间距
  :deep(br) {
    display: none;
  }

  // 段落之间的间距
  :deep(p + p) {
    margin-top: 0.15em;
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
  0%,
  80%,
  100% {
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
  .chat-container {
    height: calc(100vh - 60px);
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
