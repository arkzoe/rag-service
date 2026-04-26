package com.rag.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.backend.entity.Document;
import com.rag.backend.infrastructure.RedisUtil;
import com.rag.backend.mapper.DocumentMapper;
import com.rag.backend.service.DocumentService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document>
        implements DocumentService {

    private final DocumentMapper documentMapper;

    private final VectorStore vectorStore;

    private final RedisUtil redisUtil;

    private final QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:rag_collection}")
    private String collectionName;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:52428800}") // 默认50MB
    private Long maxFileSize;

    private final Tika tika = new Tika();

    @Override
    public Document uploadFile(MultipartFile file, String roles, Long userId) {
        // 1. 文件校验
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        log.info("开始处理文件上传: {}, 用户ID: {}", originalFilename, userId);

        // 2. 生成存储路径（按日期分目录）
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        Path relativePath = Paths.get(dateDir, newFilename);
        Path absolutePath = Paths.get(uploadDir, dateDir, newFilename);

        try {
            // 3. 保存文件到本地
            Files.createDirectories(absolutePath.getParent());
            file.transferTo(absolutePath);
            log.info("文件已保存到: {}", absolutePath);

            // 4. 使用 Tika 解析文件内容
            String content = parseFileContent(absolutePath.toFile());
            log.info("文件内容解析完成, 字符数: {}", content.length());

            // 5. 保存元数据到 MySQL
            // 将角色字符串转换为JSON数组格式
            List<String> roleList = roles != null && !roles.isEmpty()
                    ? Arrays.asList(roles.split(","))
                    : List.of("ALL");
            String rolesJson = new ObjectMapper().writeValueAsString(roleList);

            // 简化文件类型存储，只取主要部分
            String fileType = file.getContentType();
            if (fileType != null && fileType.length() > 50) {
                fileType = fileType.substring(0, 50);
            }

            Document doc = new Document()
                    .setFileName(originalFilename)
                    .setFilePath(relativePath.toString())
                    .setFileType(fileType)
                    .setFileSize(file.getSize())
                    .setContentText(content.substring(0, Math.min(content.length(), 10000))) // 限制存储长度
                    .setAllowedRoles(rolesJson)
                    .setUploadUserId(userId);
            documentMapper.insert(doc);
            log.info("文档元数据已保存到MySQL, ID: {}", doc.getId());

            // 6. 向量化并存入 Qdrant
            indexToVectorStore(doc, content);

            // 7. 缓存文档信息到Redis
            cacheDocumentInfo(doc);

            return doc;

        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "文件大小超过限制，最大允许: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 校验文件类型（通过ContentType或文件扩展名）
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        List<String> allowedTypes = List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "text/markdown",
                "text/x-markdown"
        );

        List<String> allowedExts = List.of(".pdf", ".doc", ".docx", ".txt", ".md");

        // 如果ContentType不在允许列表中，检查文件扩展名
        if (!allowedTypes.contains(contentType) && !allowedExts.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType + ", 扩展名: " + ext);
        }
    }

    /**
     * 解析文件内容
     */
    private String parseFileContent(java.io.File file) {
        try {
            String content = tika.parseToString(file);
            return content != null ? content.trim() : "";
        } catch (TikaException | IOException e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 将文档索引到向量存储
     */
    private void indexToVectorStore(Document doc, String content) {
        try {
            // 将长文本分块（简单实现，每块1000字符）
            int chunkSize = 1000;
            int overlap = 100;

            for (int i = 0; i < content.length(); i += chunkSize - overlap) {
                String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));

                org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(
                        chunk,
                        Map.of(
                                "doc_id", doc.getId().toString(),
                                "filename", doc.getFileName(),
                                "allowed_roles", doc.getAllowedRoles() != null ? doc.getAllowedRoles().toString() : "[\"ALL\"]",
                                "chunk_index", String.valueOf(i / (chunkSize - overlap))
                        )
                );

                vectorStore.add(List.of(aiDoc));
            }

            log.info("文档已向量化并存储到Qdrant, ID: {}, 分块数: {}",
                    doc.getId(), (content.length() + chunkSize - 1) / chunkSize);

        } catch (Exception e) {
            log.error("文档向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档向量化失败: " + e.getMessage());
        }
    }

    /**
     * 缓存文档信息到Redis
     */
    private void cacheDocumentInfo(Document doc) {
        try {
            String cacheKey = "doc:info:" + doc.getId();
            redisUtil.set(cacheKey, doc, 1, TimeUnit.HOURS);
            log.debug("文档信息已缓存到Redis: {}", cacheKey);
        } catch (Exception e) {
            log.warn("文档信息缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Document> getUserDocuments(Long userId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getUploadUserId, userId)
               .orderByDesc(Document::getCreateTime);
        return documentMapper.selectList(wrapper);
    }

    @Override
    public void deleteDocument(Long userId, Long docId) {
        // 1. 查询文档
        Document doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!doc.getUploadUserId().equals(userId)) {
            throw new RuntimeException("无权删除该文档");
        }

        log.info("开始删除文档 - ID: {}, 文件名: {}", docId, doc.getFileName());

        // 2. 删除Qdrant中的向量数据
        try {
            deleteFromVectorStore(docId);
        } catch (Exception e) {
            log.error("删除Qdrant向量失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除向量数据失败: " + e.getMessage());
        }

        // 3. 删除本地文件
        try {
            Path filePath = Paths.get(uploadDir, doc.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("本地文件已删除: {}", filePath);
        } catch (IOException e) {
            log.warn("删除本地文件失败: {}", e.getMessage());
        }

        // 4. 删除数据库记录
        documentMapper.deleteById(docId);

        // 5. 清除Redis缓存
        redisUtil.delete("doc:info:" + docId);

        log.info("文档删除完成 - ID: {}", docId);
    }

    /**
     * 从Qdrant中删除文档相关的向量数据
     */
    private void deleteFromVectorStore(Long docId) throws Exception {
        String docIdStr = docId.toString();

        // 构建过滤条件：doc_id 等于指定值
        var condition = Points.Condition.newBuilder()
                .setField(Points.FieldCondition.newBuilder()
                        .setKey("doc_id")
                        .setMatch(Points.Match.newBuilder().setText(docIdStr).build())
                        .build())
                .build();

        var filter = Points.Filter.newBuilder()
                .addMust(condition)
                .build();

        // 先查询有多少条记录
        var scrollRequest = Points.ScrollPoints.newBuilder()
                .setCollectionName(collectionName)
                .setFilter(filter)
                .setLimit(1000)
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(false).build())
                .build();

        var scrollResponse = qdrantClient.scrollAsync(scrollRequest).get();
        List<Points.RetrievedPoint> points = scrollResponse.getResultList();

        if (points.isEmpty()) {
            log.info("Qdrant中没有找到文档 {} 的向量数据", docId);
            return;
        }

        // 提取所有点的ID
        List<Points.PointId> pointIds = points.stream()
                .map(Points.RetrievedPoint::getId)
                .toList();

        // 批量删除
        var deleteRequest = Points.DeletePoints.newBuilder()
                .setCollectionName(collectionName)
                .setPoints(Points.PointsSelector.newBuilder()
                        .setPoints(Points.PointsIdsList.newBuilder().addAllIds(pointIds).build())
                        .build())
                .build();

        qdrantClient.deleteAsync(deleteRequest).get();
        log.info("已从Qdrant删除文档 {} 的 {} 条向量数据", docId, pointIds.size());
    }
}