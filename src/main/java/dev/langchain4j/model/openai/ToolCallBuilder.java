package dev.langchain4j.model.openai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.CompleteToolCall;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具调用构建器
 * 从 LangChain4j main 分支提取，用于支持流式工具调用
 */
public class ToolCallBuilder {

    private int index = -1;
    private String id;
    private String name;
    private final StringBuilder argumentsBuilder = new StringBuilder();
    private final List<ToolExecutionRequest> allRequests = new ArrayList<>();

    public ToolCallBuilder() {
    }

    public ToolCallBuilder(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public void updateIndex(int index) {
        this.index = index;
    }

    public String updateId(String id) {
        if (id != null) {
            this.id = id;
        }
        return this.id;
    }

    public String updateName(String name) {
        if (name != null) {
            this.name = name;
        }
        return this.name;
    }

    public void appendArguments(String arguments) {
        if (arguments != null) {
            argumentsBuilder.append(arguments);
        }
    }

    public boolean hasRequests() {
        return index >= 0 && id != null;
    }

    public CompleteToolCall buildAndReset() {
        if (index < 0 || id == null) {
            return null;
        }

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id(id)
                .name(name)
                .arguments(argumentsBuilder.toString())
                .build();

        allRequests.add(request);

        // 1.9.1 中 CompleteToolCall 使用构造函数而非 builder
        CompleteToolCall completeToolCall = new CompleteToolCall(index, request);

        // Reset for next tool call
        id = null;
        name = null;
        argumentsBuilder.setLength(0);

        return completeToolCall;
    }

    public List<ToolExecutionRequest> allRequests() {
        return allRequests;
    }
}
