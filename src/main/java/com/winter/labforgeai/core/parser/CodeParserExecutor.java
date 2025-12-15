package com.winter.labforgeai.core.parser;

import com.winter.labforgeai.exception.BusinessException;
import com.winter.labforgeai.exception.ErrorCode;
import com.winter.labforgeai.model.enums.CodeGenTypeEnum;

/**
 * 代码解析执行器
 * 根据代码生成类型执行相应的解析逻辑
 *
 * @author winter
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 根据代码生成类型执行相应的代码解析器
     *
     * @param codeContent 待解析的代码内容
     * @param codeGenType 代码生成类型枚举
     * @return 解析后的对象结果
     * @throws BusinessException 当遇到不支持的代码生成类型时抛出业务异常
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenType) {
    // 使用switch表达式根据不同的代码生成类型调用相应的解析器
        return switch (codeGenType) {
        // 当类型为HTML时，调用HTML代码解析器进行解析
            case HTML -> htmlCodeParser.parseCode(codeContent);
        // 当类型为MULTI_FILE时，调用多文件代码解析器进行解析
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
        // 对于其他不支持的类型，抛出系统异常并提示不支持的代码生成类型
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}
