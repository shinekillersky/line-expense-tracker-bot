package com.example.lineexpensetrackerbot.controller;

import com.example.lineexpensetrackerbot.service.LineBotService;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@LineMessageHandler
@Controller
public class LineBotController {

    private static final Logger log = LoggerFactory.getLogger(LineBotController.class);

    @Autowired
    private LineBotService lineBotService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
    	System.out.println("✅ 收到訊息：" + event.getMessage().getText());
        log.info("收到訊息事件：{}", event.getMessage().getText());
        lineBotService.handleTextMessage(event);
    }
}