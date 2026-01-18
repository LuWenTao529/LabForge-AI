package com.winter.labforgeai.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.winter.labforgeai.ai.message.*;
import com.winter.labforgeai.ai.tools.BaseTool;
import com.winter.labforgeai.ai.tools.ToolManager;
import com.winter.labforgeai.model.entity.User;
import com.winter.labforgeai.model.enums.ChatHistoryMessageTypeEnum;
import com.winter.labforgeai.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Autowired
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        // 用于追踪每个工具调用的流式解析状态
        Map<String, ToolContentParseState> toolParseStates = new HashMap<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds, toolParseStates);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
//                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
//                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, 
                                          Set<String> seenToolIds, Map<String, ToolContentParseState> toolParseStates) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.error("不支持的消息类型: {}, 原始消息: {}", streamMessage.getType(), chunk);
            return "";
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case PARTIAL_TOOL_CALL -> {
                // 处理流式工具调用参数片段
                PartialToolCallMessage partialToolCallMessage = JSONUtil.toBean(chunk, PartialToolCallMessage.class);
                String toolId = partialToolCallMessage.getId();
                String toolName = partialToolCallMessage.getName();
                
                // 检查是否是第一次看到这个工具 ID（首次会携带 name）
                if (toolId != null && !seenToolIds.contains(toolId) && StrUtil.isNotBlank(toolName)) {
                    // 第一次调用这个工具，记录 ID
                    seenToolIds.add(toolId);
                    // 初始化该工具的解析状态，记录工具信息
                    ToolContentParseState state = new ToolContentParseState();
                    state.toolName = toolName;
                    BaseTool tool = toolManager.getTool(toolName);
                    if (tool != null) {
                        state.displayName = tool.getDisplayName();
                        state.isStreamingTool = true;
                    } else {
                        state.displayName = toolName;
                        log.warn("未找到工具实例, toolName={}, toolId={}", toolName, toolId);
                    }
                    toolParseStates.put(toolId, state);
                    // 注意：不要直接返回，第一个 chunk 可能同时包含 arguments，需要继续处理
                }
                
                // 流式提取 content 字段内容
                String arguments = partialToolCallMessage.getArguments();
                if (StrUtil.isNotBlank(arguments) && toolId != null) {
                    String output = extractContentFromStream(toolId, arguments, toolParseStates);
                    // 流式内容也要保存到聊天历史，否则结束后重新加载会丢失
                    if (StrUtil.isNotBlank(output)) {
                        chatHistoryStringBuilder.append(output);
                    }
                    return output;
                }
                return "";
            }
            case TOOL_REQUEST -> {
                // 保留兼容：如果有其他地方发送 TOOL_REQUEST 消息
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    BaseTool tool = toolManager.getTool(toolName);
                    if (tool == null) {
                        log.warn("未找到工具实例, toolName={}, toolId={}, chunk={}", toolName, toolId, chunk);
                        return String.format("\n\n[选择工具] %s\n\n", StrUtil.blankToDefault(toolName, "unknown"));
                    }
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                String toolId = toolExecutedMessage.getId();
                
                // 获取并清理该工具的解析状态
                ToolContentParseState state = toolParseStates.remove(toolId);
                
                // 检查是否是流式工具且已经输出了markdown
                if (state != null && state.isStreamingTool && state.markdownStarted) {
                    // 流式工具：只关闭markdown代码块，不重复输出内容
                    String output = "\n```\n\n";
                    chatHistoryStringBuilder.append(output);
                    return output;
                }
                
                // 非流式工具：输出完整内容
                JSONObject jsonObject = StrUtil.isBlank(toolExecutedMessage.getArguments())
                        ? new JSONObject()
                        : JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 根据工具名称获取工具实例并生成相应的结果格式
                BaseTool tool = toolManager.getTool(toolName);
                String result;
                if (tool == null) {
                    log.warn("未找到工具实例, toolName={}, toolId={}, chunk={}", toolName, toolExecutedMessage.getId(), chunk);
                    result = String.format("[工具调用] %s", StrUtil.blankToDefault(toolName, "unknown"));
                } else {
                    result = tool.generateToolExecutedResult(jsonObject);
                }
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    /**
     * 从流式 JSON 参数中提取字段内容
     * 支持 writeFile (relativeFilePath + content) 和 modifyFile (relativeFilePath + oldContent + newContent)
     * 使用状态机追踪解析进度，流式输出内容字段的值
     */
    private String extractContentFromStream(String toolId, String chunk, Map<String, ToolContentParseState> toolParseStates) {
        ToolContentParseState state = toolParseStates.get(toolId);
        if (state == null) {
            state = new ToolContentParseState();
            toolParseStates.put(toolId, state);
        }
        
        StringBuilder output = new StringBuilder();
        
        for (char c : chunk.toCharArray()) {
            state.buffer.append(c);
            
            switch (state.phase) {
                case SCANNING_KEY -> {
                    // 扫描JSON key
                    String bufStr = state.buffer.toString();
                    
                    // 检测 relativeFilePath
                    if (bufStr.endsWith("\"relativeFilePath\"")) {
                        state.currentKey = "relativeFilePath";
                        state.phase = ParsePhase.WAITING_FOR_COLON;
                        state.buffer.setLength(0);
                    }
                    // 检测 content (writeFile)
                    else if (bufStr.endsWith("\"content\"")) {
                        state.currentKey = "content";
                        state.phase = ParsePhase.WAITING_FOR_COLON;
                        state.buffer.setLength(0);
                    }
                    // 检测 oldContent (modifyFile)
                    else if (bufStr.endsWith("\"oldContent\"")) {
                        state.currentKey = "oldContent";
                        state.phase = ParsePhase.WAITING_FOR_COLON;
                        state.buffer.setLength(0);
                    }
                    // 检测 newContent (modifyFile)
                    else if (bufStr.endsWith("\"newContent\"")) {
                        state.currentKey = "newContent";
                        state.phase = ParsePhase.WAITING_FOR_COLON;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_COLON -> {
                    if (c == ':') {
                        state.phase = ParsePhase.WAITING_FOR_VALUE_START;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_VALUE_START -> {
                    if (c == '"') {
                        state.phase = ParsePhase.IN_VALUE;
                        state.buffer.setLength(0);
                        state.valueBuilder.setLength(0);
                        // 如果是内容字段，输出 markdown 头部
                        if (isContentKey(state.currentKey)) {
                            output.append(prepareMarkdownHeader(state, state.currentKey));
                        }
                    }
                }
                case IN_VALUE -> {
                    if (state.escapeNext) {
                        char unescaped = unescapeChar(c);
                        state.valueBuilder.append(unescaped);
                        // 对于内容字段，流式输出
                        if (isContentKey(state.currentKey) && state.markdownStarted) {
                            output.append(unescaped);
                        }
                        state.escapeNext = false;
                    } else if (c == '\\') {
                        state.escapeNext = true;
                    } else if (c == '"') {
                        // 值结束，处理该字段
                        String value = state.valueBuilder.toString();
                        output.append(handleFieldComplete(state, value));
                        state.phase = ParsePhase.SCANNING_KEY;
                        state.buffer.setLength(0);
                    } else {
                        state.valueBuilder.append(c);
                        // 对于内容字段，流式输出
                        if (isContentKey(state.currentKey) && state.markdownStarted) {
                            output.append(c);
                        }
                    }
                }
            }
        }
        
        return output.toString();
    }
    
    /**
     * 判断是否是需要流式输出的内容字段
     */
    private boolean isContentKey(String key) {
        return "content".equals(key) || "oldContent".equals(key) || "newContent".equals(key);
    }
    
    /**
     * 处理字段解析完成
     */
    private String handleFieldComplete(ToolContentParseState state, String value) {
        StringBuilder output = new StringBuilder();
        
        switch (state.currentKey) {
            case "relativeFilePath" -> {
                state.relativeFilePath = value;
                state.fileSuffix = cn.hutool.core.io.FileUtil.getSuffix(value);
            }
            case "content" -> {
                // writeFile 的 content 字段结束，关闭 markdown
                // 注意：流式输出已在 IN_VALUE 阶段完成
            }
            case "oldContent" -> {
                // modifyFile 的 oldContent 字段结束
                // 关闭旧内容的 markdown，准备输出 newContent 的标题
                if (state.markdownStarted) {
                    output.append("\n```\n\n**新内容:**\n```").append(state.fileSuffix).append("\n");
                    state.contentBlockCount++;
                }
            }
            case "newContent" -> {
                // modifyFile 的 newContent 字段结束
                // 注意：markdown 关闭在 TOOL_EXECUTED 阶段处理
            }
        }
        
        // 在解析到文件路径后，且下一个是内容字段前，输出 markdown 头部
        // 这里需要在开始解析内容字段时输出头部
        state.currentKey = null;
        return output.toString();
    }
    
    /**
     * 为内容字段准备 markdown 头部
     */
    private String prepareMarkdownHeader(ToolContentParseState state, String contentKey) {
        if (state.relativeFilePath == null) {
            return "";
        }
        StringBuilder header = new StringBuilder();
        
        if ("content".equals(contentKey)) {
            // writeFile: [工具调用] 写入文件 xxx.vue\n```vue\n
            header.append(String.format("\n\n[工具调用] %s %s\n```%s\n",
                    state.displayName, state.relativeFilePath, state.fileSuffix));
            state.markdownStarted = true;
        } else if ("oldContent".equals(contentKey)) {
            // modifyFile: [工具调用] 修改文件 xxx.vue\n\n**旧内容:**\n```vue\n
            header.append(String.format("\n\n[工具调用] %s %s\n\n**旧内容:**\n```%s\n",
                    state.displayName, state.relativeFilePath, state.fileSuffix));
            state.markdownStarted = true;
            state.contentBlockCount = 1;
        } else if ("newContent".equals(contentKey) && state.contentBlockCount == 0) {
            // 如果 newContent 先出现（不太可能，但做兼容）
            header.append(String.format("\n\n[工具调用] %s %s\n\n**新内容:**\n```%s\n",
                    state.displayName, state.relativeFilePath, state.fileSuffix));
            state.markdownStarted = true;
            state.contentBlockCount = 2;
        }
        
        return header.toString();
    }
    
    /**
     * 处理 JSON 转义字符
     */
    private char unescapeChar(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> c;
        };
    }

    /**
     * 工具内容解析状态类
     */
    private static class ToolContentParseState {
        ParsePhase phase = ParsePhase.SCANNING_KEY;
        StringBuilder buffer = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();  // 当前字段值累积
        boolean escapeNext = false;
        
        // 当前正在解析的字段名
        String currentKey;
        
        // 工具信息
        String toolName;           // 工具名称
        String displayName;        // 工具显示名称
        
        // 解析出的字段值
        String relativeFilePath;   // 文件相对路径
        String fileSuffix;         // 文件后缀
        
        // 流式输出状态
        boolean markdownStarted = false;  // markdown代码块是否已开始
        boolean isStreamingTool = false;  // 是否是流式工具
        int contentBlockCount = 0;        // 已输出的内容块数量（modifyFile 有2个）
    }

    /**
     * 解析阶段枚举
     */
    private enum ParsePhase {
        SCANNING_KEY,           // 扫描JSON key
        WAITING_FOR_COLON,      // 等待冒号
        WAITING_FOR_VALUE_START,// 等待引号开始
        IN_VALUE                // 在值中
    }
}
