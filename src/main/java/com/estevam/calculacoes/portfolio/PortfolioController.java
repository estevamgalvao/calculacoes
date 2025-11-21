package com.estevam.calculacoes.portfolio;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.core.response.Response;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import com.estevam.calculacoes.portfolio.dto.PortfolioSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * REST controller for portfolio operations.
 */
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio", description = "Endpoints for managing stock portfolio positions")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    /**
     * Endpoint to upload a CSV file and get consolidated positions.
     * 
     * POST /api/portfolio/upload
     */
    @Operation(
            summary = "Upload CSV file with trading history",
            description = "Uploads a CSV file containing stock trades and returns consolidated portfolio positions with average prices and realized profit/loss"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV processed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid CSV format or content",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Response.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "File size exceeds maximum allowed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Response.class)
                    )
            )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<PortfolioSummary>> uploadCsv(@RequestParam("file") MultipartFile file) throws CsvParseException, Exception {
        Map<String, Asset> assets = portfolioService.processPortfolioFromCsvContent(file.getBytes());
        PortfolioSummary summary = portfolioService.generateSummary(assets);
        return ResponseEntity.ok(new Response<>(true, 200, "HTTP_STATUS_OK", "Sucesso.", summary));
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

    /**
     * Health check endpoint.
     */
    @Operation(
            summary = "Health check",
            description = "Simple endpoint to verify the API is running"
    )
    @ApiResponse(
            responseCode = "200",
            description = "API is healthy"
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CalculAções API is running!");
    }
}