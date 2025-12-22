package com.winter.labforgeai.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户密码修改请求
 */
@Data
public class UserPasswordUpdateRequest implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String confirmPassword;

    @Serial
    private static final long serialVersionUID = 1L;
}
