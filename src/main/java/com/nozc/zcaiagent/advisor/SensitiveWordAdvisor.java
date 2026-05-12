package com.nozc.zcaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 敏感词校验 Advisor
 * <p>
 * 在调用大模型之前，检查用户输入是否包含敏感词，如果包含则直接拦截，不再调用大模型。
 * 支持从外部文件动态加载敏感词，修改文件后无需重启服务即可生效。
 * </p>
 */
@Slf4j
public class SensitiveWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final int order;

    /**
     * 敏感词文件路径（为 null 时使用内存中的固定列表）
     */
    private final File sensitiveWordFile;

    /**
     * 当前生效的敏感词列表（volatile 保证多线程可见性）
     */
    private volatile List<String> sensitiveWords;

    /**
     * 上次文件修改时间戳，用于判断文件是否变更
     */
    private volatile long lastModified = 0;

    // ======================== 构造方法 ========================

    /**
     * 从文件加载敏感词，使用最高优先级
     *
     * @param filePath 敏感词文件路径，每行一个敏感词
     */
    public SensitiveWordAdvisor(String filePath) {
        this(filePath, Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * 从文件加载敏感词，指定优先级
     *
     * @param filePath 敏感词文件路径，每行一个敏感词
     * @param order    执行优先级
     */
    public SensitiveWordAdvisor(String filePath, int order) {
        this.sensitiveWordFile = new File(filePath);
        this.order = order;
        // 初始化加载
        this.sensitiveWords = loadSensitiveWords();
    }

    /**
     * 使用固定敏感词列表（不支持热更新），指定优先级
     *
     * @param sensitiveWords 敏感词列表
     * @param order          执行优先级
     */
    public SensitiveWordAdvisor(List<String> sensitiveWords, int order) {
        this.sensitiveWordFile = null;
        this.sensitiveWords = sensitiveWords;
        this.order = order;
    }

    // ======================== 文件加载与热更新 ========================

    /**
     * 从文件加载敏感词列表
     */
    private List<String> loadSensitiveWords() {
        if (sensitiveWordFile == null || !sensitiveWordFile.exists()) {
            log.warn("敏感词文件不存在: {}", sensitiveWordFile);
            return List.of();
        }
        try {
            List<String> words = Files.readAllLines(sensitiveWordFile.toPath(), StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
            this.lastModified = sensitiveWordFile.lastModified();
            log.info("加载敏感词文件成功，共 {} 个敏感词: {}", words.size(), sensitiveWordFile.getAbsolutePath());
            return words;
        } catch (IOException e) {
            log.error("读取敏感词文件失败: {}", sensitiveWordFile.getAbsolutePath(), e);
            return List.of();
        }
    }

    /**
     * 检查文件是否有变更，如有则重新加载
     */
    private void refreshIfNeeded() {
        if (sensitiveWordFile == null || !sensitiveWordFile.exists()) {
            return;
        }
        long currentModified = sensitiveWordFile.lastModified();
        if (currentModified != lastModified) {
            log.info("检测到敏感词文件变更，重新加载: {}", sensitiveWordFile.getAbsolutePath());
            this.sensitiveWords = loadSensitiveWords();
        }
    }

    // ======================== 敏感词检测 ========================

    /**
     * 获取当前生效的敏感词列表（会自动刷新文件变更）
     */
    private List<String> getSensitiveWords() {
        refreshIfNeeded();
        return sensitiveWords;
    }

    /**
     * 检查文本中是否包含敏感词
     *
     * @param text 待检查的文本
     * @return 匹配到的敏感词列表，如果没有则返回空列表
     */
    private List<String> findSensitiveWords(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return getSensitiveWords().stream()
                .filter(text::contains)
                .toList();
    }

    // ======================== 拦截响应 ========================

    /**
     * 构建拦截后的响应
     */
    private AdvisedResponse buildBlockedResponse(AdvisedRequest advisedRequest, List<String> matched) {
        String msg = "您的输入包含敏感词 " + matched + "，请修改后重试。";
        log.warn("敏感词拦截，用户输入: {}，命中敏感词: {}", advisedRequest.userText(), matched);
        // 构建助手消息返回提示
        AssistantMessage assistantMessage = new AssistantMessage(msg);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        return new AdvisedResponse(chatResponse, advisedRequest.adviseContext());
    }

    // ======================== Advisor 接口实现 ========================

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        List<String> matched = findSensitiveWords(advisedRequest.userText());
        if (!matched.isEmpty()) {
            return buildBlockedResponse(advisedRequest, matched);
        }
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        List<String> matched = findSensitiveWords(advisedRequest.userText());
        if (!matched.isEmpty()) {
            return Flux.just(buildBlockedResponse(advisedRequest, matched));
        }
        return chain.nextAroundStream(advisedRequest);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}