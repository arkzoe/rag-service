# RAG 智能问答系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5.33-brightgreen.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

一个基于 RAG（Retrieval-Augmented Generation）技术的智能问答系统，支持文档上传、向量检索、角色权限管理和多轮对话。

## 功能特性

- **智能问答**：基于向量检索和 LLM 的精准回答
- **文档管理**：支持 PDF、Word、TXT、Markdown 等格式上传
- **角色权限**：基于角色的文档访问控制
- **会话管理**：支持多轮对话和历史记录
- **向量存储**：使用 Qdrant 高性能向量数据库
- **响应式 UI**：基于 Element Plus 的现代化界面

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|-----|------|------|
| Spring Boot | 3.5.14 | 应用框架 |
| Spring AI | 1.1.4 | AI 开发框架 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| Qdrant | 1.x | 向量数据库 |
| Sa-Token | 1.45.0 | 认证授权框架 |
| OpenAI API | - | 大语言模型接口 |

### 前端

| 技术 | 版本 | 说明 |
|-----|------|------|
| Vue.js | 3.5.33 | 前端框架 |
| Element Plus | 2.13.7 | UI 组件库 |
| Pinia | 3.0.4 | 状态管理 |
| Vue Router | 5.0.6 | 路由管理 |
| Axios | 1.15.2 | HTTP 客户端 |
| Vite | 8.0.10 | 构建工具 |

## 环境要求

- **JDK**: 17 或更高版本
- **Node.js**: 20.19.0 或 >= 22.12.0
- **MySQL**: 8.0 或更高版本
- **Redis**: 6.0 或更高版本
- **Qdrant**: 1.0 或更高版本

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/arkzoe/rag-service.git
cd rag-service
```

### 2. 后端配置

#### 2.1 创建数据库

```sql
-- 创建数据库
CREATE DATABASE rag_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行初始化脚本
-- 位于 backend/src/main/resources/db/create.sql
```

#### 2.2 配置应用

编辑 `backend/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
  
  ai:
    openai:
      api-key: your_openai_api_key
      base-url: https://api.openai.com/v1  # 或代理地址
```

#### 2.3 启动后端

```bash
cd backend
./mvnw spring-boot:run

# 或使用 Maven
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

### 3. 前端配置

#### 3.1 安装依赖

```bash
cd front
pnpm install
# 或 npm install
```

#### 3.2 配置代理

编辑 `front/vite.config.js`：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

#### 3.3 启动前端

```bash
pnpm dev
# 或 npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`

### 4. 访问应用

打开浏览器访问 `http://localhost:5173`

默认登录账号：
- 用户名：`admin`
- 密码：`123456`

## 目录结构

```
rag-system/
├── backend/                    # 后端项目
│   ├── src/main/java/
│   │   └── com/rag/backend/
│   │       ├── common/         # 公共组件
│   │       │   ├── config/     # 配置类
│   │       │   ├── constant/   # 常量定义
│   │       │   ├── exception/  # 异常处理
│   │       │   ├── result/     # 统一响应
│   │       │   └── util/       # 工具类
│   │       ├── controller/     # 控制器层
│   │       ├── dto/            # 数据传输对象
│   │       ├── entity/         # 实体类
│   │       ├── infrastructure/ # 基础设施
│   │       ├── mapper/         # 数据访问层
│   │       └── service/        # 业务逻辑层
│   └── src/main/resources/
│       ├── db/                 # 数据库脚本
│       └── mapper/             # MyBatis XML
├── front/                      # 前端项目
│   ├── src/
│   │   ├── api/                # API 接口
│   │   ├── components/         # 公共组件
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── utils/              # 工具函数
│   │   └── views/              # 页面视图
│   └── public/                 # 静态资源
├── docs/                       # 项目文档
│   ├── frontend-api.md         # 前端 API 文档
│   └── backend-api.md          # 后端 API 文档
└── README.md                   # 项目说明
```

## 核心功能说明

### RAG 问答流程

1. **用户提问**：用户输入问题
2. **向量检索**：系统从 Qdrant 检索相关文档片段（Top-K=20）
3. **权限过滤**：根据用户角色过滤文档
4. **上下文构建**：将相关文档拼接为上下文
5. **LLM 生成**：调用大语言模型生成回答
6. **历史保存**：异步保存问答记录到 MySQL

### 角色权限控制

- 文档上传时可指定可访问角色
- 用户只能检索到其角色有权访问的文档
- 支持多角色匹配

### 会话管理

- 支持创建多个对话会话
- 每个会话独立维护聊天记录
- 支持会话的增删改查

## 配置文件详解

### 后端配置 (application.yaml)

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_system
    username: root
    password: password

# Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

# AI 模型配置
  ai:
    openai:
      api-key: sk-xxx
      base-url: https://api.openai.com/v1
      chat:
        options:
          model: gpt-4
          temperature: 0.7

# 向量数据库配置
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: rag_collection

# 文件上传配置
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

# Sa-Token 配置
sa-token:
  token-name: rag-token
  timeout: 2592000
```

## API 文档

- [前端 API 文档](docs/frontend-api.md) - 前端接口调用说明
- [后端 API 文档](docs/backend-api.md) - 后端接口设计文档

## 部署指南

### 生产环境部署

#### 1. 构建前端

```bash
cd front
pnpm build
```

构建产物位于 `front/dist/` 目录

#### 2. 构建后端

```bash
cd backend
./mvnw clean package -DskipTests
```

构建产物位于 `backend/target/backend-0.0.1-SNAPSHOT.jar`

#### 3. 部署

```bash
# 使用 Docker Compose（推荐）
docker-compose up -d

# 或手动启动
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker 部署

项目提供 `docker-compose.yml` 用于快速部署：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: rag_system
  
  redis:
    image: redis:7-alpine
  
  qdrant:
    image: qdrant/qdrant:latest
  
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - qdrant
  
  frontend:
    build: ./front
    ports:
      - "80:80"
    depends_on:
      - backend
```

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 后端遵循阿里巴巴 Java 开发手册
- 前端使用 Prettier 进行代码格式化
- 提交前请运行测试

## 常见问题

### Q: 上传文件大小限制是多少？

A: 默认限制为 50MB，可在 `application.yaml` 中修改：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
```

### Q: 如何更换 AI 模型？

A: 修改 `application.yaml` 中的模型配置：

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-3.5-turbo  # 或其他支持的模型
```

### Q: 向量检索结果不准确怎么办？

A: 可以尝试：
1. 调整 `topK` 参数（默认 20）
2. 优化文档分块策略
3. 调整 Embedding 模型
4. 检查文档质量

### Q: 如何添加新的角色权限？

A: 
1. 在数据库 `sys_role` 表添加角色
2. 为用户分配角色
3. 上传文档时指定允许访问的角色

### Q: 部署后前端无法连接后端？

A: 检查：
1. 后端服务是否正常运行
2. 前端代理配置是否正确
3. 防火墙是否放行端口
4. 跨域配置是否正确

## 更新日志

### v1.0.0 (2024-XX-XX)

- 初始版本发布
- 支持基础 RAG 问答
- 支持文档上传和管理
- 支持会话管理
- 支持角色权限控制

## 许可证

本项目基于 [MIT](LICENSE) 许可证开源。

## 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 [Issue](https://github.com/yourusername/rag-system/issues)

---

**感谢使用 RAG 智能问答系统！**
