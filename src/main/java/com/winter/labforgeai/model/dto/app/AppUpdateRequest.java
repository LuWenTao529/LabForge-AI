package com.winter.labforgeai.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员更新应用请求
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    @Serial
    private static final long serialVersionUID = 1L;
}

