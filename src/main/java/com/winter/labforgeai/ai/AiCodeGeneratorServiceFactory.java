package com.winter.labforgeai.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 创建并配置AiCodeGeneratorService Bean的方法
     * 该方法使用AiServices构建器模式来创建AiCodeGeneratorService实例
     * 并配置了聊天模型和流式聊天模型两个关键组件
     *
     * @return 配置完成的AiCodeGeneratorService实例
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
    // 使用AiServices构建器创建AiCodeGeneratorService实例
    // 并设置chatModel和streamingChatModel两个必要属性
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)        // 设置普通聊天模型
                .streamingChatModel(streamingChatModel)  // 设置流式聊天模型
                .build();    // 完成构建并返回实例
    }
}
