package com.winter.labforgeai.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建应用请求
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt（必填）
     */
    private String initPrompt;

    @Serial
    private static final long serialVersionUID = 1L;
}
