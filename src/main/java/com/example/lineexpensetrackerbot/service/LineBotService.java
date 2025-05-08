// LineBotService.java
package com.example.lineexpensetrackerbot.service;

import com.example.lineexpensetrackerbot.util.GoogleSheetsService;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.component.Text.TextWeight;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.container.Carousel;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        try {
            if (userText.startsWith("新增 ")) {
                String[] parts = userText.split(" ", 5);
                String item = parts.length > 1 ? parts[1] : "";
                String amount = parts.length > 2 ? parts[2] : "";
                String note = parts.length > 3 ? parts[3] : "";
                String date = parts.length > 4 ? parts[4] : LocalDate.now().format(DATE_FORMATTER);

                if (item.isEmpty() || amount.isEmpty()) {
                    replyWithText(event, "❌ 格式錯誤，請輸入：新增 項目 金額 [備註] [日期]");
                    return;
                }

                sheetsService.appendRow("工作表1", List.of(date, item, amount, note));
                Bubble bubble = buildRecordBubble(date, item, amount, note, 1);
                FlexMessage flex = new FlexMessage("新增記錄", bubble);
                lineMessagingClient.replyMessage(new ReplyMessage(event.getReplyToken(), flex)).get();

            } else if (userText.startsWith("查詢")) {
                List<List<Object>> rows = sheetsService.readSheet("工作表1!A2:D");
                if (rows == null || rows.isEmpty()) {
                    replyWithText(event, "❌ 尚無任何記錄，請重新輸入指令。");
                    return;
                }
                List<Bubble> bubbles = new ArrayList<>();
                int index = 1;
                for (List<Object> row : rows) {
                    String date = getFromRow(row, 0);
                    String item = getFromRow(row, 1);
                    String amount = getFromRow(row, 2);
                    String note = getFromRow(row, 3);
                    bubbles.add(buildRecordBubble(date, item, amount, note, index++));
                }
                lineMessagingClient.replyMessage(new ReplyMessage(event.getReplyToken(), new FlexMessage("查詢結果", Carousel.builder().contents(bubbles).build()))).get();

            } else if (userText.startsWith("修改 ")) {
                String[] parts = userText.split(" ", 6);
                if (parts.length < 4) {
                    replyWithText(event, "❌ 格式錯誤，請輸入：修改 行號 項目 金額 [備註] [日期]");
                    return;
                }
                int row = Integer.parseInt(parts[1]);
                String item = parts[2];
                String amount = parts[3];
                String note = parts.length > 4 ? parts[4] : "";
                String date = parts.length > 5 ? parts[5] : LocalDate.now().format(DATE_FORMATTER);

                sheetsService.updateCell("工作表1!A" + (row + 1), date);
                sheetsService.updateCell("工作表1!B" + (row + 1), item);
                sheetsService.updateCell("工作表1!C" + (row + 1), amount);
                sheetsService.updateCell("工作表1!D" + (row + 1), note);

                Bubble bubble = buildRecordBubble(date, item, amount, note, row);
                FlexMessage flex = new FlexMessage("更新記錄", bubble);
                lineMessagingClient.replyMessage(new ReplyMessage(event.getReplyToken(), flex)).get();

            } else if (userText.startsWith("刪除 ")) {
                String[] parts = userText.split(" ");
                if (parts.length != 2) {
                    replyWithText(event, "❌ 格式錯誤，請輸入：刪除 行號");
                    return;
                }
                int row = Integer.parseInt(parts[1]);
                sheetsService.deleteRow(row + 1);
                replyWithText(event, "✅ 已成功刪除第 " + row + " 筆資料");

            } else if (userText.startsWith("統計")) {
                String date = userText.length() > 2 ? userText.substring(2).trim() : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String dashDate = date.replaceAll("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");

                List<List<Object>> rows = sheetsService.readSheet("工作表1!A2:D");
                int total = 0;
                Map<String, Integer> itemTotals = new HashMap<>();
                for (List<Object> row : rows) {
                    if (getFromRow(row, 0).equals(dashDate)) {
                        int amt = Integer.parseInt(getFromRow(row, 2));
                        String item = getFromRow(row, 1);
                        total += amt;
                        itemTotals.put(item, itemTotals.getOrDefault(item, 0) + amt);
                    }
                }
                if (total == 0) {
                    replyWithText(event, "❌ " + dashDate + " 查無資料");
                    return;
                }
                StringBuilder detail = new StringBuilder();
                itemTotals.forEach((k, v) -> detail.append(k).append(": ").append(v).append("\n"));
                replyWithText(event, "📊 統計日期：" + dashDate + "\n總金額：" + total + " 元\n\n明細：\n" + detail);
            } else {
                replyWithQuickMenu(event);
            }
        } catch (Exception e) {
            log.error("❌ 錯誤：", e);
            replyWithText(event, "❌ 操作失敗，請檢查格式或稍後再試。");
        }
    }

    private void replyWithQuickMenu(MessageEvent<TextMessageContent> event) throws ExecutionException, InterruptedException {
        List<QuickReplyItem> items = List.of(
                QuickReplyItem.builder().action(new MessageAction("➕ 新增", "新增 早餐 100 QBurger")).build(),
                QuickReplyItem.builder().action(new MessageAction("📋 查詢", "查詢")).build(),
                QuickReplyItem.builder().action(new MessageAction("✏️ 修改", "修改 1 午餐 120 拉麵")).build(),
                QuickReplyItem.builder().action(new MessageAction("🗑️ 刪除", "刪除 1")).build(),
                QuickReplyItem.builder().action(new MessageAction("📊 統計", "統計" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))).build()
        );
        Message msg = TextMessage.builder()
                .text("請選擇功能：")
                .quickReply(QuickReply.items(items))
                .build();
        lineMessagingClient.replyMessage(new ReplyMessage(event.getReplyToken(), msg)).get();
    }

    private void replyWithText(MessageEvent<TextMessageContent> event, String text) {
        try {
            lineMessagingClient.replyMessage(new ReplyMessage(event.getReplyToken(), new TextMessage(text))).get();
        } catch (Exception e) {
            log.error("❌ 傳送訊息失敗", e);
        }
    }

    // ✅ 正確的輔助方法：取得某行的第 i 欄資料
    private String getFromRow(List<Object> row, int i) {
        return (row != null && row.size() > i) ? row.get(i).toString() : "";
    }

    private Bubble buildRecordBubble(String date, String item, String amount, String note, int index) {
        Box body = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(List.of(
                		Text.builder().text("📝 第 " + index + " 筆").weight(Text.TextWeight.BOLD).build(),
                        Text.builder().text("📅 日期: " + date).build(),
                        Text.builder().text("📝 項目: " + item).build(),
                        Text.builder().text("💰 金額: " + amount).build(),
                        Text.builder().text("🗒️ 備註: " + note).wrap(true).build()
                )).build();
        return Bubble.builder().body(body).build();
    }
}
