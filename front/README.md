# RAG 智能问答系统 - 前端应用

基于 Vue 3 + Element Plus 开发的 RAG 智能问答系统前端应用。

## 功能模块

### 1. 登录模块
- 用户名/密码登录
- 表单验证
- 登录状态管理（基于 Sa-Token）
- 自动登录状态保持

### 2. 文件上传模块
- 拖拽上传
- 批量上传
- 上传进度显示
- 文件类型验证（PDF、Word、TXT、Markdown）
- 文件大小限制（50MB）
- 上传状态管理
- 失败重试

### 3. 对话功能模块
- 实时对话界面
- 消息历史展示
- 加载历史消息
- 消息时间分组
- 清空对话
- 发送状态提示

## 技术栈

- **框架**: Vue 3.5+
- **构建工具**: Vite 8+
- **UI 组件库**: Element Plus 2.9+
- **状态管理**: Pinia 3+
- **路由**: Vue Router 5+
- **HTTP 客户端**: Axios
- **样式**: SCSS

## 项目结构

```
front/
├── src/
│   ├── api/              # API 接口封装
│   │   ├── user.js       # 用户相关接口
│   │   ├── document.js   # 文档上传接口
│   │   └── chat.js       # 对话接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # Pinia 状态管理
│   │   └── user.js       # 用户状态
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装
│   ├── views/            # 页面视图
│   │   ├── login/        # 登录页
│   │   ├── layout/       # 布局组件
│   │   ├── chat/         # 对话页面
│   │   └── upload/       # 上传页面
│   ├── App.vue
│   └── main.js
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 安装与运行

### 环境要求
- Node.js >= 20.19.0
- pnpm 或 npm

### 安装依赖

```bash
cd front
pnpm install
# 或
npm install
```

### 开发环境运行

```bash
pnpm dev
# 或
npm run dev
```

应用将在 http://localhost:5173 启动

### 生产环境构建

```bash
pnpm build
# 或
npm run build
```

构建产物将输出到 `dist` 目录

### 预览生产构建

```bash
pnpm preview
# 或
npm run preview
```

## 后端接口配置

### 开发环境代理
在 `vite.config.js` 中已配置代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),
    },
  },
}
```

### 生产环境配置
生产环境中，需要将前端请求代理到后端服务。可以通过以下方式：

1. **Nginx 反向代理**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/front/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

2. **修改请求基础 URL**
在 `src/utils/request.js` 中修改：
```javascript
const request = axios.create({
  baseURL: 'http://your-backend-domain:8080',
  // ...
})
```

## 后端 API 接口

### 用户接口
- `POST /user/login` - 登录
  - 参数: username, password
  - 返回: Sa-Token 信息

- `POST /user/logout` - 登出

### 文档接口
- `POST /doc/upload` - 文件上传
  - 参数: file, roles
  - 需要权限: doc:upload

### 对话接口
- `POST /chat/send` - 发送消息
  - 参数: question (请求体)
  - 返回: answer
  - 需要权限: chat:ask

## 响应式设计

应用已适配不同屏幕尺寸：
- 桌面端 (> 1200px): 完整侧边栏布局
- 平板端 (768px - 1200px): 自适应布局
- 移动端 (< 768px): 紧凑布局，优化触摸交互

## 浏览器支持

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 开发说明

### 代码规范
- 使用 Vue 3 Composition API
- 组件使用 `<script setup>` 语法
- 样式使用 SCSS
- 使用 Element Plus 图标

### 提交前检查
```bash
# 代码格式化
pnpm format
```

## 许可证

MIT
