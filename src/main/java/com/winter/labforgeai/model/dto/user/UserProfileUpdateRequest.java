package com.winter.labforgeai.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户个人信息更新请求（普通用户可用）
 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    @Serial
    private static final long serialVersionUID = 1L;
}
