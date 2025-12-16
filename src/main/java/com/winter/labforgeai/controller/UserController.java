package com.winter.labforgeai.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.winter.labforgeai.annotation.AuthCheck;
import com.winter.labforgeai.common.BaseResponse;
import com.winter.labforgeai.common.DeleteRequest;
import com.winter.labforgeai.common.ResultUtils;
import com.winter.labforgeai.constant.UserConstant;
import com.winter.labforgeai.exception.BusinessException;
import com.winter.labforgeai.exception.ErrorCode;
import com.winter.labforgeai.exception.ThrowUtils;
import com.winter.labforgeai.model.dto.user.*;
import com.winter.labforgeai.model.entity.User;
import com.winter.labforgeai.model.vo.LoginUserVO;
import com.winter.labforgeai.model.vo.UserVO;
import com.winter.labforgeai.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }


    /**
     * 用户登录接口
     * @param userLoginRequest 用户登录请求参数，包含用户账号和密码
     * @param request HTTP请求对象，用于获取请求相关信息
     * @return 返回登录结果，包含用户信息的BaseResponse对象
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 校验请求参数是否为空，为空则抛出参数错误异常
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        // 从请求参数中获取用户账号
        String userAccount = userLoginRequest.getUserAccount();
        // 从请求参数中获取用户密码
        String userPassword = userLoginRequest.getUserPassword();
        // 调用用户服务层处理登录逻辑，返回登录用户信息
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        // 返回登录成功结果
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 获取当前登录用户信息的接口方法
     *
     * @param request HttpServletRequest对象，用于获取当前请求信息
     * @return BaseResponse<LoginUserVO> 返回一个包含登录用户视图对象的基础响应对象
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 通过userService获取当前登录用户实体对象
        User loginUser = userService.getLoginUser(request);
        // 将用户实体对象转换为视图对象并封装到成功响应中返回
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    /**
     * 用户登出接口
     * 处理用户登出请求，验证请求参数并调用服务层方法执行登出操作
     *
     * @param request HTTP请求对象，包含用户会话信息
     * @return 返回操作结果，包含登出是否成功的布尔值
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 参数校验：如果请求对象为空，则抛出参数错误异常
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 调用用户服务层的登出方法，执行登出操作
        boolean result = userService.userLogout(request);
        // 返回操作成功结果，包含登出操作的结果
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }


    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }


    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }


    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }


    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


}
