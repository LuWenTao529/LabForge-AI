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
     * 从流式 JSON 参数中提取 relativeFilePath 和 content 字段内容
     * 使用状态机追踪解析进度，先解析文件路径，再流式输出 content 字段的值
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
                case WAITING_FOR_FILE_PATH_KEY -> {
                    // 等待 "relativeFilePath" 关键字
                    String bufStr = state.buffer.toString();
                    if (bufStr.endsWith("\"relativeFilePath\"") || bufStr.endsWith("\"relativeFilePath\" ")) {
                        state.phase = ParsePhase.WAITING_FOR_FILE_PATH_COLON;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_FILE_PATH_COLON -> {
                    // 等待冒号
                    if (c == ':') {
                        state.phase = ParsePhase.WAITING_FOR_FILE_PATH_START;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_FILE_PATH_START -> {
                    // 等待引号开始
                    if (c == '"') {
                        state.phase = ParsePhase.IN_FILE_PATH_VALUE;
                        state.buffer.setLength(0);
                    }
                }
                case IN_FILE_PATH_VALUE -> {
                    // 在文件路径值中
                    if (state.escapeNext) {
                        state.filePathBuilder.append(unescapeChar(c));
                        state.escapeNext = false;
                    } else if (c == '\\') {
                        state.escapeNext = true;
                    } else if (c == '"') {
                        // 文件路径结束，保存并切换到等待 content
                        state.relativeFilePath = state.filePathBuilder.toString();
                        state.fileSuffix = cn.hutool.core.io.FileUtil.getSuffix(state.relativeFilePath);
                        state.phase = ParsePhase.WAITING_FOR_CONTENT_KEY;
                        state.buffer.setLength(0);
                        // 输出 markdown 头部：[工具调用] 写入文件 xxx.js\n```js\n
                        state.markdownStarted = true;
                        output.append(String.format("\n\n[工具调用] %s %s\n```%s\n", 
                                state.displayName, state.relativeFilePath, state.fileSuffix));
                    } else {
                        state.filePathBuilder.append(c);
                    }
                }
                case WAITING_FOR_CONTENT_KEY -> {
                    // 等待 "content" 关键字
                    String bufStr = state.buffer.toString();
                    if (bufStr.endsWith("\"content\"") || bufStr.endsWith("\"content\" ")) {
                        state.phase = ParsePhase.WAITING_FOR_COLON;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_COLON -> {
                    // 等待冒号
                    if (c == ':') {
                        state.phase = ParsePhase.WAITING_FOR_VALUE_START;
                        state.buffer.setLength(0);
                    }
                }
                case WAITING_FOR_VALUE_START -> {
                    // 等待引号开始
                    if (c == '"') {
                        state.phase = ParsePhase.IN_CONTENT_VALUE;
                        state.buffer.setLength(0);
                    }
                }
                case IN_CONTENT_VALUE -> {
                    // 在 content 值中，处理转义和结束
                    if (state.escapeNext) {
                        // 处理转义字符
                        output.append(unescapeChar(c));
                        state.escapeNext = false;
                    } else if (c == '\\') {
                        // 下一个字符是转义
                        state.escapeNext = true;
                    } else if (c == '"') {
                        // content 值结束
                        state.phase = ParsePhase.CONTENT_DONE;
                    } else {
                        // 正常字符，直接输出
                        output.append(c);
                    }
                }
                case CONTENT_DONE -> {
                    // content 已结束，忽略后续内容
                }
            }
        }
        
        return output.toString();
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
        ParsePhase phase = ParsePhase.WAITING_FOR_FILE_PATH_KEY;
        StringBuilder buffer = new StringBuilder();
        StringBuilder filePathBuilder = new StringBuilder();  // 跨chunk累积文件路径
        boolean escapeNext = false;
        // 新增：追踪流式输出状态
        String toolName;           // 工具名称
        String displayName;        // 工具显示名称
        String relativeFilePath;   // 文件相对路径
        String fileSuffix;         // 文件后缀
        boolean markdownStarted = false;  // markdown代码块是否已开始
        boolean isStreamingTool = false;  // 是否是流式工具（有流式内容输出）
    }

    /**
     * 解析阶段枚举
     */
    private enum ParsePhase {
        WAITING_FOR_FILE_PATH_KEY,  // 等待 "relativeFilePath" 关键字
        WAITING_FOR_FILE_PATH_COLON, // 等待文件路径冒号
        WAITING_FOR_FILE_PATH_START, // 等待文件路径引号开始
        IN_FILE_PATH_VALUE,          // 在文件路径值中
        WAITING_FOR_CONTENT_KEY,     // 等待 "content" 关键字
        WAITING_FOR_COLON,           // 等待冒号
        WAITING_FOR_VALUE_START,     // 等待引号开始
        IN_CONTENT_VALUE,            // 在 content 值中
        CONTENT_DONE                 // content 解析完成
    }
}
