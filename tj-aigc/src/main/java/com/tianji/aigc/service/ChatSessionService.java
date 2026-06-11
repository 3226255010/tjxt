package com.tianji.aigc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.aigc.domain.entity.ChatSession;
import com.tianji.aigc.domain.vo.ChatSessionVO;
import com.tianji.aigc.domain.vo.MessageVO;
import com.tianji.aigc.domain.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {

    /**
     * 创建会话session
     *
     * @param num 热门问题的数量
     * @return 会话信息
     */
    SessionVO createSession(Integer num);

    /**
     * 热门问题
     * @param num
     * @return
     */
    List<SessionVO.Example> hotExamples(Integer num);

    /**
     * 查询单个历史对话详情
     * @param sessionId
     * @return
     */
    List<MessageVO> queryBySessionId(String sessionId);
    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    void update(String sessionId, String title, Long userId);

    /**
     * 查询历史会话
     * @return
     */
    Map<String, List<ChatSessionVO>> queryHistorySessions();

    /**
     * 删除历史会话
     * @param sessionId
     */
    void deleteHistorySession(String sessionId);

    /**
     * 修改历史会话标题
     * @param sessionId
     * @param title
     */
    void updateHistorySessionTitle(String sessionId, String title);
}
