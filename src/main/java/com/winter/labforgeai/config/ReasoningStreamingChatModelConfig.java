package com.winter.labforgeai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 配置类，用于配置LangChain4j中的OpenAI推理流式聊天模型
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    // OpenAI API的基础URL
    private String baseUrl;

    // OpenAI API的密钥
    private String apiKey;

    // 使用的模型名称
    private String modelName;

    // 生成的最大令牌数
    private Integer maxTokens;

    // 控制生成内容的随机性，值越大随机性越高
    private Double temperature;

    // 是否记录请求日志，默认为false
    private Boolean logRequests = false;

    // 是否记录响应日志，默认为false
    private Boolean logResponses = false;

    /**
     * 创建一个原型作用域的StreamingChatModel Bean
     * 该方法返回一个配置好的OpenAI流式聊天模型实例
     *
     * @return 配置好的OpenAiStreamingChatModel实例
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
