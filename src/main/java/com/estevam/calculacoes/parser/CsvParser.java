package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;
import com.estevam.calculacoes.core.util.TickerUtils;
//import com.estevam.calculacoes.parser.exception.CsvParseException;

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
                    isFirstLine = false; // Skip header
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
                String priceStr = fields[7].trim().replace("R$", "").replace(".", "").replace(",", ".").trim();
                BigDecimal price = new BigDecimal(priceStr);

                OperationType operationType = OperationType.fromPortuguese(typeLabel);

                Operation operation = new Operation(date, ticker, operationType, marketType, quantity, price);

                Asset asset = assets.computeIfAbsent(ticker, k -> new Asset(ticker, ticker, institution));
                asset.addOperation(operation);
            }

        } catch (IOException e) {
            throw new CsvParseException("Error reading CSV file: " + csvFilePath, e);
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