<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Document, Delete, Check, Close, RefreshRight, FolderOpened } from '@element-plus/icons-vue'
import { uploadFile, getDocumentList, deleteDocument } from '@/api/document'

const uploadRef = ref(null)
const fileList = ref([])
const uploading = ref(false)
const uploadedDocuments = ref([])
const loadingDocuments = ref(false)

const CHUNK_SIZE = 2 * 1024 * 1024

const uploadConfig = reactive({
  accept: '.pdf,.doc,.docx,.txt,.md',
  maxSize: 50 * 1024 * 1024,
  multiple: true,
  roles: 'admin,user',
})

const stats = computed(() => {
  const total = fileList.value.length
  const success = fileList.value.filter((f) => f.status === 'success').length
  const failed = fileList.value.filter((f) => f.status === 'error').length
  const uploading = fileList.value.filter((f) => f.status === 'uploading').length
  return { total, success, failed, uploading }
})

const beforeUpload = (file) => {
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown',
  ]
  const allowedExts = ['.pdf', '.doc', '.docx', '.txt', '.md']
  const ext = '.' + file.name.split('.').pop().toLowerCase()

  if (!allowedExts.includes(ext)) {
    ElMessage.error(`不支持的文件类型: ${ext}`)
    return false
  }

  if (file.size > uploadConfig.maxSize) {
    ElMessage.error(`文件大小超过限制: ${(uploadConfig.maxSize / 1024 / 1024).toFixed(0)}MB`)
    return false
  }

  return true
}

const handleFileChange = (uploadFile) => {
  const file = uploadFile.raw
  const fileItem = reactive({
    uid: uploadFile.uid,
    name: file.name,
    size: file.size,
    type: file.type,
    status: 'ready',
    progress: 0,
    speed: 0,
    uploadedSize: 0,
    errorMsg: '',
    raw: file,
  })

  const existingIndex = fileList.value.findIndex((f) => f.name === file.name && f.size === file.size)
  if (existingIndex !== -1) {
    fileList.value[existingIndex] = fileItem
  } else {
    fileList.value.push(fileItem)
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const uploadSingleFile = async (fileItem) => {
  if (fileItem.status === 'uploading') return

  fileItem.status = 'uploading'
  fileItem.progress = 0
  fileItem.errorMsg = ''

  const startTime = Date.now()
  let lastLoaded = 0

  try {
    const onProgress = (progressEvent) => {
      const loaded = progressEvent.loaded
      const total = progressEvent.total
      const currentTime = Date.now()
      const timeDiff = (currentTime - startTime) / 1000

      if (timeDiff > 0) {
        fileItem.speed = (loaded - lastLoaded) / timeDiff
        lastLoaded = loaded
      }

      fileItem.uploadedSize = loaded
      fileItem.progress = Math.round((loaded / total) * 100)
    }

    await uploadFile(fileItem.raw, uploadConfig.roles, onProgress)
    fileItem.status = 'success'
    fileItem.progress = 100
    ElMessage.success(`${fileItem.name} 上传成功`)
  } catch (error) {
    fileItem.status = 'error'
    fileItem.errorMsg = error.message || '上传失败'
    ElMessage.error(`${fileItem.name} 上传失败: ${fileItem.errorMsg}`)
  }
}

const uploadAll = async () => {
  const readyFiles = fileList.value.filter((f) => f.status === 'ready' || f.status === 'error')
  if (readyFiles.length === 0) {
    ElMessage.warning('没有待上传的文件')
    return
  }

  uploading.value = true
  try {
    await Promise.all(readyFiles.map((file) => uploadSingleFile(file)))
  } finally {
    uploading.value = false
  }
}

const removeFile = (fileItem) => {
  const index = fileList.value.findIndex((f) => f.uid === fileItem.uid)
  if (index !== -1) {
    fileList.value.splice(index, 1)
  }
}

const clearAll = () => {
  ElMessageBox.confirm('确定要清空所有文件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    fileList.value = []
    ElMessage.success('已清空')
  })
}

const retryUpload = (fileItem) => {
  uploadSingleFile(fileItem)
}

const getStatusType = (status) => {
  const types = {
    ready: 'info',
    uploading: 'primary',
    success: 'success',
    error: 'danger',
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    ready: '待上传',
    uploading: '上传中',
    success: '上传成功',
    error: '上传失败',
  }
  return texts[status] || '未知'
}

// 获取已上传文档列表
const fetchUploadedDocuments = async () => {
  loadingDocuments.value = true
  try {
    const data = await getDocumentList()
    uploadedDocuments.value = data || []
  } catch (error) {
    ElMessage.error('获取文档列表失败')
  } finally {
    loadingDocuments.value = false
  }
}

// 删除已上传文档
const handleDeleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${doc.fileName}" 吗？\n删除后将同时移除向量数据库中的数据。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await deleteDocument(doc.id)
    ElMessage.success('删除成功')
    fetchUploadedDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUploadedDocuments()
})
</script>

<template>
  <div class="upload-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span class="title">文件上传</span>
          <div class="stats">
            <el-tag type="info">总计: {{ stats.total }}</el-tag>
            <el-tag type="success">成功: {{ stats.success }}</el-tag>
            <el-tag type="danger">失败: {{ stats.failed }}</el-tag>
            <el-tag type="primary">上传中: {{ stats.uploading }}</el-tag>
          </div>
        </div>
      </template>

      <div class="upload-area">
        <el-upload
          ref="uploadRef"
          class="upload-drop-zone"
          drag
          :auto-upload="false"
          :accept="uploadConfig.accept"
          :multiple="uploadConfig.multiple"
          :before-upload="beforeUpload"
          :on-change="handleFileChange"
          :show-file-list="false"
        >
          <el-icon class="upload-icon"><Upload /></el-icon>
          <div class="upload-text">
            <p>拖拽文件到此处，或 <em>点击上传</em></p>
            <p class="upload-tip">
              支持 PDF、Word、TXT、Markdown 格式，单个文件不超过 50MB
            </p>
          </div>
        </el-upload>
      </div>

      <div class="upload-actions">
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="fileList.length === 0"
          @click="uploadAll"
        >
          <el-icon><Upload /></el-icon>
          开始上传
        </el-button>
        <el-button @click="clearAll" :disabled="fileList.length === 0">
          <el-icon><Delete /></el-icon>
          清空列表
        </el-button>
      </div>

      <div v-if="fileList.length > 0" class="file-list">
        <div class="file-list-header">
          <span>文件列表 ({{ fileList.length }})</span>
        </div>

        <div class="file-items">
          <div
            v-for="file in fileList"
            :key="file.uid"
            class="file-item"
            :class="`status-${file.status}`"
          >
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-details">
                <span class="file-name" :title="file.name">{{ file.name }}</span>
                <span class="file-size">{{ formatFileSize(file.size) }}</span>
              </div>
            </div>

            <div class="file-progress">
              <el-progress
                v-if="file.status === 'uploading'"
                :percentage="file.progress"
                :stroke-width="6"
                :show-text="false"
              />
              <span v-else-if="file.status === 'success'" class="success-text">
                <el-icon><Check /></el-icon> 上传成功
              </span>
              <span v-else-if="file.status === 'error'" class="error-text" :title="file.errorMsg">
                <el-icon><Close /></el-icon> {{ file.errorMsg || '上传失败' }}
              </span>
              <span v-else class="ready-text">待上传</span>
            </div>

            <div class="file-actions">
              <el-tag :type="getStatusType(file.status)" size="small">
                {{ getStatusText(file.status) }}
              </el-tag>
              <el-button
                v-if="file.status === 'error'"
                type="primary"
                link
                size="small"
                @click="retryUpload(file)"
              >
                <el-icon><RefreshRight /></el-icon>
                重试
              </el-button>
              <el-button type="danger" link size="small" @click="removeFile(file)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 已上传文件列表 -->
      <el-divider />
      <div class="uploaded-documents">
        <div class="section-header">
          <span class="title">
            <el-icon><FolderOpened /></el-icon>
            已上传文件 ({{ uploadedDocuments.length }})
          </span>
          <el-button type="primary" link size="small" @click="fetchUploadedDocuments" :loading="loadingDocuments">
            <el-icon><RefreshRight /></el-icon>
            刷新
          </el-button>
        </div>

        <el-table :data="uploadedDocuments" v-loading="loadingDocuments" style="width: 100%" empty-text="暂无已上传文件">
          <el-table-column prop="fileName" label="文件名" min-width="200">
            <template #default="{ row }">
              <div class="document-name">
                <el-icon><Document /></el-icon>
                <span :title="row.fileName">{{ row.fileName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="150" />
          <el-table-column prop="createTime" label="上传时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">已处理</el-tag>
              <el-tag v-else-if="row.status === 0" type="warning" size="small">处理中</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="handleDeleteDocument(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.upload-container {
  max-width: 1200px;
  margin: 0 auto;
}

.upload-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .title {
      font-size: 18px;
      font-weight: 600;
    }

    .stats {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }
}

.upload-area {
  margin-bottom: 24px;

  .upload-drop-zone {
    :deep(.el-upload) {
      width: 100%;
    }

    :deep(.el-upload-dragger) {
      width: 100%;
      height: 200px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      border: 2px dashed #d9d9d9;
      border-radius: 8px;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }

      .upload-icon {
        font-size: 48px;
        color: #909399;
        margin-bottom: 16px;
      }

      .upload-text {
        text-align: center;

        p {
          margin: 0;
          color: #606266;
          font-size: 16px;

          em {
            color: #409eff;
            font-style: normal;
            cursor: pointer;
          }
        }

        .upload-tip {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.upload-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #ebeef5;
}

.file-list {
  .file-list-header {
    font-size: 14px;
    font-weight: 600;
    color: #606266;
    margin-bottom: 12px;
  }

  .file-items {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .file-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
    transition: all 0.3s;
    flex-wrap: wrap;
    gap: 12px;

    &:hover {
      background: #ebeef5;
    }

    &.status-success {
      border-left: 4px solid #67c23a;
    }

    &.status-error {
      border-left: 4px solid #f56c6c;
    }

    &.status-uploading {
      border-left: 4px solid #409eff;
    }

    .file-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      min-width: 200px;

      .file-icon {
        font-size: 24px;
        color: #409eff;
      }

      .file-details {
        display: flex;
        flex-direction: column;
        gap: 4px;
        overflow: hidden;

        .file-name {
          font-size: 14px;
          color: #303133;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 300px;
        }

        .file-size {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .file-progress {
      flex: 1;
      min-width: 150px;
      max-width: 200px;

      .success-text {
        color: #67c23a;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .error-text {
        color: #f56c6c;
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .ready-text {
        color: #909399;
        font-size: 12px;
      }
    }

    .file-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.uploaded-documents {
  margin-top: 24px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .document-name {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-icon {
      font-size: 18px;
      color: #409eff;
    }

    span {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 250px;
    }
  }
}

@media (max-width: 768px) {
  .file-item {
    flex-direction: column;
    align-items: flex-start !important;

    .file-progress {
      width: 100%;
      max-width: none !important;
    }
  }
}
</style>
