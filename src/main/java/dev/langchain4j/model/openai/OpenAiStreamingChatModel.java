package dev.langchain4j.model.openai;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import dev.langchain4j.model.openai.internal.OpenAiUtils;
import dev.langchain4j.model.openai.internal.ParsedAndRawResponse;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionChoice;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionRequest;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionResponse;
import dev.langchain4j.model.openai.internal.chat.Delta;
import dev.langchain4j.model.openai.internal.chat.ToolCall;
import dev.langchain4j.model.openai.internal.shared.StreamOptions;
import dev.langchain4j.model.openai.spi.OpenAiStreamingChatModelBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.InternalStreamingChatResponseHandlerUtils.*;
import static dev.langchain4j.internal.Utils.copyIfNotNull;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNotNullOrEmpty;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.model.ModelProvider.OPEN_AI;
import static dev.langchain4j.model.openai.internal.OpenAiUtils.*;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

/**
 * 覆盖的 OpenAiStreamingChatModel，添加了 onPartialToolCall 支持
 * 从 LangChain4j main 分支回移，解决 1.9.1 版本不支持流式工具调用的问题
 */
public class OpenAiStreamingChatModel implements StreamingChatModel {

    private static final Logger log = LoggerFactory.getLogger(OpenAiStreamingChatModel.class);

    private final OpenAiClient client;
    private final OpenAiChatRequestParameters defaultRequestParameters;
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final Boolean toolStream;  // 新增：GLM 等模型的流式工具调用参数
    private final List<ChatModelListener> listeners;

    public OpenAiStreamingChatModel(OpenAiStreamingChatModelBuilder builder) {
        this.client = OpenAiClient.builder()
                .httpClientBuilder(builder.httpClientBuilder)
                .baseUrl(getOrDefault(builder.baseUrl, DEFAULT_OPENAI_URL))
                .apiKey(builder.apiKey)
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .logRequests(getOrDefault(builder.logRequests, false))
                .logResponses(getOrDefault(builder.logResponses, false))
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();

        ChatRequestParameters commonParameters;
        if (builder.defaultRequestParameters != null) {
            commonParameters = builder.defaultRequestParameters;
        } else {
            commonParameters = DefaultChatRequestParameters.builder().build();
        }

        OpenAiChatRequestParameters openAiParameters;
        if (builder.defaultRequestParameters instanceof OpenAiChatRequestParameters openAiChatRequestParameters) {
            openAiParameters = openAiChatRequestParameters;
        } else {
            openAiParameters = OpenAiChatRequestParameters.builder().build();
        }

        this.defaultRequestParameters = OpenAiChatRequestParameters.builder()
                // common parameters
                .modelName(getOrDefault(builder.modelName, commonParameters.modelName()))
                .temperature(getOrDefault(builder.temperature, commonParameters.temperature()))
                .topP(getOrDefault(builder.topP, commonParameters.topP()))
                .frequencyPenalty(getOrDefault(builder.frequencyPenalty, commonParameters.frequencyPenalty()))
                .presencePenalty(getOrDefault(builder.presencePenalty, commonParameters.presencePenalty()))
                .maxOutputTokens(getOrDefault(builder.maxTokens, commonParameters.maxOutputTokens()))
                .stopSequences(getOrDefault(builder.stop, () -> copyIfNotNull(commonParameters.stopSequences())))
                .toolSpecifications(copyIfNotNull(commonParameters.toolSpecifications()))
                .toolChoice(commonParameters.toolChoice())
                .responseFormat(getOrDefault(fromOpenAiResponseFormat(builder.responseFormat), commonParameters.responseFormat()))
                // OpenAI-specific parameters
                .maxCompletionTokens(getOrDefault(builder.maxCompletionTokens, openAiParameters.maxCompletionTokens()))
                .logitBias(getOrDefault(builder.logitBias, () -> copyIfNotNull(openAiParameters.logitBias())))
                .parallelToolCalls(getOrDefault(builder.parallelToolCalls, openAiParameters.parallelToolCalls()))
                .seed(getOrDefault(builder.seed, openAiParameters.seed()))
                .user(getOrDefault(builder.user, openAiParameters.user()))
                .store(getOrDefault(builder.store, openAiParameters.store()))
                .metadata(getOrDefault(builder.metadata, () -> copyIfNotNull(openAiParameters.metadata())))
                .serviceTier(getOrDefault(builder.serviceTier, openAiParameters.serviceTier()))
                .reasoningEffort(openAiParameters.reasoningEffort())
                .build();
        this.strictJsonSchema = getOrDefault(builder.strictJsonSchema, false);
        this.strictTools = getOrDefault(builder.strictTools, false);
        this.toolStream = getOrDefault(builder.toolStream, false);  // 新增：默认 false

        this.listeners = builder.listeners == null ? emptyList() : new ArrayList<>(builder.listeners);
    }

    @Override
    public OpenAiChatRequestParameters defaultRequestParameters() {
        return defaultRequestParameters;
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) chatRequest.parameters();
        OpenAiUtils.validate(parameters);

        // 构建自定义参数，合并已有的 customParameters 和 toolStream 设置
        Map<String, Object> customParams = new HashMap<>();
        // 先添加已有的 customParameters（来自配置文件）
        if (parameters.customParameters() != null) {
            customParams.putAll(parameters.customParameters());
        }
        // 如果启用了 toolStream 则添加 tool_stream=true
        if (Boolean.TRUE.equals(toolStream)) {
            customParams.put("tool_stream", true);
        }

        ChatCompletionRequest openAiRequest =
                toOpenAiChatRequest(chatRequest, parameters, strictTools, strictJsonSchema)
                        .stream(true)
                        .streamOptions(StreamOptions.builder()
                                .includeUsage(true)
                                .build())
                        .customParameters(customParams.isEmpty() ? null : customParams)
                        .build();

        OpenAiStreamingResponseBuilder openAiResponseBuilder = new OpenAiStreamingResponseBuilder();
        // 新增：工具调用构建器
        ToolCallBuilder toolCallBuilder = new ToolCallBuilder();

        client.chatCompletion(openAiRequest)
                // 修改：使用 onRawPartialResponse 以获取 StreamingHandle
                .onRawPartialResponse(parsedAndRawResponse -> {
                    openAiResponseBuilder.append(parsedAndRawResponse);
                    // 修改：调用新的 handle 方法，支持工具调用流式输出
                    handle(parsedAndRawResponse, toolCallBuilder, handler);
                })
                .onComplete(() -> {
                    // 新增：完成时处理未完成的工具调用
                    if (toolCallBuilder.hasRequests()) {
                        onCompleteToolCall(handler, toolCallBuilder.buildAndReset());
                    }
                    ChatResponse chatResponse = openAiResponseBuilder.build();
                    handler.onCompleteResponse(chatResponse);
                })
                .onError(handler::onError)
                .execute();
    }

    /**
     * 处理流式响应，包括文本内容和工具调用
     * 从 main 分支回移的核心方法
     */
    private void handle(
            ParsedAndRawResponse<ChatCompletionResponse> parsedAndRawResponse,
            ToolCallBuilder toolCallBuilder,
            StreamingChatResponseHandler handler) {
        ChatCompletionResponse partialResponse = parsedAndRawResponse.parsedResponse();
        if (partialResponse == null) {
            return;
        }

        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (isNullOrEmpty(choices)) {
            return;
        }

        ChatCompletionChoice chatCompletionChoice = choices.get(0);
        if (chatCompletionChoice == null) {
            return;
        }

        Delta delta = chatCompletionChoice.delta();
        if (delta == null) {
            return;
        }

        // 处理文本内容
        String content = delta.content();
        if (!isNullOrEmpty(content)) {
            onPartialResponse(handler, content, parsedAndRawResponse.streamingHandle());
        }

        // 新增：处理工具调用
        List<ToolCall> toolCalls = delta.toolCalls();
        if (toolCalls != null) {
            log.debug("[OpenAiStreamingChatModel] 收到 toolCalls, 数量: {}", toolCalls.size());
            for (ToolCall toolCall : toolCalls) {
                int index = toolCall.index() != null ? toolCall.index() : 0;
                
                // 如果是新的工具调用索引，完成前一个
                if (toolCallBuilder.index() != index) {
                    if (toolCallBuilder.hasRequests()) {
                        onCompleteToolCall(handler, toolCallBuilder.buildAndReset());
                    }
                    toolCallBuilder.updateIndex(index);
                }

                String id = toolCallBuilder.updateId(toolCall.id());
                String name = toolCallBuilder.updateName(
                        toolCall.function() != null ? toolCall.function().name() : null);

                String partialArguments = toolCall.function() != null 
                        ? toolCall.function().arguments() : null;
                        
                if (isNotNullOrEmpty(partialArguments)) {
                    toolCallBuilder.appendArguments(partialArguments);

                    // 构建 PartialToolCall 并调用回调
                    PartialToolCall partialToolRequest = PartialToolCall.builder()
                            .index(index)
                            .id(id)
                            .name(name)
                            .partialArguments(partialArguments)
                            .build();
                    log.debug("[OpenAiStreamingChatModel] 调用 onPartialToolCall: id={}, name={}, args={}", 
                            id, name, partialArguments.length() > 50 ? partialArguments.substring(0, 50) + "..." : partialArguments);
                    onPartialToolCall(handler, partialToolRequest, parsedAndRawResponse.streamingHandle());
                }
            }
        }
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public ModelProvider provider() {
        return OPEN_AI;
    }

    public static OpenAiStreamingChatModelBuilder builder() {
        for (OpenAiStreamingChatModelBuilderFactory factory :
                loadFactories(OpenAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingChatModelBuilder();
    }

    public static class OpenAiStreamingChatModelBuilder {

        HttpClientBuilder httpClientBuilder;
        String baseUrl;
        String apiKey;
        String organizationId;
        String projectId;

        ChatRequestParameters defaultRequestParameters;
        String modelName;
        Double temperature;
        Double topP;
        List<String> stop;
        Integer maxTokens;
        Integer maxCompletionTokens;
        Double presencePenalty;
        Double frequencyPenalty;
        Map<String, Integer> logitBias;
        String responseFormat;
        Boolean strictJsonSchema;
        Integer seed;
        String user;
        Boolean strictTools;
        Boolean parallelToolCalls;
        Boolean store;
        Boolean toolStream;  // 新增：GLM 等模型的流式工具调用
        Map<String, String> metadata;
        String serviceTier;
        String reasoningEffort;
        Boolean returnThinking;
        Boolean sendThinking;
        String thinkingFieldName;
        Duration timeout;
        Boolean logRequests;
        Boolean logResponses;
        Map<String, String> customHeaders;
        List<ChatModelListener> listeners;

        public OpenAiStreamingChatModelBuilder() {
        }

        public OpenAiStreamingChatModelBuilder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        public OpenAiStreamingChatModelBuilder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(OpenAiChatModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiStreamingChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiStreamingChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiStreamingChatModelBuilder store(Boolean store) {
            this.store = store;
            return this;
        }

        /**
         * 启用流式工具调用输出（GLM-4.7/GLM-4.6 等模型需要）
         * 设置为 true 时，会在请求中添加 tool_stream=true 参数
         */
        public OpenAiStreamingChatModelBuilder toolStream(Boolean toolStream) {
            this.toolStream = toolStream;
            return this;
        }

        public OpenAiStreamingChatModelBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OpenAiStreamingChatModelBuilder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public OpenAiStreamingChatModelBuilder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public OpenAiStreamingChatModelBuilder returnThinking(Boolean returnThinking) {
            this.returnThinking = returnThinking;
            return this;
        }

        public OpenAiStreamingChatModelBuilder sendThinking(Boolean sendThinking) {
            this.sendThinking = sendThinking;
            return this;
        }

        public OpenAiStreamingChatModelBuilder sendThinking(Boolean sendThinking, String fieldName) {
            this.sendThinking = sendThinking;
            this.thinkingFieldName = fieldName;
            return this;
        }

        public OpenAiStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            return new OpenAiStreamingChatModel(this);
        }
    }
}
