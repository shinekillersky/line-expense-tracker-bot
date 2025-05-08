package com.example.lineexpensetrackerbot.util;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;

@SuppressWarnings("deprecation")
@Component
public class GoogleSheetsService {

	private Sheets sheetsService;
    private String spreadsheetId = System.getenv("SPREADSHEET_ID");

    public GoogleSheetsService() throws Exception {
        String base64Json = System.getenv("GOOGLE_CREDENTIALS_BASE64");
        byte[] decoded = Base64.getDecoder().decode(base64Json);
        GoogleCredential credential = GoogleCredential.fromStream(new ByteArrayInputStream(decoded))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        this.sheetsService = new Sheets.Builder(
                credential.getTransport(), credential.getJsonFactory(), credential)
                .setApplicationName("Line Expense Tracker")
                .build();
    }

    public List<List<Object>> readSheet(String range) throws Exception {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    public void appendRow(String range, List<Object> rowData) throws Exception {
        ValueRange appendBody = new ValueRange().setValues(Collections.singletonList(rowData));
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, range, appendBody)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public void updateCell(String range, Object value) throws Exception {
        ValueRange body = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(value)));
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public void deleteRow(int rowIndex) throws Exception {
        BatchUpdateSpreadsheetRequest content = new BatchUpdateSpreadsheetRequest().setRequests(Collections.singletonList(
                new Request().setDeleteDimension(new DeleteDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(0)
                                .setDimension("ROWS")
                                .setStartIndex(rowIndex - 1)
                                .setEndIndex(rowIndex)))
        ));
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, content).execute();
    }
}