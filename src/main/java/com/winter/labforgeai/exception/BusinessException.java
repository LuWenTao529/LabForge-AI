package com.winter.labforgeai.exception;

import lombok.Getter;

/**
 * 自定义业务异常类，继承自RuntimeException
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码，用于标识具体的错误类型
     */
    private final int code;

    /**
     * 构造方法1：通过错误码和自定义消息创建异常对象
     * @param code 错误码
     * @param message 异常消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法2：通过预定义的错误枚举创建异常对象
     * @param errorCode 错误枚举，包含错误码和错误信息
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造方法3：通过预定义的错误枚举和自定义消息创建异常对象
     * @param errorCode 错误枚举，包含错误码
     * @param message 自定义异常消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
