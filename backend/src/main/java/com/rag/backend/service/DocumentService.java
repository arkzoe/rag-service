package com.rag.backend.service;

import com.rag.backend.entity.Document;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* 文档服务接口
*/
public interface DocumentService extends IService<Document> {

    /**
     * 上传文件并处理向量化
     * @param file 上传的文件
     * @param roles 可访问该文档的角色列表
     * @param userId 上传用户ID
     * @return 文档实体
     */
    Document uploadFile(MultipartFile file, String roles, Long userId);

    /**
     * 获取用户的文档列表
     * @param userId 用户ID
     * @return 文档列表
     */
    List<Document> getUserDocuments(Long userId);

    /**
     * 删除文档（同时删除Qdrant中的向量）
     * @param userId 用户ID
     * @param docId 文档ID
     */
    void deleteDocument(Long userId, Long docId);
}
