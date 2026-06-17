package com.processing.transactionlogger.export;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Сериализует транзакции в CSV (RFC 4180)
 * Первая строка - заголовк с именами колонок, дале по строке на транзакцию.
 * Поля, содержащие спец символы экранируются кавычками.
 * {@code null}-значения превращаются в пустые поля
 */
@Component
public class TransactionCsvWriter {
    private static final char DELIMITER = ',';
    private static final String LINE_END = "\r\n";
    private static final String HEADER = String.join(",", "id", "mti", "stan", "rrn", "processingCode",
            "amount", "currencyCode", "terminalId", "terminalType", "merchantId", "mcc", "acquirerId", "issuerId",
            "acquiringFee", "status", "declineReason", "authCode", "processingTimeMs", "transmissionDateTime", "createdAt");

    /**
     * Формирует CSV-документ из списка транзакций.
     *
     * @param transactions транзакции для экспорта (порядок сохраняется)
     * @return CSV-текст с заголовком. Для пустого списка - только строка заголовка.
     */
    public String toCsv(List<TransactionResponse> transactions) {
        StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
        for (TransactionResponse transaction : transactions) {
            appendRow(csv, transaction);
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder csv, TransactionResponse transaction) {
        appendField(csv, transaction.id(), true);
        appendField(csv, transaction.mti(), true);
        appendField(csv, transaction.stan(), true);
        appendField(csv, transaction.rrn(), true);
        appendField(csv, transaction.pan(), true);
        appendField(csv, transaction.processingCode(), true);
        appendField(csv, transaction.amount(), true);
        appendField(csv, transaction.currencyCode(), true);
        appendField(csv, transaction.terminalId(), true);
        appendField(csv, transaction.terminalType(), true);
        appendField(csv, transaction.merchantId(), true);
        appendField(csv, transaction.mcc(), true);
        appendField(csv, transaction.acquirerId(), true);
        appendField(csv, transaction.issuerId(), true);
        appendField(csv, transaction.acquiringFee(), true);
        appendField(csv, transaction.status(), true);
        appendField(csv, transaction.declineReason(), true);
        appendField(csv, transaction.authCode(), true);
        appendField(csv, transaction.processingTimeMs(), true);
        appendField(csv, transaction.transmissionDateTime(), true);
        appendField(csv, transaction.createdAt(), true);
        csv.append(LINE_END);
    }

    private void appendField(StringBuilder csv, Object value, boolean first) {
        if (!first) {
            csv.append(DELIMITER);
        }
        csv.append(escapeSpecialCharacters(value));
    }

    private String escapeSpecialCharacters(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.indexOf(DELIMITER) < 0 && text.indexOf('"') < 0
            && text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
