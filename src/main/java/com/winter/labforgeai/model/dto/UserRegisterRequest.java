package com.winter.labforgeai.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求类
 * 用于封装用户注册时提交的数据信息
 * 实现Serializable接口以支持序列化操作
 */
@Data  // 使用Lombok注解自动生成getter、setter等方法
public class UserRegisterRequest implements Serializable {

    // 序列化版本UID，用于在反序列化时验证版本一致性
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
