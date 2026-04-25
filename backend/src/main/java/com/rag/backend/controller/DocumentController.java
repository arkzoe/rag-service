package com.rag.backend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.rag.backend.common.result.Result;
import com.rag.backend.entity.Document;
import com.rag.backend.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/doc")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    @SaCheckPermission("doc:upload")
    public Result<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "roles", required = false) String roles) {

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("文件上传请求 - 用户ID: {}, 文件名: {}, 大小: {} bytes",
                userId, file.getOriginalFilename(), file.getSize());

        // 如果没有指定角色，使用当前用户角色
        if (roles == null || roles.isEmpty()) {
            roles = String.join(",", StpUtil.getRoleList());
        }

        try {
            Document doc = documentService.uploadFile(file, roles, userId);
            log.info("文件上传成功 - 文档ID: {}, 文件名: {}", doc.getId(), doc.getFileName());
            return Result.success(doc);
        } catch (Exception e) {
            log.error("文件上传失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
