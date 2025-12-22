package com.winter.labforgeai.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.winter.labforgeai.model.dto.user.UserQueryRequest;
import com.winter.labforgeai.model.entity.User;
import com.winter.labforgeai.model.vo.LoginUserVO;
import com.winter.labforgeai.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码字符串
     * @param userPassword 用户原始密码字符串
     * @return 加密处理后的密码字符串
     */
    String getEncryptPassword(String userPassword);


    /**
     * 根据用户信息获取登录用户视图对象
     * 该方法用于将用户实体对象转换为登录用户视图对象，通常用于前端展示
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return LoginUserVO 登录用户视图对象，包含前端需要的用户信息
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 用户登录接口方法
     *
     * @param userAccount 用户登录账号
     * @param userPassword 用户登录密码
     * @param request HTTP请求对象，用于获取请求相关信息
     * @return LoginUserVO 登录成功后的用户信息对象，包含用户的基本信息和登录状态等
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 根据HTTP请求获取登录用户信息
     * @param request HttpServletRequest对象，包含客户端请求信息
     * @return User 返回当前登录的用户对象，如果用户未登录则可能返回null
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 用户登出方法
     * @param request HttpServletRequest对象，用于获取用户会话信息
     * @return 登出操作是否成功执行
     *         true: 登出成功
     *         false: 登出失败
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 根据User对象获取对应的UserVO对象
     * UserVO是User对象的视图对象，用于在前端展示
     *
     * @param user 用户实体对象，包含用户的基本信息
     * @return 返回对应的UserVO视图对象，用于前端展示
     */
    UserVO getUserVO(User user);

    /**
     * 根据用户列表获取用户视图对象列表
     * 该方法用于将User实体列表转换为UserVO视图对象列表
     *
     * @param userList 用户实体列表，包含用户的基本信息
     * @return 返回UserVO视图对象列表，用于前端展示
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据用户查询请求条件构建查询包装器
     *
     * @param userQueryRequest 用户查询请求对象，包含查询条件
     * @return QueryWrapper 返回一个包含查询条件的QueryWrapper对象，用于数据库查询操作
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 更新用户个人信息
     *
     * @param userName    用户昵称
     * @param userAvatar  用户头像
     * @param userProfile 用户简介
     * @param loginUser   当前登录用户
     * @return 是否更新成功
     */
    boolean updateUserProfile(String userName, String userAvatar, String userProfile, User loginUser);

    /**
     * 修改用户密码
     *
     * @param oldPassword     旧密码
     * @param newPassword     新密码
     * @param confirmPassword 确认密码
     * @param loginUser       当前登录用户
     * @return 是否修改成功
     */
    boolean updateUserPassword(String oldPassword, String newPassword, String confirmPassword, User loginUser);
}
