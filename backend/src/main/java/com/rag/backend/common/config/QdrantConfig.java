package com.rag.backend.common.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:rag_collection}")
    private String collectionName;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false);
        return new QdrantClient(builder.build());
    }

    @PostConstruct
    public void initCollection() {
        try {
            // 创建新的客户端实例用于初始化（避免循环依赖）
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false);
            QdrantClient client = new QdrantClient(builder.build());

            // 检查集合是否存在
            boolean exists = client.collectionExistsAsync(collectionName).get(5, TimeUnit.SECONDS);

            if (!exists) {
                log.info("Qdrant集合 '{}' 不存在，正在创建...", collectionName);

                // 创建集合，向量维度为1024（阿里云 text-embedding-v4 的维度）
                client.createCollectionAsync(collectionName,
                        Collections.VectorParams.newBuilder()
                                .setDistance(Collections.Distance.Cosine)
                                .setSize(1024)
                                .build()
                ).get(10, TimeUnit.SECONDS);

                log.info("Qdrant集合 '{}' 创建成功", collectionName);
            } else {
                log.info("Qdrant集合 '{}' 已存在", collectionName);
            }

            // 关闭临时客户端
            client.close();
        } catch (Exception e) {
            log.error("初始化Qdrant集合失败: {}", e.getMessage(), e);
        }
    }
}
