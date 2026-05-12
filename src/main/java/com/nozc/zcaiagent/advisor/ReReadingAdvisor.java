package com.nozc.zcaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

@Slf4j
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private int order = 0;

    public ReReadingAdvisor(int order) {
        this.order = order;
    }

    /**
     * 修改了重读模板代码，可以结合 log advisor 验证
     * @param advisedRequest
     * @return
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
    
//        log.info("re2 1 request:{}",advisedRequest.userText());
            
        String inputQuery = advisedRequest.userText();
            
        // 手动替换占位符
        String processedText = """
                %s
                Read the question again: %s
                """.formatted(inputQuery, inputQuery);
    
        AdvisedRequest build = AdvisedRequest.from(advisedRequest)
                .userText(processedText)
                .build();
    
//        log.info("re2 2 request:{}",build.userText());
    
        return build;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
