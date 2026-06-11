package com.tianji.aigc.agent;

import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.domain.enums.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getKnowledgeAgentSystemMessage().get();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

}
