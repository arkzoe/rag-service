# 前端 API 文档

## 概述

本文档详细描述了 RAG 系统前端与后端交互的所有 API 接口。前端使用 Axios 进行 HTTP 请求，所有 API 函数封装在 `src/api/` 目录下。

## 基础配置

### 请求基础配置

```javascript
// src/utils/request.js
const request = axios.create({
  baseURL: '/api',
  timeout: 120000,  // 超时时间 120 秒
  headers: {
    'Content-Type': 'application/json',
  },
})
```

### 认证方式

- 使用 Sa-Token 进行身份认证
- Token 名称：`rag-token`
- Token 通过请求头传递

---

## 1. 用户认证模块

### 1.1 用户登录

**接口名称**：用户登录

**请求方法**：POST

**URL 路径**：`/user/login`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（明文） |

**请求示例**：

```javascript
import { login } from '@/api/user'

login('admin', '123456')
  .then(tokenInfo => {
    console.log('登录成功', tokenInfo)
  })
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "tokenName": "rag-token",
    "tokenValue": "uuid-xxxx-xxxx-xxxx",
    "isLogin": true,
    "loginId": 1,
    "loginType": "login"
  }
}
```

**错误码说明**：

| 错误码 | 说明 |
|-------|------|
| 500 | 用户名或密码错误 |

---

### 1.2 用户登出

**接口名称**：用户登出

**请求方法**：POST

**URL 路径**：`/user/logout`

**请求参数**：无

**请求示例**：

```javascript
import { logout } from '@/api/user'

logout().then(() => {
  console.log('登出成功')
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "登出成功"
}
```

---

### 1.3 获取用户信息

**接口名称**：获取用户信息

**请求方法**：GET

**URL 路径**：`/user/info`

**请求参数**：无

**请求示例**：

```javascript
import { getUserInfo } from '@/api/user'

getUserInfo().then(user => {
  console.log('用户信息', user)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@example.com",
    "createTime": "2024-01-01T00:00:00"
  }
}
```

---

## 2. 聊天模块

### 2.1 发送消息

**接口名称**：发送聊天消息

**请求方法**：POST

**URL 路径**：`/chat/send`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| question | string | 是 | 用户问题，最大长度 2000 字符 |
| sessionId | string | 否 | 会话 ID，为空时创建新会话 |

**请求示例**：

```javascript
import { sendMessage } from '@/api/chat'

sendMessage('什么是 RAG？', 'session-xxx')
  .then(answer => {
    console.log('AI 回答', answer)
  })
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "RAG（Retrieval-Augmented Generation）是一种结合检索和生成的 AI 技术..."
}
```

**错误码说明**：

| 错误码 | 说明 |
|-------|------|
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问（缺少 chat:ask 权限） |
| 500 | 系统异常 |

---

### 2.2 获取聊天历史

**接口名称**：获取会话历史记录

**请求方法**：GET

**URL 路径**：`/chat/history/{sessionId}`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| sessionId | string | 是 | 会话 ID（路径参数） |

**请求示例**：

```javascript
import { getChatHistory } from '@/api/chat'

getChatHistory('session-xxx')
  .then(history => {
    console.log('历史记录', history)
  })
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "sessionId": "session-xxx",
      "userId": 1,
      "query": "什么是 RAG？",
      "answer": "RAG 是一种...",
      "sourceCount": 5,
      "createTime": [2024, 1, 1, 12, 0, 0]
    }
  ]
}
```

---

## 3. 会话管理模块

### 3.1 创建会话

**接口名称**：创建对话会话

**请求方法**：POST

**URL 路径**：`/api/chat/session/create`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| title | string | 是 | 对话标题，最大长度 100 字符 |
| description | string | 否 | 对话描述，最大长度 500 字符 |

**请求示例**：

```javascript
import { createSession } from '@/api/chatSession'

createSession({
  title: '新对话',
  description: '关于 RAG 的讨论'
}).then(session => {
  console.log('会话创建成功', session)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 1,
    "title": "新对话",
    "description": "关于 RAG 的讨论",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

---

### 3.2 获取会话列表

**接口名称**：获取用户对话列表

**请求方法**：GET

**URL 路径**：`/api/chat/session/list`

**请求参数**：无

**请求示例**：

```javascript
import { getSessionList } from '@/api/chatSession'

getSessionList().then(sessions => {
  console.log('会话列表', sessions)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "sessionId": "550e8400-e29b-41d4-a716-446655440000",
      "userId": 1,
      "title": "新对话",
      "description": "关于 RAG 的讨论",
      "createTime": "2024-01-01T12:00:00",
      "updateTime": "2024-01-01T12:00:00"
    }
  ]
}
```

---

### 3.3 获取会话详情

**接口名称**：获取会话详情

**请求方法**：GET

**URL 路径**：`/api/chat/session/detail/{sessionId}`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| sessionId | string | 是 | 会话 ID（路径参数） |

**请求示例**：

```javascript
import { getSessionDetail } from '@/api/chatSession'

getSessionDetail('session-xxx').then(session => {
  console.log('会话详情', session)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 1,
    "title": "新对话",
    "description": "关于 RAG 的讨论",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

**错误码说明**：

| 错误码 | 说明 |
|-------|------|
| 500 | 会话不存在 |

---

### 3.4 删除会话

**接口名称**：删除单个对话会话

**请求方法**：POST

**URL 路径**：`/api/chat/session/delete/{sessionId}`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| sessionId | string | 是 | 会话 ID（路径参数） |

**请求示例**：

```javascript
import { deleteSession } from '@/api/chatSession'

deleteSession('session-xxx').then(() => {
  console.log('会话删除成功')
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

### 3.5 批量删除会话

**接口名称**：批量删除对话会话

**请求方法**：POST

**URL 路径**：`/api/chat/session/delete/batch`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| sessionIds | string[] | 是 | 会话 ID 列表 |

**请求示例**：

```javascript
import { batchDeleteSessions } from '@/api/chatSession'

batchDeleteSessions({
  sessionIds: ['session-1', 'session-2']
}).then(result => {
  console.log('删除数量', result.deletedCount)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "deletedCount": 2
  }
}
```

---

### 3.6 更新会话

**接口名称**：更新对话会话信息

**请求方法**：POST

**URL 路径**：`/api/chat/session/update/{sessionId}`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| sessionId | string | 是 | 会话 ID（路径参数） |
| title | string | 否 | 对话标题，最大长度 100 字符 |
| description | string | 否 | 对话描述，最大长度 500 字符 |

**请求示例**：

```javascript
import { updateSession } from '@/api/chatSession'

updateSession('session-xxx', {
  title: '更新后的标题',
  description: '更新后的描述'
}).then(session => {
  console.log('会话更新成功', session)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": 1,
    "title": "更新后的标题",
    "description": "更新后的描述",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:30:00"
  }
}
```

---

## 4. 文档管理模块

### 4.1 上传文件

**接口名称**：上传文档文件

**请求方法**：POST

**URL 路径**：`/doc/upload`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| file | File | 是 | 文件对象，支持 PDF、DOC、DOCX、TXT 等格式 |
| roles | string | 否 | 可访问该文档的角色，多个角色用逗号分隔 |

**请求示例**：

```javascript
import { uploadFile } from '@/api/document'

const file = document.getElementById('fileInput').files[0]
uploadFile(file, 'admin,user', (progressEvent) => {
  const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
  console.log('上传进度', percent + '%')
}).then(doc => {
  console.log('文件上传成功', doc)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "fileName": "document.pdf",
    "fileType": "application/pdf",
    "fileSize": 1024000,
    "allowedRoles": "admin,user",
    "userId": 1,
    "createTime": "2024-01-01T12:00:00"
  }
}
```

**错误码说明**：

| 错误码 | 说明 |
|-------|------|
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问（缺少 doc:upload 权限） |
| 500 | 文件上传失败 |

---

### 4.2 获取文档列表

**接口名称**：获取用户文档列表

**请求方法**：GET

**URL 路径**：`/doc/list`

**请求参数**：无

**请求示例**：

```javascript
import { getDocumentList } from '@/api/document'

getDocumentList().then(documents => {
  console.log('文档列表', documents)
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "fileName": "document.pdf",
      "fileType": "application/pdf",
      "fileSize": 1024000,
      "allowedRoles": "admin,user",
      "userId": 1,
      "createTime": "2024-01-01T12:00:00"
    }
  ]
}
```

---

### 4.3 删除文档

**接口名称**：删除文档

**请求方法**：POST

**URL 路径**：`/doc/delete/{docId}`

**请求参数**：

| 参数名称 | 类型 | 是否必填 | 说明 |
|---------|------|---------|------|
| docId | number | 是 | 文档 ID（路径参数） |

**请求示例**：

```javascript
import { deleteDocument } from '@/api/document'

deleteDocument(1).then(() => {
  console.log('文档删除成功')
})
```

**响应数据格式**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "删除成功"
}
```

**错误码说明**：

| 错误码 | 说明 |
|-------|------|
| 500 | 文档删除失败 |

---

## 通用错误码

| 错误码 | 说明 |
|-------|------|
| 200 | 操作成功 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 系统异常 |

---

## 请求拦截器

前端请求拦截器会自动处理以下逻辑：

1. **请求拦截**：自动添加 Token 到请求头
2. **响应拦截**：
   - 状态码 200：返回响应数据
   - 状态码 401：清除登录状态，提示重新登录
   - 其他状态码：显示错误消息

```javascript
// 响应拦截器处理逻辑
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    } else if (res.code === 401) {
      // 清除登录状态，跳转登录页
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.msg || '未登录'))
    } else {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
  },
  (error) => {
    const message = error.response?.data?.msg || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)
```
