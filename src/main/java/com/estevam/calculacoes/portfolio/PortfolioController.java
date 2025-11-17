package com.estevam.calculacoes.portfolio;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import com.estevam.calculacoes.portfolio.dto.PortfolioSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for portfolio operations.
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    /**
     * Endpoint to upload a CSV file and get consolidated positions.
     * 
     * POST /api/portfolio/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<PortfolioSummary> uploadCsv(@RequestParam("file") MultipartFile file) throws CsvParseException, Exception {
        Map<String, Asset> assets = portfolioService.processPortfolioFromCsvContent(file.getBytes());
        PortfolioSummary summary = portfolioService.generateSummary(assets);
        return ResponseEntity.ok(summary);
    }

    /**
     * Endpoint to get portfolio summary.
     * 
     * GET /api/portfolio/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummary> getSummary() {
        // TODO: Retrieve from database or session
        throw new UnsupportedOperationException("Not implemented yet");
    }
}