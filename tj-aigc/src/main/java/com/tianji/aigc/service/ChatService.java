package com.tianji.aigc.service;

import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import reactor.core.publisher.Flux;

public interface ChatService {
    static String getConversationId(String sessionId) {
        return UserContext.getUser() + "_" + sessionId;
    }

    /**
     * 聊天接口
     * @param question
     * @param sessionId
     * @return
     */
    Flux<ChatEventVO> chat(String question, String sessionId);

    /**
     * 停止输出
     * @param sessionId
     */
    void stop(String sessionId);

    /**
     * 聊天接口
     * @param question
     * @return
     */
    String chatText(String question);
}
