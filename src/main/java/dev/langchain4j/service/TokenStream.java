package dev.langchain4j.service;

import dev.langchain4j.Experimental;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Represents a token stream from the model to which you can subscribe and receive updates
 * when a new partial response (usually a single token) is available,
 * when the model finishes streaming, or when an error occurs during streaming.
 * It is intended to be used as a return type in AI Service.
 * <p>
 * 注意：此类是从 LangChain4j main 分支提取的，用于在 1.9.1 版本中支持 onPartialToolCall 功能。
 * 当升级到官方 1.11.0+ 版本后，应删除此文件。
 */
public interface TokenStream {

    /**
     * The provided consumer will be invoked every time a new partial textual response (usually a single token)
     * from a language model is available.
     *
     * @param partialResponseHandler lambda that will be invoked when a model generates a new partial textual response
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream onPartialResponse(Consumer<String> partialResponseHandler);

    /**
     * The provided consumer will be invoked every time a new partial textual response (usually a single token)
     * from a language model is available.
     *
     * @param handler lambda that will be invoked when a model generates a new partial textual response
     * @return token stream instance used to configure or start stream processing
     * @since 1.8.0
     */
    @Experimental
    default TokenStream onPartialResponseWithContext(BiConsumer<PartialResponse, PartialResponseContext> handler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked every time a new partial thinking/reasoning text (usually a single token)
     * from a language model is available.
     *
     * @param partialThinkingHandler lambda that will be invoked when a model generates a new partial thinking/reasoning text
     * @return token stream instance used to configure or start stream processing
     * @since 1.2.0
     */
    @Experimental
    default TokenStream onPartialThinking(Consumer<PartialThinking> partialThinkingHandler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked every time a new partial thinking/reasoning text (usually a single token)
     * from a language model is available.
     *
     * @param handler lambda that will be invoked when a model generates a new partial thinking/reasoning text
     * @return token stream instance used to configure or start stream processing
     * @since 1.8.0
     */
    @Experimental
    default TokenStream onPartialThinkingWithContext(BiConsumer<PartialThinking, PartialThinkingContext> handler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked every time a new partial tool call
     * (usually containing a single token of the tool's arguments) from a language model is available.
     * <p>
     * 注意：此方法是从 LangChain4j main 分支 (1.11.0) 提取的，用于支持工具调用参数的流式输出。
     *
     * @param partialToolCallHandler lambda that will be invoked when a model generates a new partial tool call
     * @return token stream instance used to configure or start stream processing
     * @since 1.11.0 (backported to 1.9.1)
     */
    @Experimental
    default TokenStream onPartialToolCall(Consumer<PartialToolCall> partialToolCallHandler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked every time a new partial tool call
     * (usually containing a single token of the tool's arguments) from a language model is available.
     *
     * @param handler lambda that will be invoked when a model generates a new partial tool call
     * @return token stream instance used to configure or start stream processing
     * @since 1.11.0 (backported to 1.9.1)
     */
    @Experimental
    default TokenStream onPartialToolCallWithContext(BiConsumer<PartialToolCall, PartialToolCallContext> handler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked if any {@link Content}s are retrieved using RetrievalAugmentor.
     *
     * @param contentHandler lambda that consumes all retrieved contents
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream onRetrieved(Consumer<List<Content>> contentHandler);

    /**
     * The provided consumer will be invoked when a language model finishes streaming the intermediate chat response.
     *
     * @param intermediateResponseHandler lambda that consumes intermediate chat responses
     * @return token stream instance used to configure or start stream processing
     * @since 1.2.0
     */
    default TokenStream onIntermediateResponse(Consumer<ChatResponse> intermediateResponseHandler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked right before a tool is executed.
     *
     * @param beforeToolExecutionHandler lambda that consumes {@link BeforeToolExecution}
     * @return token stream instance used to configure or start stream processing
     * @since 1.2.0
     */
    default TokenStream beforeToolExecution(Consumer<BeforeToolExecution> beforeToolExecutionHandler) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * The provided consumer will be invoked right after a tool is executed.
     *
     * @param toolExecuteHandler lambda that consumes {@link ToolExecution}
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream onToolExecuted(Consumer<ToolExecution> toolExecuteHandler);

    /**
     * The provided consumer will be invoked when a language model finishes streaming the final chat response.
     *
     * @param completionHandler lambda that will be invoked when a language model finishes streaming
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream onCompleteResponse(Consumer<ChatResponse> completionHandler);

    /**
     * The provided consumer will be invoked when an error occurs during streaming.
     *
     * @param errorHandler lambda that will be invoked when an error occurs
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream onError(Consumer<Throwable> errorHandler);

    /**
     * Ignores errors during streaming.
     *
     * @return token stream instance used to configure or start stream processing
     */
    TokenStream ignoreErrors();

    /**
     * Starts the stream processing.
     */
    void start();
}
