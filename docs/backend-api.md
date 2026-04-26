# 后端 API 文档

## 概述

本文档详细描述了 RAG 系统后端 API 的设计与实现，包括接口功能、请求处理流程、数据验证规则、业务逻辑、数据库交互及性能指标。

## 系统架构

### 技术栈

| 层级 | 技术 |
|-----|------|
| 框架 | Spring Boot 3.5.14 |
| ORM | MyBatis-Plus 3.5.15 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 向量数据库 | Qdrant |
| AI 模型 | Spring AI + OpenAI API |
| 认证 | Sa-Token |
| 安全 | Spring Security |

---

## 1. 用户认证模块

### 1.1 用户登录

**接口功能描述**：验证用户身份并生成访问令牌

**URL**：`POST /user/login`

**请求处理流程**：

```
1. 接收用户名和密码
2. 查询数据库验证用户存在性
3. 明文密码比对验证
4. 生成 Sa-Token
5. 返回 Token 信息
```

**数据验证规则**：

| 字段 | 规则 | 说明 |
|-----|------|------|
| username | @NotBlank | 用户名不能为空 |
| password | @NotBlank | 密码不能为空 |

**业务逻辑说明**：

- 使用明文密码验证（开发环境简化处理）
- 登录成功后调用 `StpUtil.login(user.getId())` 生成 Token
- Token 有效期：30 天（2592000 秒）
- Token 风格：UUID

**数据库交互**：

```sql
-- 查询用户信息
SELECT * FROM sys_user WHERE username = ?
```

**接口性能指标**：

| 指标 | 值 |
|-----|---|
| 平均响应时间 | < 100ms |
| 并发支持 | 依赖 Redis 性能 |
| Token 生成 | ~10ms |

---

### 1.2 用户登出

**接口功能描述**：销毁用户会话 Token

**URL**：`POST /user/logout`

**请求处理流程**：

```
1. 从请求头获取 Token
2. 调用 StpUtil.logout() 销毁会话
3. 记录登出日志
```

**业务逻辑说明**：

- 支持并发登录（is-concurrent: true）
- 多设备登录共享 Token（is-share: true）
- 登出后 Token 立即失效

---

### 1.3 获取用户信息

**接口功能描述**：获取当前登录用户详细信息

**URL**：`GET /user/info`

**请求处理流程**：

```
1. 从 Token 解析用户 ID
2. 查询用户表获取信息
3. 脱敏处理（清除密码字段）
4. 返回用户信息
```

**数据库交互**：

```sql
SELECT id, username, nickname, email, create_time 
FROM sys_user 
WHERE id = ?
```

---

## 2. 聊天模块

### 2.1 发送消息

**接口功能描述**：执行 RAG 问答流程，结合向量检索和 LLM 生成回答

**URL**：`POST /chat/send`

**权限要求**：`chat:ask`

**请求处理流程**：

```
1. 参数校验（问题非空、长度限制）
2. 获取当前用户 ID 和角色列表
3. 向量检索（Top-K=20）
4. 权限过滤文档
5. 构建上下文和系统提示词
6. 调用 LLM 生成回答
7. 异步保存聊天记录
8. 返回答案
```

**数据验证规则**：

| 字段 | 规则 | 说明 |
|-----|------|------|
| question | @NotBlank, @Size(max=2000) | 问题不能为空，最长 2000 字符 |
| sessionId | 可选 | 会话 ID，为空时创建新会话 |

**业务逻辑说明**：

**向量检索流程**：

```java
// 1. 执行相似度搜索（不带权限过滤）
SearchRequest searchRequest = SearchRequest.builder()
    .query(question)
    .topK(20)  // 检索 Top 20 条
    .build();

List<Document> allDocuments = vectorStore.similaritySearch(searchRequest);

// 2. 手动权限过滤
List<Document> documents = filterByRoles(allDocuments, userRoles);
// 限制返回 5 条最相关文档
```

**权限过滤逻辑**：

```java
private List<Document> filterByRoles(List<Document> documents, List<String> userRoles) {
    return documents.stream()
        .filter(doc -> {
            Object allowedRolesObj = doc.getMetadata().get("allowed_roles");
            if (allowedRolesObj == null) return true; // 无权限限制，允许访问
            
            String allowedRoles = allowedRolesObj.toString();
            // 检查用户是否有任一角色匹配
            return userRoles.stream()
                .anyMatch(role -> allowedRoles.contains(role));
        })
        .limit(5)
        .collect(Collectors.toList());
}
```

**系统提示词模板**：

```
你是一个专业的 AI 助手。请基于以下参考资料回答用户问题。
如果参考资料中没有相关信息，请明确告知用户。

参考资料:
{context}

用户问题: {question}
```

**数据库交互**：

```sql
-- 异步保存聊天记录
INSERT INTO chat_history 
(session_id, user_id, query, answer, source_count, create_time) 
VALUES (?, ?, ?, ?, ?, NOW())
```

**外部服务调用**：

| 服务 | 操作 | 超时 |
|-----|------|------|
| Qdrant | similaritySearch | 5s |
| OpenAI API | chat completion | 120s |

**接口性能指标**：

| 指标 | 值 | 说明 |
|-----|---|------|
| 向量检索 | 50-200ms | 依赖 Qdrant 性能 |
| LLM 生成 | 3-30s | 依赖模型响应速度 |
| 总响应时间 | 5-35s | 包含流式输出 |
| 并发限制 | 10 | 防止 API 限流 |

---

### 2.2 获取聊天历史

**接口功能描述**：获取指定会话的历史聊天记录

**URL**：`GET /chat/history/{sessionId}`

**权限要求**：`chat:ask`

**请求处理流程**：

```
1. 解析 sessionId 路径参数
2. 获取当前用户 ID
3. 查询聊天记录表
4. 按时间排序返回
```

**数据库交互**：

```sql
SELECT * FROM chat_history 
WHERE session_id = ? AND user_id = ? 
ORDER BY create_time ASC
```

**接口性能指标**：

| 指标 | 值 |
|-----|---|
| 平均响应时间 | < 50ms |
| 单次查询上限 | 1000 条记录 |

---

## 3. 会话管理模块

### 3.1 创建会话

**接口功能描述**：创建新的对话会话

**URL**：`POST /api/chat/session/create`

**请求处理流程**：

```
1. 参数校验（标题非空、长度限制）
2. 生成 UUID 作为 sessionId
3. 插入数据库
4. 清除用户会话列表缓存
5. 缓存新会话信息
6. 返回会话 DTO
```

**数据验证规则**：

| 字段 | 规则 | 说明 |
|-----|------|------|
| title | @NotBlank, @Size(max=100) | 标题必填，最长 100 字符 |
| description | @Size(max=500) | 描述最长 500 字符 |

**数据库交互**：

```sql
INSERT INTO chat_session 
(session_id, user_id, title, description, message_count, created_at, updated_at, is_deleted) 
VALUES (?, ?, ?, ?, 0, NOW(), NOW(), 0)
```

**Redis 缓存策略**：

| 缓存 Key | 过期时间 | 说明 |
|---------|---------|------|
| session:{sessionId} | 24 小时 | 单个会话详情 |
| session:list:{userId} | 1 小时 | 用户会话列表 |

**接口性能指标**：

| 指标 | 值 |
|-----|---|
| 平均响应时间 | < 50ms |
| 缓存清除 | ~5ms |

---

### 3.2 获取会话列表

**接口功能描述**：获取当前用户的所有会话列表

**URL**：`GET /api/chat/session/list`

**请求处理流程**：

```
1. 获取当前用户 ID
2. 尝试从 Redis 缓存获取
3. 缓存未命中，查询数据库
4. 转换为 DTO 列表
5. 写入缓存
6. 返回结果
```

**数据库交互**：

```sql
SELECT * FROM chat_session 
WHERE user_id = ? AND is_deleted = 0 
ORDER BY updated_at DESC
```

**缓存策略**：

- 使用缓存优先模式
- 列表缓存 1 小时
- 数据变更时主动清除缓存

**接口性能指标**：

| 指标 | 值 |
|-----|---|
| 缓存命中响应 | < 10ms |
| 缓存未命中响应 | < 50ms |

---

### 3.3 获取会话详情

**接口功能描述**：获取指定会话的详细信息

**URL**：`GET /api/chat/session/detail/{sessionId}`

**请求处理流程**：

```
1. 解析 sessionId 路径参数
2. 尝试从 Redis 缓存获取
3. 缓存未命中，查询数据库
4. 写入缓存
5. 返回会话 DTO
```

**数据库交互**：

```sql
SELECT * FROM chat_session WHERE session_id = ?
```

---

### 3.4 删除会话

**接口功能描述**：逻辑删除单个会话

**URL**：`POST /api/chat/session/delete/{sessionId}`

**请求处理流程**：

```
1. 解析 sessionId
2. 查询会话验证存在性和所有权
3. 设置 is_deleted = 1
4. 更新 updated_at
5. 清除相关缓存
```

**数据验证规则**：

- 验证会话存在
- 验证用户所有权（只能删除自己的会话）

**数据库交互**：

```sql
UPDATE chat_session 
SET is_deleted = 1, updated_at = NOW() 
WHERE session_id = ? AND user_id = ?
```

---

### 3.5 批量删除会话

**接口功能描述**：批量删除多个会话

**URL**：`POST /api/chat/session/delete/batch`

**请求处理流程**：

```
1. 解析 sessionIds 列表
2. 遍历调用单个删除逻辑
3. 记录失败日志
4. 返回删除数量
```

**数据验证规则**：

| 字段 | 规则 | 说明 |
|-----|------|------|
| sessionIds | @NotEmpty | 会话 ID 列表不能为空 |

**事务处理**：

- 每个会话删除独立事务
- 单个失败不影响其他
- 失败记录 WARN 日志

---

### 3.6 更新会话

**接口功能描述**：更新会话标题和描述

**URL**：`POST /api/chat/session/update/{sessionId}`

**请求处理流程**：

```
1. 解析参数
2. 验证会话存在性和所有权
3. 更新数据库
4. 清除缓存
5. 返回更新后的会话
```

**数据验证规则**：

| 字段 | 规则 | 说明 |
|-----|------|------|
| title | @Size(max=100) | 标题最长 100 字符 |
| description | @Size(max=500) | 描述最长 500 字符 |

---

## 4. 文档管理模块

### 4.1 上传文件

**接口功能描述**：上传文档文件，解析内容并向量化存储

**URL**：`POST /doc/upload`

**权限要求**：`doc:upload`

**请求处理流程**：

```
1. 文件校验（非空、大小、类型）
2. 生成存储路径（按日期分目录）
3. 保存文件到本地磁盘
4. 使用 Apache Tika 解析文件内容
5. 保存元数据到 MySQL
6. 向量化并存入 Qdrant
7. 缓存文档信息到 Redis
8. 返回文档信息
```

**数据验证规则**：

| 规则 | 值 | 说明 |
|-----|---|------|
| 文件大小限制 | 50MB | max-file-size |
| 请求大小限制 | 100MB | max-request-size |
| 允许的文件类型 | PDF, DOC, DOCX, TXT, MD | 通过 ContentType 或扩展名校验 |

**支持的文件类型**：

| 扩展名 | MIME 类型 |
|-------|----------|
| .pdf | application/pdf |
| .doc | application/msword |
| .docx | application/vnd.openxmlformats-officedocument.wordprocessingml.document |
| .txt | text/plain |
| .md | text/markdown, text/x-markdown |

**业务逻辑说明**：

**文件存储结构**：

```
uploads/
├── 2024/
│   ├── 01/
│   │   ├── 01/
│   │   │   ├── uuid_filename.pdf
│   │   │   └── uuid_filename.docx
```

**内容解析**：

```java
// 使用 Apache Tika 解析
Tika tika = new Tika();
String content = tika.parseToString(file);
```

**向量化存储**：

```java
// 使用 Spring AI 的 VectorStore
// 1. 文本分块
// 2. 调用 Embedding 模型生成向量
// 3. 存储到 Qdrant，附带元数据（文档ID、角色权限等）
```

**数据库交互**：

```sql
INSERT INTO doc_metadata 
(file_name, file_path, file_type, file_size, content_text, allowed_roles, upload_user_id, create_time) 
VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
```

**接口性能指标**：

| 指标 | 值 | 说明 |
|-----|---|------|
| 小文件 (<1MB) | 2-5s | 解析+向量化 |
| 大文件 (10-50MB) | 10-30s | 依赖内容复杂度 |
| 并发上传限制 | 3 | 防止资源耗尽 |

---

### 4.2 获取文档列表

**接口功能描述**：获取当前用户上传的文档列表

**URL**：`GET /doc/list`

**权限要求**：`doc:upload`

**请求处理流程**：

```
1. 获取当前用户 ID
2. 查询文档元数据表
3. 返回文档列表
```

**数据库交互**：

```sql
SELECT * FROM doc_metadata 
WHERE upload_user_id = ? 
ORDER BY create_time DESC
```

---

### 4.3 删除文档

**接口功能描述**：删除文档（文件、向量、元数据）

**URL**：`POST /doc/delete/{docId}`

**权限要求**：`doc:upload`

**请求处理流程**：

```
1. 验证文档存在性和所有权
2. 删除本地文件
3. 删除 Qdrant 向量
4. 删除 MySQL 元数据
5. 清除 Redis 缓存
6. 返回成功
```

**数据库交互**：

```sql
-- 查询文档
SELECT * FROM doc_metadata WHERE id = ?

-- 删除元数据（逻辑删除或物理删除）
DELETE FROM doc_metadata WHERE id = ?
```

**Qdrant 操作**：

```java
// 根据文档 ID 删除相关向量点
qdrantClient.deletePoints(
    collectionName,
    PointsSelector.newBuilder()
        .setFilter(Filter.newBuilder()
            .addMust(FieldCondition.newBuilder()
                .setKey("doc_id")
                .setMatch(Match.newBuilder().setInteger(docId))
            )
        )
        .build()
);
```

---

## 5. 数据模型

### 5.1 数据库表结构

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(100) | 密码（明文存储） |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| status | TINYINT | 状态 |
| create_time | DATETIME | 创建时间 |

#### chat_session（会话表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(36) | UUID 会话标识 |
| user_id | BIGINT | 用户 ID |
| title | VARCHAR(100) | 会话标题 |
| description | VARCHAR(500) | 会话描述 |
| message_count | INT | 消息数量 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除标志 |

#### chat_history（聊天记录表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| session_id | VARCHAR(36) | 会话 ID |
| user_id | BIGINT | 用户 ID |
| query | TEXT | 用户问题 |
| answer | TEXT | AI 回答 |
| tokens_used | INT | Token 消耗 |
| source_count | INT | 引用文档数 |
| create_time | DATETIME | 创建时间 |

#### doc_metadata（文档元数据表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| file_name | VARCHAR(255) | 原始文件名 |
| file_path | VARCHAR(500) | 存储路径 |
| file_type | VARCHAR(50) | 文件类型 |
| file_size | BIGINT | 文件大小（字节） |
| content_text | TEXT | 内容预览（前 10000 字符） |
| allowed_roles | JSON | 可访问角色列表 |
| upload_user_id | BIGINT | 上传用户 ID |
| create_time | DATETIME | 创建时间 |

---

## 6. 缓存策略

### 6.1 Redis Key 设计

| Key 模式 | 示例 | 过期时间 |
|---------|------|---------|
| session:{id} | session:550e8400-... | 24h |
| session:list:{userId} | session:list:1 | 1h |
| doc:{id} | doc:1 | 24h |
| user:{id} | user:1 | 30min |

### 6.2 缓存更新策略

- **写入时更新**：数据变更后立即清除相关缓存
- **延迟双删**：先删缓存 → 更新数据库 → 延迟删缓存
- **定时过期**：设置合理的过期时间，允许短暂不一致

---

## 7. 安全配置

### 7.1 Sa-Token 配置

```yaml
sa-token:
  token-name: rag-token
  timeout: 2592000  # 30天
  is-concurrent: true
  is-share: true
  token-style: uuid
  is-read-cookie: false
  is-read-header: true
```

### 7.2 权限注解

| 权限码 | 说明 |
|-------|------|
| chat:ask | 聊天问答权限 |
| doc:upload | 文档上传权限 |

---

## 8. 异常处理

### 8.1 全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常: " + e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Result.error("参数校验失败: " + message);
    }
}
```

### 8.2 错误码定义

| 错误码 | 说明 |
|-------|------|
| 200 | 操作成功 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 系统异常 |

---

## 9. 性能优化

### 9.1 异步处理

```java
@Async("taskExecutor")
public void saveHistoryAsync(...) {
    // 异步保存聊天记录
}
```

### 9.2 连接池配置

| 组件 | 配置 |
|-----|------|
| MySQL | HikariCP 默认配置 |
| Redis | Lettuce 连接池 |
| Qdrant | gRPC 连接池 |

### 9.3 超时配置

| 操作 | 超时时间 |
|-----|---------|
| HTTP 请求 | 120s |
| 异步请求 | 120s |
| AI 模型调用 | 120s |
| 数据库查询 | 30s |
