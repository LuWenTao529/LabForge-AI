package com.winter.labforgeai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 配置类，用于配置路由AI模型的属性和创建ChatModel的Bean
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingAiModelConfig {

    // AI模型的基础URL配置
    private String baseUrl;

    // API密钥配置
    private String apiKey;

    // 模型名称配置
    private String modelName;

    // 最大令牌数配置
    private Integer maxTokens;

    // 温度参数，控制生成文本的随机性
    private Double temperature;

    // 是否记录请求日志，默认为false
    private Boolean logRequests = false;

    // 是否记录响应日志，默认为false
    private Boolean logResponses = false;


    /**
     * 创建一个原型(prototype)作用域的ChatModel Bean
     * 使用@Bean注解标记为Bean方法
     * 使用@Scope("prototype")注解指定Bean的作用域为原型，每次请求都会创建新的实例
     *
     * @return 配置好的OpenAiChatModel实例
     */
    @Bean
    @Scope("prototype")
    public ChatModel routingChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)              // 设置API密钥
                .modelName(modelName)        // 设置模型名称
                .baseUrl(baseUrl)            // 设置基础URL
                .maxTokens(maxTokens)        // 设置最大令牌数
                .temperature(temperature)    // 设置温度参数
                .logRequests(logRequests)    // 设置是否记录请求日志
                .logResponses(logResponses)  // 设置是否记录响应日志
                .build();                    // 构建并返回ChatModel实例
    }
}
