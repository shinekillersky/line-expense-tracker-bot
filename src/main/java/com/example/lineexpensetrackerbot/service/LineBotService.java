package com.example.lineexpensetrackerbot.service;

import com.example.lineexpensetrackerbot.util.GoogleSheetsService;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class LineBotService {

    private static final Logger log = LoggerFactory.getLogger(LineBotService.class);

    @Autowired
    private LineMessagingClient lineMessagingClient;
    
    @Autowired
    private GoogleSheetsService sheetsService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        String userText = event.getMessage().getText();
        String replyText;
        try {
            if (userText.startsWith("新增 ")) {
                replyText = handleAdd(userText);
            } else if (userText.startsWith("查詢")) {
                replyText = handleQuery(userText);
            } else if (userText.startsWith("修改 ")) {
                replyText = handleModify(userText);
            } else if (userText.startsWith("刪除 ")) {
                replyText = handleDelete(userText);
            } else if (userText.startsWith("統計")) {
                replyText = handleSummary();
            } else {
                replyText = "請輸入正確的指令：新增/查詢/修改/刪除/統計";
            }
        } catch (Exception e) {
            log.error("處理訊息時發生錯誤", e);
            replyText = "❌ 操作失敗，請檢查格式或稍後再試。";
        }

        ReplyMessage replyMessage = new ReplyMessage(event.getReplyToken(), new TextMessage(replyText));
        try {
            lineMessagingClient.replyMessage(replyMessage).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("回覆訊息失敗", e);
        }
    }
    
    private String handleAdd(String text) throws Exception {
        String[] parts = text.split(" ", 4);
        if (parts.length < 3) return "❌ 格式錯誤，請輸入：新增 項目 金額 [備註]";

        String item = parts[1];
        String amount = parts[2];
        String note = parts.length == 4 ? parts[3] : "";
        String date = LocalDate.now().format(DATE_FORMATTER);

        List<Object> row = List.of(date, item, amount, note);
        sheetsService.appendRow("工作表1!A:D", row);
        return "✅ 已新增一筆記錄：" + item + " $" + amount;
    }

    private String handleQuery(String text) throws Exception {
        List<List<Object>> rows = sheetsService.readSheet("工作表1!A2:D");
        if (rows == null || rows.isEmpty()) return "❌ 尚無任何記錄";

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (List<Object> row : rows) {
            sb.append("第").append(i++).append("筆 ➜ ")
              .append(String.join(" | ", row.toString().replace("[", "").replace("]", "")))
              .append("\n");
        }
        return sb.toString();
    }

    private String handleModify(String text) throws Exception {
        String[] parts = text.split(" ", 5);
        if (parts.length < 5) return "❌ 格式錯誤，請輸入：修改 行號 項目 金額 備註";
        int row = Integer.parseInt(parts[1]) + 1; // 因為第 1 行是標題
        String item = parts[2];
        String amount = parts[3];
        String note = parts[4];

        sheetsService.updateCell("B" + row, item);
        sheetsService.updateCell("C" + row, amount);
        sheetsService.updateCell("D" + row, note);
        return "✅ 第 " + parts[1] + " 筆已修改";
    }

    private String handleDelete(String text) throws Exception {
        String[] parts = text.split(" ");
        if (parts.length < 2) return "❌ 格式錯誤，請輸入：刪除 行號";
        int row = Integer.parseInt(parts[1]) + 1;
        sheetsService.deleteRow(row);
        return "🗑️ 已刪除第 " + parts[1] + " 筆資料";
    }

    private String handleSummary() throws Exception {
        List<List<Object>> rows = sheetsService.readSheet("工作表1!A2:D");
        int total = 0;
        for (List<Object> row : rows) {
            if (row.size() >= 3) {
                try {
                    total += Integer.parseInt(row.get(2).toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return "📊 統計結果：總支出 " + total + " 元";
    }
    
}