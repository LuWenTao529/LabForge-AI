package com.winter.labforgeai.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.winter.labforgeai.model.dto.chathistory.ChatHistoryQueryRequest;
import com.winter.labforgeai.model.entity.ChatHistory;
import com.winter.labforgeai.model.entity.User;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/LuWenTao529">Winter</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加聊天消息的方法
     *
     * @param appId 应用ID，用于标识不同的应用
     * @param message 聊天消息的具体内容
     * @param messageType 消息的类型，如文本、图片等
     * @param userId 发送消息的用户ID
     * @return 返回布尔值，表示消息添加是否成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);


    /**
     * 根据应用ID删除相关数据
     *
     * @param appId 应用ID，用于标识要删除的应用
     * @return 删除操作是否成功执行，返回true表示成功，false表示失败
     */
    boolean deleteByAppId(Long appId);


    /**
     * 根据聊天历史查询请求参数获取查询包装器
     *
     * @param chatHistoryQueryRequest 聊天历史查询请求对象，包含查询条件
     * @return QueryWrapper 返回一个包含查询条件的QueryWrapper对象，用于数据库查询操作
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);


    /**
     * 分页查询应用聊天记录
     *
     * @param appId 应用ID，用于指定要查询的应用
     * @param pageSize 每页大小，控制返回记录的数量
     * @param lastCreateTime 上一次查询的最后创建时间，用于分页查询
     * @param loginUser 当前登录用户，用于权限验证
     * @return 返回分页后的聊天记录列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
}
