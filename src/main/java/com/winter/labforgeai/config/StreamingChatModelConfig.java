package com.winter.labforgeai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;


/**
 * 配置类：用于配置流式聊天模型的相关参数
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    // 基础URL配置
    private String baseUrl;

    // API密钥配置
    private String apiKey;

    // 模型名称配置
    private String modelName;

    // 最大令牌数配置
    private Integer maxTokens;

    // 温度参数配置，控制输出的随机性
    private Double temperature;

    // 是否记录请求日志的配置
    private boolean logRequests;

    // 是否记录响应日志的配置
    private boolean logResponses;

    /**
     * 创建并配置一个原型(prototype)作用域的StreamingChatModel Bean
     * 使用@Bean注解将该方法返回的对象注册为Spring Bean
     * 使用@Scope("prototype")注解指定该Bean为原型作用域，每次获取都会创建新实例
     *
     * @return 配置好的StreamingChatModel实例
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        // 使用建造者模式构建OpenAiStreamingChatModel实例
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)              // 设置API密钥
                .baseUrl(baseUrl)            // 设置基础URL
                .modelName(modelName)        // 设置模型名称
                .maxTokens(maxTokens)        // 设置最大令牌数
                .temperature(temperature)    // 设置温度参数
                .logRequests(logRequests)    // 设置是否记录请求日志
                .logResponses(logResponses)  // 设置是否记录响应日志
                .build();                    // 构建并返回实例
    }
}
