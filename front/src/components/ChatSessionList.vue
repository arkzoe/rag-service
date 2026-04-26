<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useChatSessionStore } from '@/stores/chatSession'
import { useChatStore } from '@/stores/chat'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ChatDotRound, Delete, Plus, MoreFilled, Edit } from '@element-plus/icons-vue'

const chatStore = useChatStore()

// 自定义指令：自动聚焦
const vFocus = {
  mounted: (el) => {
    nextTick(() => {
      const input = el.querySelector('input') || el
      input.focus()
      input.select()
    })
  }
}

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'select', 'create'])

const chatSessionStore = useChatSessionStore()

// 选中的会话ID列表（用于批量删除）
const selectedSessions = ref([])
// 是否显示批量删除模式
const batchMode = ref(false)
// 搜索关键词
const searchKeyword = ref('')
// 正在编辑的会话ID
const editingSessionId = ref('')
// 编辑中的标题
const editingTitle = ref('')

// 过滤后的会话列表
const filteredSessions = computed(() => {
  if (!searchKeyword.value) return chatSessionStore.sessions
  const keyword = searchKeyword.value.toLowerCase()
  return chatSessionStore.sessions.filter(session =>
    session.title?.toLowerCase().includes(keyword) ||
    session.description?.toLowerCase().includes(keyword)
  )
})

// 选择会话
const handleSelect = (session) => {
  if (batchMode.value) return
  chatSessionStore.setCurrentSession(session.sessionId)
  emit('update:modelValue', session.sessionId)
  emit('select', session)
}

// 创建新对话
const handleCreate = async () => {
  const session = await chatSessionStore.createSession('新对话')
  if (session) {
    emit('create', session)
    emit('select', session)
  }
}

// 删除单个会话
const handleDelete = async (session, event) => {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(
      `确定要删除对话 "${session.title}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await chatSessionStore.deleteSession(session.sessionId)
    // 同步删除该会话的消息记录
    chatStore.deleteSessionMessages(session.sessionId)
  } catch (error) {
    // 用户取消
  }
}

// 切换批量模式
const toggleBatchMode = () => {
  batchMode.value = !batchMode.value
  selectedSessions.value = []
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedSessions.value.length === 0) {
    ElMessage.warning('请选择要删除的对话')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedSessions.value.length} 个对话吗？`,
      '确认批量删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await chatSessionStore.batchDeleteSessions(selectedSessions.value)
    // 同步删除这些会话的消息记录
    selectedSessions.value.forEach(sessionId => {
      chatStore.deleteSessionMessages(sessionId)
    })
    batchMode.value = false
    selectedSessions.value = []
  } catch (error) {
    // 用户取消
  }
}

// 开始编辑会话标题
const startEditing = (session, event) => {
  event.stopPropagation()
  editingSessionId.value = session.sessionId
  editingTitle.value = session.title || '新对话'
}

// 保存编辑的标题
const saveEditing = async () => {
  if (!editingSessionId.value) return
  const newTitle = editingTitle.value.trim()
  if (!newTitle) {
    ElMessage.warning('标题不能为空')
    return
  }
  await chatSessionStore.updateSessionInfo(editingSessionId.value, { title: newTitle })
  editingSessionId.value = ''
  editingTitle.value = ''
}

// 取消编辑
const cancelEditing = () => {
  editingSessionId.value = ''
  editingTitle.value = ''
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  // 小于1小时显示"xx分钟前"
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000))
    return minutes < 1 ? '刚刚' : `${minutes}分钟前`
  }
  // 小于24小时显示"xx小时前"
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000))
    return `${hours}小时前`
  }
  // 小于7天显示"xx天前"
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    const days = Math.floor(diff / (24 * 60 * 60 * 1000))
    return `${days}天前`
  }
  // 否则显示日期
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(async () => {
  await chatSessionStore.fetchSessions()
  // 如果没有对话，自动创建一个新对话
  if (chatSessionStore.sessions.length === 0) {
    await handleCreate()
  }
})
</script>

<template>
  <div class="session-list-container">
    <!-- 头部操作栏 -->
    <div class="session-header">
      <el-button type="primary" @click="handleCreate" :icon="Plus">
        新建对话
      </el-button>
      <el-button
        :type="batchMode ? 'danger' : 'default'"
        @click="toggleBatchMode"
        :icon="Delete"
      >
        {{ batchMode ? '取消' : '批量删除' }}
      </el-button>
    </div>

    <!-- 搜索框 -->
    <div class="session-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索对话..."
        clearable
        prefix-icon="Search"
      />
    </div>

    <!-- 批量操作栏 -->
    <div v-if="batchMode" class="batch-actions">
      <el-checkbox
        v-model="selectedSessions"
        :label="'全选'"
        :indeterminate="selectedSessions.length > 0 && selectedSessions.length < filteredSessions.length"
        @change="(val) => {
          if (val) {
            selectedSessions = filteredSessions.map(s => s.sessionId)
          } else {
            selectedSessions = []
          }
        }"
      >
        全选 ({{ selectedSessions.length }}/{{ filteredSessions.length }})
      </el-checkbox>
      <el-button
        type="danger"
        size="small"
        :disabled="selectedSessions.length === 0"
        @click="handleBatchDelete"
      >
        删除选中
      </el-button>
    </div>

    <!-- 会话列表 -->
    <div class="session-list" v-loading="chatSessionStore.loading">
      <div
        v-for="session in filteredSessions"
        :key="session.sessionId"
        class="session-item"
        :class="{
          'session-active': session.sessionId === chatSessionStore.currentSessionId,
          'session-selected': selectedSessions.includes(session.sessionId)
        }"
        @click="handleSelect(session)"
      >
        <!-- 批量选择复选框 -->
        <el-checkbox
          v-if="batchMode"
          v-model="selectedSessions"
          :label="session.sessionId"
          class="session-checkbox"
          @click.stop
        >
          {{ '' }}
        </el-checkbox>

        <!-- 会话图标 -->
        <div class="session-icon">
          <el-icon><ChatDotRound /></el-icon>
        </div>

        <!-- 会话信息 -->
        <div class="session-info">
          <!-- 标题显示/编辑 -->
          <div v-if="editingSessionId !== session.sessionId" class="session-title">
            {{ session.title || '未命名对话' }}
          </div>
          <div v-else class="session-title-edit" @click.stop>
            <el-input
              v-model="editingTitle"
              size="small"
              placeholder="请输入对话名称"
              @keyup.enter="saveEditing"
              @keyup.esc="cancelEditing"
              v-focus
            />
            <el-button type="primary" link size="small" @click="saveEditing">保存</el-button>
            <el-button link size="small" @click="cancelEditing">取消</el-button>
          </div>
          <div class="session-preview" v-if="session.lastMessagePreview">
            {{ session.lastMessagePreview }}
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
            <span class="session-count" v-if="session.messageCount">
              {{ session.messageCount }}条消息
            </span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div v-if="!batchMode" class="session-actions">
          <el-button
            class="session-edit"
            type="primary"
            link
            size="small"
            :icon="Edit"
            @click="startEditing(session, $event)"
          />
          <el-button
            class="session-delete"
            type="danger"
            link
            size="small"
            :icon="Delete"
            @click="handleDelete(session, $event)"
          />
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredSessions.length === 0" class="session-empty">
        <el-icon :size="48"><ChatDotRound /></el-icon>
        <p>{{ searchKeyword ? '没有找到匹配的对话' : '还没有对话，点击新建开始吧' }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.session-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.session-header {
  display: flex;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid #ebeef5;

  .el-button {
    flex: 1;
  }
}

.session-search {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 4px;

  &:hover {
    background: #f5f7fa;

    .session-delete {
      opacity: 1;
    }
  }

  &.session-active {
    background: #ecf5ff;
    border-left: 3px solid #409eff;
  }

  &.session-selected {
    background: #fef0f0;
  }
}

.session-checkbox {
  margin-top: 4px;
}

.session-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-title-edit {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;

  .el-input {
    flex: 1;
  }
}

.session-preview {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #c0c4cc;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;

  .session-edit,
  .session-delete {
    padding: 4px;
  }
}

.session-item:hover .session-actions {
  opacity: 1;
}

.session-delete {
  opacity: 0;
  transition: opacity 0.3s;
  padding: 4px;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #909399;

  p {
    margin-top: 12px;
    font-size: 14px;
  }
}
</style>
