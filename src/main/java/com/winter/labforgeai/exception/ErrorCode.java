package com.winter.labforgeai.exception;

import lombok.Getter;

@Getter // 使用Lombok库的@Getter注解，自动为所有字段生成getter方法
public enum ErrorCode { // 定义一个名为ErrorCode的枚举类，用于表示各种错误码



    // 枚举实例定义，每个实例包含一个错误码和对应的错误信息
    SUCCESS(0, "ok"), // 成功状态码
    PARAMS_ERROR(40000, "请求参数错误"), // 参数错误状态码
    NOT_LOGIN_ERROR(40100, "未登录"), // 未登录错误状态码
    NO_AUTH_ERROR(40101, "无权限"), // 无权限错误状态码
    NOT_FOUND_ERROR(40400, "请求数据不存在"), // 数据不存在错误状态码
    FORBIDDEN_ERROR(40300, "禁止访问"), // 禁止访问错误状态码
    SYSTEM_ERROR(50000, "系统内部异常"), // 系统内部错误状态码
    OPERATION_ERROR(50001, "操作失败"); // 操作失败错误状态码

    /**
     * 状态码
     * 用于存储错误码的整数值
     */
    private final int code; // 定义一个final类型的整型字段，用于存储错误码

    /**
     * 信息
     * 用于存储错误信息的字符串
     */
    private final String message; // 定义一个final类型的字符串字段，用于存储错误信息



    /**
     * 枚举类的构造方法
     * @param code 错误码
     * @param message 错误信息
     */
    ErrorCode(int code, String message) { // 枚举类的构造方法，用于初始化错误码和错误信息
        this.code = code; // 将传入的错误码赋值给类的code字段
        this.message = message; // 将传入的错误信息赋值给类的message字段
    }

}
