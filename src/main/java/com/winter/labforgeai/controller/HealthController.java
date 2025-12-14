package com.winter.labforgeai.controller;

import com.winter.labforgeai.common.BaseResponse;
import com.winter.labforgeai.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康控制器类
 * 该类用于处理与健康相关的请求和业务逻辑
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    // 健康控制器类的主体

    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
