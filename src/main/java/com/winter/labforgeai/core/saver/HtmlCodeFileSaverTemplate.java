package com.winter.labforgeai.core.saver;

import cn.hutool.core.util.StrUtil;
import com.winter.labforgeai.ai.model.HtmlCodeResult;
import com.winter.labforgeai.exception.BusinessException;
import com.winter.labforgeai.exception.ErrorCode;
import com.winter.labforgeai.model.enums.CodeGenTypeEnum;


/**
 * 用于保存 HTML 代码文件的模板实现类
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    /**
     * 获取代码类型
     * @return 返回 CodeGenTypeEnum 枚举值 HTML，表示处理的是 HTML 代码
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
