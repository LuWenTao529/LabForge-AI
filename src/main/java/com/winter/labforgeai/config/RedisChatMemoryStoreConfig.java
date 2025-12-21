package com.winter.labforgeai.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis聊天记忆存储配置类
 * 用于配置Redis连接参数并创建RedisChatMemoryStore Bean
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    // Redis服务器主机地址
    private String host;

    // Redis服务器端口号
    private int port;

    // Redis访问密码
    private String password;

    // 数据过期时间，单位为毫秒
    private long ttl;

    /**
     * 创建并配置RedisChatMemoryStore Bean
     * @return 配置好的RedisChatMemoryStore实例
     */
    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
         RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                // todo 默认使用 RedisJSON 但是需要特定Redis版本支持，暂时修改为String
                .storeType(StoreType.STRING)
                .ttl(ttl);
         if(StrUtil.isNotBlank(password)){
             builder.user("default");
         }
        return builder.build();
    }
}
