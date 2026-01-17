package com.winter.labforgeai.ai.message;

import dev.langchain4j.model.chat.response.PartialToolCall;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用参数流式片段消息
 * 每次回调只包含参数的增量片段，前端需要累加拼接
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PartialToolCallMessage extends StreamMessage {

    /**
     * 工具调用ID
     */
    private String id;

    /**
     * 工具调用索引（多工具调用场景）
     */
    private Integer index;

    /**
     * 工具名称（通常只在首次回调时有值）
     */
    private String name;

    /**
     * 工具参数的增量片段（不是完整JSON，需要前端累加）
     */
    private String arguments;

    public PartialToolCallMessage(PartialToolCall partialToolCall) {
        super(StreamMessageTypeEnum.PARTIAL_TOOL_CALL.getValue());
        this.id = partialToolCall.id();
        this.index = partialToolCall.index();
        this.name = partialToolCall.name();
        this.arguments = partialToolCall.partialArguments();
    }
}
