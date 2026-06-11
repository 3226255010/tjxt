package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.AbstractAgent;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.domain.enums.AgentTypeEnum;
import com.tianji.aigc.domain.enums.ChatEventTypeEnum;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 基于路由智能体的会话
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai",name = "chat-type",havingValue = "ROUTE")
public class AgentServiceImpl implements ChatService {
    /**
     * 智能助手的会话
     */
    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        // 先通过路由智能体，分析用户的意图，再执行后面的逻辑
        var result = this.findAgentByAgentType(AgentTypeEnum.ROUTE).process(question, sessionId);
        var agentTypeEnum = AgentTypeEnum.agentNameOf(result);

        var agent = this.findAgentByAgentType(agentTypeEnum);
        if (agent == null) {
            // 找不到对应的智能体，直接返回结果
            var chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build();
            return Flux.just(chatEventVO, AbstractAgent.STOP_EVENT);
        }
        // 执行智能体的逻辑
        return agent.processStream(question, sessionId);
    }

    /**
     * 根据智能助手类型查找对应的智能助手
     */
    public Agent findAgentByAgentType(AgentTypeEnum agentType) {
        if (agentType == null) {
            return null;
        }
        //查找所有agent实例
        Map<String, Agent> agents = SpringUtil.getBeansOfType(Agent.class);
        //遍历并匹配
        for (Agent agent : agents.values()) {
            if (agent.getAgentType().equals(agentType)) {
                return agent;
            }
        }
        return null;
    }

    /**
     * 停止智能助手的会话
     */
    @Override
    public void stop(String sessionId) {
        this.findAgentByAgentType(AgentTypeEnum.ROUTE).stop(sessionId);
    }

    @Override
    public String chatText(String question) {
        return "";
    }
}
