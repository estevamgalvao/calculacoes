package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import com.estevam.calculacoes.core.util.TickerUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses CSV files containing stock trading history.
 */
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
     * Reads a CSV file and returns a map of assets indexed by trading code.
     */
    public static Map<String, Asset> parseTradesFromCsv(String csvFilePath) throws CsvParseException {
        Map<String, Asset> assets = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    validateHeader(line); // Validate header before processing
                    isFirstLine = false;
                    continue;
                }

                String[] fields = parseCsvLine(line);

                if (fields.length < 9) {
                    continue; // Skip malformed lines
                }

                LocalDate date = LocalDate.parse(fields[0].trim(), DATE_FORMATTER);
                String typeLabel = fields[1].trim();
                String marketType = fields[2].trim();
                String institution = fields[4].trim();
                String originalTicker = fields[5].trim();
                String ticker = TickerUtils.cleanTicker(originalTicker);
                int quantity = Integer.parseInt(fields[6].trim());
                String priceStr = fields[7];
                priceStr = priceStr.replace("\"", "").replace(" ", ""); // Remove quotes and spaces
                priceStr = priceStr.replace("R$", "").replace(".", "").replace(",", "."); // Normalize currency format
                BigDecimal price = new BigDecimal(priceStr);

                OperationType operationType = OperationType.fromPortuguese(typeLabel);

                Operation operation = new Operation(date, ticker, operationType, marketType, quantity, price);

                Asset asset = assets.computeIfAbsent(ticker, k -> new Asset(ticker, ticker, institution));
                asset.addOperation(operation);
            }

        } catch (IOException e) {
            throw new CsvParseException("Error reading CSV file: " + csvFilePath, e);
        } catch (CsvParseException e) {
            throw e; // Re-throw CSV parse exceptions
        } catch (Exception e) {
            throw new CsvParseException("Error parsing CSV content", e);
        }

        return assets;
    }

    /**
     * Parses a CSV line respecting quoted fields.
     */
    private static String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}