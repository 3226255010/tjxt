package com.tianji.aigc.controller;


import cn.hutool.core.collection.CollStreamUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 保存文本到向量库
 */
@Slf4j
@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 保存文本到向量库
     *
     * @param messages
     */
    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        log.info("保存文本到向量库中:{}", messages);
        var documentList = CollStreamUtil.toList(messages, message -> Document.builder()
                .text(message)
                .build());
        this.vectorStore.add(documentList);
        log.info("保存到向量库成功，数量:{}", messages.size());
    }

    /**
     * 文本转换成向量
     *
     * @param message
     * @return
     */
    @GetMapping
    public EmbeddingResponse textChangeToDocument(@RequestParam("message") String message) {
        return this.embeddingModel.embedForResponse(List.of(message));
    }

    /**
     * 删除向量库中的文本
     *
     * @param messages
     */
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids") List<String> ids) {
        this.vectorStore.delete(ids);
    }

    /**
     * 向量库搜索
     *
     * @param message
     * @return
     */
    @GetMapping("/search")
    public List<Document> search(@RequestParam("message") String message) {
        return this.vectorStore.similaritySearch(SearchRequest.builder()
                .topK(5)
                .query(message)
                .build());
    }

    /**
     * 向量库搜索所有
     *
     */
    @GetMapping("/search/all")
    public List<Document> searchAll() {
        return this.vectorStore.similaritySearch(SearchRequest.builder()
                .topK(999)
                .query("")
                .build());
    }
}
