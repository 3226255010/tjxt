package com.tianji.aigc.config;

import com.tianji.aigc.advisor.RecordOptimizationAdvisor;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import com.tianji.aigc.memory.RedisChatMemoryRepository;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.aigc.tools.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${tj.ai.memory.max:100}")
    private Integer maxMessages;

    /**
     * 配置 ChatClient
     */
    @Bean
    public ChatClient chatClient(@Qualifier("dashscopeChatModel") ChatModel dashScopeChatModel,
                                 Advisor loggerAdvisor,
                                 Advisor messageChatMemoryAdvisor,
                                 Advisor recordOptimizationAdvisor
                                 //CourseTools courseTools,//课程工具
                                 //OrderTools orderTools//预下单工具
                                 ) {  // 日志记录器
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(loggerAdvisor,messageChatMemoryAdvisor,recordOptimizationAdvisor) //添加 Advisor 功能增强
                //.defaultTools(courseTools,orderTools)
                .build();
    }


    @Bean
    public ChatClient openAiChatClient(@Qualifier("openAiChatModel") ChatModel openAiChatModel,
                                       Advisor loggerAdvisor  // 日志记录器
    ) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(loggerAdvisor)
                .build();
    }

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }


    @Bean
    public ChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        //基于ChatMemoryRepository创建ChatMemory对象
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(this.maxMessages)
                .build();
    }
    /**
     * 基于Redis的会话记忆，聊天记忆整合到message列表中实现多轮对话
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        // 创建基于 chatMemory 的 Advisor 对象
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public Advisor recordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository){
        return new RecordOptimizationAdvisor(myChatMemoryRepository);
    }
}
