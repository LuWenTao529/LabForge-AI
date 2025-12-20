package com.winter.labforgeai.langgraph4j.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageCollectionPlan implements Serializable {
    
    /**
     * 内容图片搜索任务列表
     * 用于存储需要搜索内容图片的任务
     */
    private List<ImageSearchTask> contentImageTasks;  // 内容图片搜索任务列表，存储内容图片搜索相关任务
    
    /**
     * 插画图片搜索任务列表
     * 用于存储需要搜索插画图片的任务
     */
    private List<IllustrationTask> illustrationTasks;  // 插画图片搜索任务列表，存储插画图片搜索相关任务
    
    /**
     * 架构图生成任务列表
     * 用于存储需要生成架构图的任务
     */
    private List<DiagramTask> diagramTasks;  // 架构图生成任务列表，存储架构图生成相关任务
    
    /**
     * Logo生成任务列表
     * 用于存储需要生成Logo的任务
     */
    private List<LogoTask> logoTasks;  // Logo生成任务列表，存储Logo生成相关任务
    
    /**
     * 内容图片搜索任务
     * 对应 ImageSearchTool.searchContentImages(String query)方法
     * 用于封装内容图片搜索的查询参数
     */
    public record ImageSearchTask(String query) implements Serializable {}  // 内容图片搜索任务记录，封装查询参数
    
    /**
     * 插画图片搜索任务
     * 对应 UndrawIllustrationTool.searchIllustrations(String query)方法
     * 用于封装插画图片搜索的查询参数
     */
    public record IllustrationTask(String query) implements Serializable {}  // 插画图片搜索任务记录，封装查询参数
    
    /**
     * 架构图生成任务
     * 对应 MermaidDiagramTool.generateMermaidDiagram(String mermaidCode, String description)方法
     * 用于封装架构图生成的参数，包括mermaid代码和描述信息
     */
    public record DiagramTask(String mermaidCode, String description) implements Serializable {}  // 架构图生成任务记录，封装mermaid代码和描述信息
    
    /**
     * Logo生成任务
     * 对应 LogoGeneratorTool.generateLogos(String description)方法
     * 用于封装Logo生成的描述信息
     */
    public record LogoTask(String description) implements Serializable {}  // Logo生成任务记录，封装描述信息
}
