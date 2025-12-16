package com.winter.labforgeai.core.saver;

import com.winter.labforgeai.ai.model.HtmlCodeResult;
import com.winter.labforgeai.ai.model.MultiFileCodeResult;
import com.winter.labforgeai.exception.BusinessException;
import com.winter.labforgeai.exception.ErrorCode;
import com.winter.labforgeai.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();

    /**
     * 根据代码生成类型执行相应的代码保存操作(appId)
     *
     * @param codeResult 代码结果对象
     * @param codeGenType 代码生成类型枚举
     * @param appId 应用程序ID
     * @return File 返回保存后的文件对象
     * @throws BusinessException 当遇到不支持的代码生成类型时抛出业务异常
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
    // 使用switch表达式根据不同的代码生成类型执行相应的保存逻辑
        return switch (codeGenType) {
        // 处理HTML类型的代码生成结果
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
        // 处理多文件类型的代码生成结果
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
        // 默认情况，抛出不支持代码生成类型的业务异常
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}
