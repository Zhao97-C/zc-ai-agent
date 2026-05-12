package com.nozc.zcaiagent.advisor;

import cn.hutool.db.sql.Order;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 简单的一个敏感词拦截器实现
 */
public class SensitiveWord2Advisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final int order;

    private List<String> sensitiveWords = List.of("暴力","血腥","杀戮");

    public SensitiveWord2Advisor(List<String> sensitiveWords,int order) {
        this.order = order;
        this.sensitiveWords = sensitiveWords;
    }

    public SensitiveWord2Advisor(List<String> sensitiveWords) {
        this.order = Ordered.HIGHEST_PRECEDENCE;
        this.sensitiveWords = sensitiveWords;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return null;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        List<String> sensitiveWord = findSensitiveWord(advisedRequest.userText());
        if (!sensitiveWord.isEmpty()) {
            AssistantMessage assistantMessage = new AssistantMessage("您输入的消息中存在敏感词"+sensitiveWord+",请修改后重试");
            Generation generation = new Generation(assistantMessage);
            ChatResponse chatResponse = new ChatResponse(List.of(generation));
            return new AdvisedResponse(chatResponse,advisedRequest.adviseContext());
        }


        chain.nextAroundCall(advisedRequest);
        return null;
    }

    private List<String> findSensitiveWord(String userText){
        if (userText == null || userText.isBlank()) {
            return List.of();
        }
        return this.sensitiveWords.stream().filter(userText::contains).toList();
    }
}
