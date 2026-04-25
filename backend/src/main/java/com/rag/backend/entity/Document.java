package com.rag.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 文档元数据表
 * @TableName doc_metadata
 */
@TableName(value ="doc_metadata")
@Data
@Accessors(chain = true)
public class Document implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件存储路径/URL
     */
    private String filePath;

    /**
     * 文件类型: PDF, DOCX, TXT
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;
    private String contentText;
    /**
     * 可见角色列表 (JSON数组: ["HR", "ADMIN"])
     */
    private Object allowedRoles;

    /**
     * 上传者ID
     */
    private Long uploadUserId;

    /**
     * 状态: 1-已处理, 0-处理中, -1-失败
     */
    private Integer status;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}