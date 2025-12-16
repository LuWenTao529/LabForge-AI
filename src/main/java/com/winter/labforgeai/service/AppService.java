package com.winter.labforgeai.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.winter.labforgeai.model.dto.AppQueryRequest;
import com.winter.labforgeai.model.entity.App;
import com.winter.labforgeai.model.entity.User;
import com.winter.labforgeai.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用封装视图
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装视图列表
     *
     * @param appList 应用实体列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构建查询包装器
     *
     * @param appQueryRequest 查询请求
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 根据应用ID和用户消息生成代码的响应流
     *
     * @param appId 应用ID，用于标识特定的应用
     * @param message 用户输入的消息内容
     * @param loginUser 当前登录用户信息
     * @return 返回一个Flux<String>类型的响应流，包含生成的代码内容
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);
}
