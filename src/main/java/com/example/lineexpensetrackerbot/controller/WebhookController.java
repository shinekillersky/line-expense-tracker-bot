package com.example.lineexpensetrackerbot.controller;

import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import com.linecorp.bot.client.LineMessagingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.ExecutionException;

@RestController
@LineMessageHandler
public class WebhookController {

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @com.linecorp.bot.spring.boot.annotation.EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws ExecutionException, InterruptedException {
        String receivedText = event.getMessage().getText();
        String replyToken = event.getReplyToken();

        // 回覆相同訊息
        TextMessage reply = new TextMessage("你說的是：" + receivedText);
        lineMessagingClient.replyMessage(new com.linecorp.bot.model.ReplyMessage(replyToken, reply)).get();
    }
}
