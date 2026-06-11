package com.tianji.aigc.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.domain.enums.ChatEventTypeEnum;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractAgent implements Agent {
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private ChatClient chatClient;
    @Resource
    private ChatMemory chatMemory;

    // 输出结束的标记
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    // 存储大模型的生成状态，这里采用ConcurrentHashMap是确保线程安全
    // 目前的版本暂时用Map实现，如果考虑分布式环境的话，可以考虑用redis来实现
    public static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();



    /**
     * 创建并返回一个Advisor的空Map对象。
     *
     * @param sessionId 会话标识符
     * @param requestId 请求标识符
     * @return 默认返回一个空的Map对象，子类可以覆盖重写该方法以返回自定义的工具上下文。
     */
    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        var conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }


    /**
     * 处理流式请求（如流式回答）
     *
     * @param question  用户输入的问题
     * @param sessionId 会话唯一标识
     * @return 包含中间结果的反应式事件流（Flux）
     */
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        // 获取用户id
        var userId = UserContext.getUser();
        var requestId = this.generateRequestId();
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        var outputBuilder = new StringBuilder();
        // 获取对话id
        var conversationId = ChatService.getConversationId(sessionId);

        //更新会话时间
        this.chatSessionService.update(sessionId, question, userId);
        return this.getChatClientRequestSpec(question, sessionId, requestId)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true)) // 第一次输出内容时执行
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 出现异常时，删除标识
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时执行，删除标识
                .doOnCancel(() -> {
                    // 当输出被取消时，保存输出的内容到历史记录中
                    this.saveStopHistoryRecord(conversationId, outputBuilder.toString());
                })
                .takeWhile(response -> { // 通过返回值来控制Flux流是否继续，true：继续，false：终止
                    return GENERATE_STATUS.getOrDefault(sessionId, false);
                })
                .map(chatResponse -> {
                    var finishReason = chatResponse.getResult().getMetadata().getFinishReason();
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        var messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId);
                    }

                    // 获取大模型的输出的内容
                    var text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中
                    outputBuilder.append(text);
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
    /**
     * 保存停止输出的记录
     *
     * @param conversationId 对话id
     * @param content   大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {

        this.chatMemory.add(conversationId, new AssistantMessage(content));
    }

    /**
     * 处理标准请求（非流式）
     *
     * @param question  用户输入的问题
     * @param sessionId 会话唯一标识
     * @return 最终处理结果字符串
     */
    @Override
    public String process(String question, String sessionId) {
        // 获取用户id
        var userId = UserContext.getUser();
        var requestId = this.generateRequestId();

        //更新会话时间
        this.chatSessionService.update(sessionId, question, userId);
        return this.getChatClientRequestSpec(question, sessionId, requestId)
                .call()//调用大模型
                .content();//获取结果
    }

    @NotNull
    private ChatClient.ChatClientRequestSpec getChatClientRequestSpec(String question, String sessionId, String requestId) {
        return this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId, requestId))
                .user(question);
    }

    private String generateRequestId() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 停止会话
     *
     * @param sessionId 会话唯一标识
     */
    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
    }
}
