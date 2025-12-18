package com.winter.labforgeai.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理器
 * 统一管理所有工具，提供根据名称获取工具的功能
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * 工具名称到工具实例的映射
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * 自动注入所有工具
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 初始化工具映射
     */
    @PostConstruct
    public void initTools() {
        if (tools == null || tools.length == 0) {
            log.warn("未注入任何 BaseTool 实例，工具列表为空");
            return;
        }
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具英文名称
     * @return 工具实例
     */
    public BaseTool getTool(String toolName) {
        if (toolName == null) {
            return null;
        }
        BaseTool tool = toolMap.get(toolName);
        if (tool != null) {
            return tool;
        }
        String trimmed = toolName.trim();
        tool = toolMap.get(trimmed);
        if (tool != null) {
            return tool;
        }
        for (Map.Entry<String, BaseTool> entry : toolMap.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(trimmed)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 获取已注册的工具集合
     *
     * @return 工具实例集合
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}
