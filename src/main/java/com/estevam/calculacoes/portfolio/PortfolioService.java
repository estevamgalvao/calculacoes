package com.estevam.calculacoes.portfolio;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.parser.CsvParser;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import com.estevam.calculacoes.portfolio.dto.PortfolioSummary;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for orchestrating portfolio operations.
 * Coordinates parsing, asset consolidation, and report generation.
 */
@Service
public class PortfolioService {

    /**
     * Processes a CSV file and returns consolidated asset positions.
     */
    public Map<String, Asset> processPortfolioFromCsv(String csvFilePath) throws CsvParseException {
        return CsvParser.parseTradesFromCsv(csvFilePath);
    }

    /**
     * Processes uploaded CSV content (for REST API).
     */
    public Map<String, Asset> processPortfolioFromCsvContent(byte[] csvContent) throws CsvParseException {
        // TODO: Implement in-memory CSV parsing
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Generates a summary report from assets.
     */
    public PortfolioSummary generateSummary(Map<String, Asset> assets) {
        // TODO: Aggregate totals, calculate portfolio metrics
        return new PortfolioSummary(assets);
    }
}