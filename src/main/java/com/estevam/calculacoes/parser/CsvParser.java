package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;
import com.estevam.calculacoes.parser.exception.CsvParseException;

import lombok.extern.slf4j.Slf4j;

import com.estevam.calculacoes.core.util.TickerUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses CSV files in canonical format (comma-delimited, quoted decimals).
 * Expects input to be pre-normalized by CsvNormalizer.
 */
@Slf4j
public class CsvParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] EXPECTED_HEADERS = {
            "Data do Negócio",
            "Tipo de Movimentação",
            "Mercado",
            "Prazo/Vencimento",
            "Instituição",
            "Código de Negociação",
            "Quantidade",
            "Preço",
            "Valor"
        };

    /**
     * Validates that the CSV header matches the expected column order.
     * 
     * @param headerLine the first line of the CSV file
     * @throws CsvParseException if the header doesn't match the expected format
     */
    public static void validateHeader(String headerLine) throws CsvParseException {
        String[] actualHeaders = parseCsvLine(headerLine);
        
        if (actualHeaders.length != EXPECTED_HEADERS.length) {
            throw new CsvParseException(
                String.format("Invalid header: expected %d columns, but found %d", 
                    EXPECTED_HEADERS.length, actualHeaders.length), 
                null
            );
        }
        
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            String expected = EXPECTED_HEADERS[i].trim();
            String actual = actualHeaders[i].trim().replace("\"", "");
            
            if (!expected.equals(actual)) {
                throw new CsvParseException(
                    String.format("Invalid header at column %d: expected '%s', but found '%s'", 
                    i + 1, expected, actual), 
                    null
                );
            }
        }
    }


    /**
     * Parses canonical CSV content from a BufferedReader.
     * Expects content already normalized by CsvNormalizer.
     */
    public static Map<String, Asset> parseTrades(BufferedReader br) throws CsvParseException {
        Map<String, Asset> assets = new HashMap<>();
        String line;
        boolean isFirstLine = true;

        try {
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    validateHeader(line);
                    isFirstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = parseCsvLine(line);

                if (fields.length < 9) {
                    throw new CsvParseException(
                    "Malformed CSV line: expected at least 9 fields but found " + fields.length + ". Line content: " + line, 
                    null
                    );
                }

                LocalDate date = LocalDate.parse(fields[0].trim(), DATE_FORMATTER);
                String typeLabel = fields[1].trim();
                String marketType = fields[2].trim();
                String institution = fields[4].trim();
                String originalTicker = fields[5].trim();
                String ticker = TickerUtils.cleanTicker(originalTicker);

                // Parse quantity (may be quoted if fractional, e.g., "246,66")
                String quantityStr = fields[6].trim().replace("\"", "");
                quantityStr = quantityStr.replace(".", "").replace(",", ".");
                BigDecimal quantity = new BigDecimal(quantityStr);

                // Parse price
                String priceStr = fields[7];
                validateQuotedBrazilianDecimalFormat(priceStr);
                priceStr = priceStr.replace("\"", "").replace(" ", "");
                priceStr = priceStr.replace("R$", "").replace(".", "").replace(",", ".");
                BigDecimal price = new BigDecimal(priceStr);

                OperationType operationType = OperationType.fromPortuguese(typeLabel);

                Operation operation = new Operation(date, ticker, operationType, marketType, quantity, price);

                Asset asset = assets.computeIfAbsent(ticker, k -> new Asset(ticker, ticker, institution));
                asset.addOperation(operation);
            }
        } catch (IOException e) {
            throw new CsvParseException("Error reading CSV content", e);
        } catch (CsvParseException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvParseException("Error parsing CSV content", e);
        }

        return assets;
    }


    /**
     * Parses CSV content from byte array (for REST API uploads).
     * Normalizes the content before parsing.
     * 
     * @param csvContent the CSV file content as byte array
     * @param requestId the request identifier for logging
     * @return a map of assets indexed by trading code
     * @throws CsvParseException if parsing fails
     */
    public static Map<String, Asset> parseTradesFromCsvContent(byte[] csvContent, String requestId) throws CsvParseException {
        log.info("[requestId={}] Starting CSV normalization and parsing.", requestId);

        String normalized = CsvNormalizer.normalize(csvContent);
        log.info("[requestId={}] CSV normalization completed.", requestId);

        Map<String, Asset> assets;
        try (BufferedReader br = new BufferedReader(new StringReader(normalized))) {
            assets = parseTrades(br);
            log.info("[requestId={}] Completed CSV parsing. Parsed {} assets.", requestId, assets.size());
        } catch (IOException e) {
            log.error("[requestId={}] Error processing CSV: {}", requestId, e.getMessage(), e);
            throw new CsvParseException("Error reading normalized CSV content", e);
        } catch (CsvParseException e) {
            log.error("[requestId={}] Error processing CSV: {}", requestId, e.getMessage(), e);
            throw e;
        }

        return assets;
    }


    /**
     * Reads a CSV file and returns a map of assets indexed by trading code.
     * Normalizes the content before parsing.
     */
    public static Map<String, Asset> parseTradesFromCsv(String csvFilePath) throws CsvParseException {
        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(csvFilePath));
            String normalized = CsvNormalizer.normalize(fileBytes);

            try (BufferedReader br = new BufferedReader(new StringReader(normalized))) {
                return parseTrades(br);
            }
        } catch (IOException e) {
            throw new CsvParseException("Error reading CSV file: " + csvFilePath, e);
        } catch (CsvParseException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvParseException("Error parsing CSV content", e);
        }
    }

    /**
     * Parses a CSV line respecting quoted fields.
     */
    private static String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }


    /**
     * Validates that a price string is in Brazilian decimal format.
     * Accepts values with or without enclosing quotes.
     * 
     * Valid examples: "R$ 9,18", " R$9,18 ", " R$ 1.234,56 "
     * 
     * @param rawPrice the raw price string from the CSV
     * @throws CsvParseException if the format is invalid
     */
    private static void validateQuotedBrazilianDecimalFormat(String rawPrice) throws CsvParseException {
        rawPrice = rawPrice.trim();


        if (!(rawPrice.startsWith("\"") && rawPrice.endsWith("\""))) {
            throw new CsvParseException(
                "Invalid price: expected to be enclosed in quotes (e.g., \"R$10,03\"). Value received: " 
                + rawPrice,
            null);
        }

        String s = rawPrice.replace("\"", "");
        
        s = s.replace("R$", "").trim();

        if (s.matches(".*[a-zA-Z].*")) {
            throw new CsvParseException(
                "Invalid price: should not contain letters after R$ removal. Value received: \"" 
                + s + "\"",
            null);
        }

        int commaIndex = s.lastIndexOf(',');
        if (commaIndex < 0) {
            throw new CsvParseException(
                "Invalid price: expected decimal separator with comma (e.g., 10,03). Value received: \"" 
                + rawPrice + "\"",
            null);
        }

        String decimalPart = s.substring(commaIndex + 1);

        if (decimalPart.length() != 2 || !decimalPart.chars().allMatch(Character::isDigit)) {
            throw new CsvParseException(
                "Invalid price: expected exactly 2 decimal places (e.g., 10,03). Value received: \"" 
                + rawPrice + "\"",
                null
            );
        }
    }
}