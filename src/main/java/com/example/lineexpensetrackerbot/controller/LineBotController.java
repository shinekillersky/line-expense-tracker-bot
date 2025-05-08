package com.example.lineexpensetrackerbot.controller;

import com.example.lineexpensetrackerbot.service.LineBotService;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class LineBotController {

    @Autowired
    private LineBotService lineBotService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
        lineBotService.handleTextMessage(event);
    }
}