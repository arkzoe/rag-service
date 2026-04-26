-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS rag_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rag_system;

-- ==========================================
-- 1. 系统用户与权限模块 (Sa-Token 核心支撑)
-- ==========================================

-- 用户表
CREATE TABLE `sys_user` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                            `password` VARCHAR(100) NOT NULL COMMENT '密码 (加密存储)',
                            `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                            `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                            `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                            `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                            `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 角色表
CREATE TABLE `sys_role` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称 (如: ADMIN, HR)',
                            `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码 (Sa-Token 使用)',
                            `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 用户角色关联表
CREATE TABLE `sys_user_role` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                 `role_id` BIGINT NOT NULL COMMENT '角色ID',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ==========================================
-- 2. 知识库与文档模块 (MyBatis-Plus + Qdrant)
-- ==========================================

-- 文档元数据表 (对应 Qdrant 的 Payload 信息)
-- 文档元数据表 (对应 Qdrant 的 Payload 信息)
CREATE TABLE `doc_metadata` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
                                `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径/URL',
                                `file_type` VARCHAR(20) DEFAULT 'PDF' COMMENT '文件类型: PDF, DOCX, TXT',
                                `file_size` BIGINT DEFAULT 0 COMMENT '文件大小 (字节)',
                                `content_text` LONGTEXT COMMENT '文件解析后的纯文本内容', -- ✅ 新增此字段
                                `allowed_roles` JSON DEFAULT NULL COMMENT '可见角色列表 (JSON数组: ["HR", "ADMIN"])',
                                `upload_user_id` BIGINT DEFAULT NULL COMMENT '上传者ID',
                                `status` TINYINT DEFAULT 1 COMMENT '状态: 1-已处理, 0-处理中, -1-失败',
                                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `idx_upload_user` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档元数据表';
-- ==========================================
-- 3. 会话与历史记录模块 (Redis + MySQL)
-- ==========================================

-- 会话归档表 (Redis 热数据定期归档至此)
CREATE TABLE `chat_history` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `session_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识 (UUID)',
                                `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                `query` TEXT NOT NULL COMMENT '用户提问内容',
                                `answer` TEXT COMMENT 'AI 回答内容',
                                `tokens_used` INT DEFAULT 0 COMMENT '消耗 Token 数',
                                `source_count` INT DEFAULT 0 COMMENT '引用文档数量',
                                `feedback` TINYINT DEFAULT NULL COMMENT '用户反馈: 1-点赞, 0-点踩',
                                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `idx_session_id` (`session_id`),
                                KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话历史记录表';
-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_session (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                            session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识(UUID)',
                                            user_id BIGINT NOT NULL COMMENT '用户ID',
                                            title VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
                                            description VARCHAR(500) DEFAULT NULL COMMENT '对话描述',
                                            message_count INT DEFAULT 0 COMMENT '消息数量',
                                            last_message_preview VARCHAR(200) DEFAULT NULL COMMENT '最后一条消息预览',
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            is_deleted TINYINT DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
                                            INDEX idx_user_id (user_id),
                                            INDEX idx_session_id (session_id),
                                            INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- ==========================================
-- 4. 初始化测试数据 (可选)
-- ==========================================

-- 插入默认角色
INSERT INTO `sys_role` (`role_name`, `role_code`) VALUES
                                                      ('超级管理员', 'ADMIN'),
                                                      ('人力资源', 'HR'),
                                                      ('普通员工', 'EMPLOYEE');

-- 插入一个默认管理员账号 (密码为明文: 123456)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) VALUES
    ('admin', '123456', '系统管理员', 1);

-- 关联 admin 用户与 ADMIN 角色 (假设 ID 均为 1)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);|
