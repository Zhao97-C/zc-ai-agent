package com.nozc.zcaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        // first
        String message = "你好，我叫zc";
        String answer = loveApp.doChat(message, chatId);
    }

    @Test
    void testDoChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        // first
        String message = "你好，我叫zc，目前处于冷淡期，我不知道该怎么做";
        LoveApp.LoveReport answer = loveApp.doChatWithReport(message,chatId);
    }

    @Test
    void testDoChatWithPromptTemplate() {
        String chatId = UUID.randomUUID().toString();
        // first
        String message = "你好，我叫zc";
        String answer = loveApp.doChatWithPromptTemplate(message,chatId);
    }
}