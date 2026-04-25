package com.rag.backend.service;

import com.rag.backend.entity.Document;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

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
}
