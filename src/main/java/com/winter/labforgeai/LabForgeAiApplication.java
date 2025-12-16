package com.winter.labforgeai;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.winter.labforgeai.mapper")
public class LabForgeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabForgeAiApplication.class, args);
    }

}
