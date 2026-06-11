package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.domain.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强型智能体
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tj.ai",name = "chat-type",havingValue = "ENHANCE")
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatClient openAiChatClient;
    private final SystemPromptConfig  systemPromptConfig;
    private final ChatMemory chatMemory;

    private final VectorStore vectorStore;

    private final ChatSessionService chatSessionService;


    private static final Map<String,Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    // 输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();


    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        //生成请求id
        var requestId = IdUtil.simpleUUID();
        //获取用户id
        Long userId = UserContext.getUser();

        // 更新会话标题
        this.chatSessionService.update(sessionId, question, userId);

        //注入qaAdvisor
        //定义RAG增强
        var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d)//相似度阈值
                        .topK(5)//搜索数量
                        .build())
                .build();
        return this.chatClient.prompt()
                .system(promptSystemSpec ->
                        promptSystemSpec
                                .text(this.systemPromptConfig.getChatSystemMessage().get())
                                .param("now", DateUtil.now())
                )
                .advisors(advisor->advisor
                        .advisors(qaAdvisor)//添加RAG增强
                        .param(ChatMemory.CONVERSATION_ID,conversationId))
                .toolContext(Map.of(Constant.REQUEST_ID,requestId,Constant.USER_ID,userId))//向工具传递请求id
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(()->GENERATE_STATUS.put(sessionId,true))
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId))
                .doOnCancel(()->{
                    //即使被取消也应该存放到数据库
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                        }

                )
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId))
                .takeWhile(response -> { // 通过返回值来控制Flux流是否继续，true：继续，false：终止
                    return GENERATE_STATUS.getOrDefault(sessionId, false);
                })
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(text);
                    //预下单卡片在大模型返回结束的末尾，所以我们可以在末尾进行关联
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if(StrUtil.equals(finishReason,Constant.STOP)){
                        //获取到消息id
                        String messageId = chatResponse.getMetadata().getId();
                        if(StrUtil.isNotBlank(messageId)){
                            //将消息id与请求id进行关联
                            ToolResultHolder.put(messageId,Constant.REQUEST_ID ,requestId);
                        }
                    }

                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // 通过请求id获取到参数列表，如果不为空，就将其追加到返回结果中
                    var map = ToolResultHolder.get(requestId);
                    if (CollUtil.isNotEmpty(map)) {
                        ToolResultHolder.remove(requestId); // 清除参数列表

                        // 响应给前端的参数数据
                        var chatEventVO = ChatEventVO.builder()
                                .eventData(map)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build();
                        return Flux.just(chatEventVO, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }


    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
    }
    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

    /**
     * 纯文本聊天
     *
     * @param question 问题
     * @return 纯文本的回答
     */
    @Override
    public String chatText(String question) {
        try {
            return this.openAiChatClient.prompt()
                    .system(promptSystem -> promptSystem.text(this.systemPromptConfig.getTextSystemMessage().get()))
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("调用OpenAI接口异常，question: {}", question, e);
            return "抱歉，AI 暂时无法回答您的问题，请稍后再试。";
        }
    }
}



