package com.tianji.aigc.controller;

import com.tianji.aigc.domain.dto.ChatDTO;
import com.tianji.aigc.domain.vo.ChatEventVO;
import com.tianji.aigc.domain.vo.TemplateVO;
import com.tianji.aigc.service.ChatService;
import com.tianji.common.annotations.NoWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private static final TemplateVO TEMPLATE_VO = new TemplateVO();

    /**
     * 聊天接口
     * @param chatDTO
     * @return
     */
    @NoWrapper
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return this.chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }

    /**
     * 停止输出
     * @param sessionId
     */
    @PostMapping("/stop")
    public void stop(@RequestParam String sessionId){
        this.chatService.stop(sessionId);
    }

    /**
     * 纯文本聊天接口
     * @param question
     * @return
     */
    @PostMapping("/text")
    public String chatText(@RequestBody String question) {
        return this.chatService.chatText(question);
    }


    /**
     * 获取模板
     * @return
     */
    @GetMapping("/templates")
    public TemplateVO getTemplates() {
        return TEMPLATE_VO;
    }
}
