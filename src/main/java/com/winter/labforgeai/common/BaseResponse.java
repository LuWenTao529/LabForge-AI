package com.winter.labforgeai.common;

import com.winter.labforgeai.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
/**
 * 通用基础响应类，用于封装API返回结果
 * @param <T> 泛型类型，表示返回数据的类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 全参数构造方法
     * @param code 响应状态码
     * @param data 响应数据
     * @param message 响应消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 省略message的构造方法
     * @param code 响应状态码
     * @param data 响应数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 使用ErrorCode枚举的构造方法
     * @param errorCode 错误码枚举
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
