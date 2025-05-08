package com.example.lineexpensetrackerbot.service;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class LineBotService {

    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    @Autowired
    private LineMessagingClient lineMessagingClient;

    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        String userText = event.getMessage().getText();
        String replyText = "你說的是：" + userText;
        ReplyMessage replyMessage = new ReplyMessage(event.getReplyToken(), new TextMessage(replyText));
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("回覆訊息失敗", e);
        }
    }
}