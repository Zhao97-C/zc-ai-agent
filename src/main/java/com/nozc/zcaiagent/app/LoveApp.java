package com.nozc.zcaiagent.app;

import com.nozc.zcaiagent.advisor.MyLoggerAdvisor;
import com.nozc.zcaiagent.advisor.SensitiveWordAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";


    public LoveApp(ChatModel dashscopeChatModel) {
        // 基于内存的会话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();
        // 基于文件的会话及记忆
//        ChatMemory chatMemory = new FileBasedChatMemory("user.dir"+"/tmp/chat-memory");
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SensitiveWordAdvisor("src/main/resources/sensitive-words.txt"),
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor(0)

//                        ,new ReReadingAdvisor(0)
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    record LoveReport(String title, List<String> suggestion) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
//                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    public String doChatWithPromptTemplate(String message,String chatId) {

        String systemText = SYSTEM_PROMPT + "你需要以{system_name}自己的称呼来回复用户";
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        String name = "小明";
        Prompt prompt = systemPromptTemplate.create(Map.of("system_name", name));


        ChatResponse chatResponse = chatClient
                .prompt(prompt)
                .user(message)
                .advisors(spce -> spce.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chatResponse: {}", content);
        return content;
    }

    @Resource
    private VectorStore MyVectorStore;

    public String doChatWithVectorStore(String message, String chatId) {

        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spce -> spce.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor(10))
                .advisors(new QuestionAnswerAdvisor(MyVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chatResponse: {}", content);
        return content;
    }


}
