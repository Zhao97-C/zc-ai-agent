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
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // first
        String message = "你好，我叫zc";
        String answer = loveApp.doChat(message, chatId);

        // second
        message = "我另一半叫nozc，我想让TA更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        // third
        message = "我叫什么？";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }
}