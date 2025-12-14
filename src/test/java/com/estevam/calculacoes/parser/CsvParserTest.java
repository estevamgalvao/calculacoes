package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;
import com.estevam.calculacoes.parser.exception.CsvParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CsvParser class.
 * 
 * Purpose: Ensure CSV parsing logic correctly handles:
 * - Valid CSV files with proper headers
 * - Invalid headers (wrong columns, wrong order)
 * - Malformed lines (missing fields, wrong data types)
 * - Currency formatting (R$, dots, commas)
 * - Date parsing (dd/MM/yyyy format)
 * - Ticker cleaning (fractional market 'F' suffix)
 * - Asset consolidation (multiple operations for same ticker)
 * 
 * Context: This parser reads CSV exports from Brazilian brokers
 * (e.g., Clear, Rico, XP) containing stock trading history.
 */
@DisplayName("CsvParser - CSV File Parsing Tests")
class CsvParserTest {

    /**
     * @TempDir creates a temporary directory for each test.
     * Automatically cleaned up after test execution.
     * 
     * Used for creating test CSV files without polluting src/test/resources.
     */
    @TempDir
    Path tempDir;

    // ========================================
    // PART 1: HEADER VALIDATION TESTS
    // ========================================

    /**
     * TEST 1: Should accept valid CSV header
     * 
     * Scenario: CSV has correct header with all expected columns in correct order
     * Expected: No exception thrown
     */
    @Test
    @DisplayName("Should accept valid CSV header")
    void shouldAcceptValidHeader() {
        // ARRANGE
        String validHeader = "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento," +
                           "Instituição,Código de Negociação,Quantidade,Preço,Valor";
        
        // ACT & ASSERT
        assertThatNoException()
            .as("Valid header should not throw exception")
            .isThrownBy(() -> CsvParser.validateHeader(validHeader));
    }

    /**
     * TEST 2: Should reject header with wrong number of columns
     * 
     * Scenario: CSV has fewer columns than expected
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should reject header with wrong number of columns")
    void shouldRejectHeaderWithWrongNumberOfColumns() {
        // ARRANGE - Only 5 columns instead of 9
        String invalidHeader = "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.validateHeader(invalidHeader))
            .as("Header with wrong column count should throw CsvParseException")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("expected 9 columns")
            .hasMessageContaining("but found 5");
    }

    /**
     * TEST 3: Should reject header with wrong column name
     * 
     * Scenario: CSV has correct number of columns but wrong name at position 2
     * Expected: CsvParseException indicating which column is wrong
     */
    @Test
    @DisplayName("Should reject header with wrong column name")
    void shouldRejectHeaderWithWrongColumnName() {
        // ARRANGE - "Market" instead of "Mercado" at position 3
        String invalidHeader = "Data do Negócio,Tipo de Movimentação,Market,Prazo/Vencimento," +
                             "Instituição,Código de Negociação,Quantidade,Preço,Valor";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.validateHeader(invalidHeader))
            .as("Header with wrong column name should throw CsvParseException")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Invalid header at column 3")
            .hasMessageContaining("expected 'Mercado'")
            .hasMessageContaining("but found 'Market'");
    }

    /**
     * TEST 4: Should reject header with columns in wrong order
     * 
     * Scenario: All columns present but in different order
     * Expected: CsvParseException (order matters for parsing)
     */
    @Test
    @DisplayName("Should reject header with columns in wrong order")
    void shouldRejectHeaderWithColumnsInWrongOrder() {
        // ARRANGE - Swapped first two columns
        String invalidHeader = "Tipo de Movimentação,Data do Negócio,Mercado,Prazo/Vencimento," +
                             "Instituição,Código de Negociação,Quantidade,Preço,Valor";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.validateHeader(invalidHeader))
            .as("Header with wrong column order should throw CsvParseException")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Invalid header at column 1")
            .hasMessageContaining("expected 'Data do Negócio'")
            .hasMessageContaining("but found 'Tipo de Movimentação'");
    }

    /**
     * TEST 5: Should handle header with extra quotes
     * 
     * Scenario: Column names are wrapped in quotes (common in CSV exports)
     * Expected: Quotes are stripped and validation passes
     */
    @Test
    @DisplayName("Should handle header with quoted column names")
    void shouldHandleHeaderWithQuotes() {
        // ARRANGE - Columns wrapped in quotes
        String quotedHeader = "\"Data do Negócio\",\"Tipo de Movimentação\",\"Mercado\"," +
                            "\"Prazo/Vencimento\",\"Instituição\",\"Código de Negociação\"," +
                            "\"Quantidade\",\"Preço\",\"Valor\"";
        
        // ACT & ASSERT
        assertThatNoException()
            .as("Header with quotes should be accepted after stripping")
            .isThrownBy(() -> CsvParser.validateHeader(quotedHeader));
    }

    // ========================================
    // PART 2: VALID CSV PARSING TESTS
    // ========================================

    /**
     * TEST 6: Should parse CSV with single buy operation
     * 
     * Scenario: CSV contains header + 1 buy operation
     * Expected: Returns map with 1 asset containing 1 operation
     */
    @Test
    @DisplayName("Should parse CSV with single buy operation")
    void shouldParseCsvWithSingleBuyOperation() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("single_buy.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should return map with 1 asset")
            .hasSize(1)
            .containsKey("PETR4");
        
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getTradingCode()).isEqualTo("PETR4");
        assertThat(petr4.getInstitution()).isEqualTo("Clear Corretora");
        assertThat(petr4.getQuantity()).isEqualTo(100);
        assertThat(petr4.getAveragePrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(petr4.getOperations()).hasSize(1);
        
        Operation operation = petr4.getOperations().get(0);
        assertThat(operation.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(operation.getType()).isEqualTo(OperationType.BUY);
        assertThat(operation.getMarketType()).isEqualTo("à vista");
        assertThat(operation.getQuantity()).isEqualTo(100);
        assertThat(operation.getPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    /**
     * TEST 7: Should parse CSV with multiple operations for same asset
     * 
     * Scenario: CSV contains 2 buy operations + 1 sell operation for PETR4
     * Expected: Returns map with 1 asset containing 3 operations, correctly consolidated
     */
    @Test
    @DisplayName("Should parse CSV with multiple operations for same asset")
    void shouldParseCsvWithMultipleOperationsForSameAsset() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"\n" +
            "20/02/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 20,00\",\"R$ 2.000,00\"\n" +
            "10/03/2024,venda,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 25,00\",\"R$ 2.500,00\"";
        
        Path csvFile = createTempCsvFile("multiple_operations.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should return map with 1 asset")
            .hasSize(1)
            .containsKey("PETR4");
        
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getOperations())
            .as("Should have 3 operations")
            .hasSize(3);
        
        // After: buy 100@10, buy 100@20, sell 100@25
        // Remaining: 100 shares at avg price 15
        // Realized profit: (25-15)*100 = 1000
        assertThat(petr4.getQuantity()).isEqualTo(100);
        assertThat(petr4.getAveragePrice()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(petr4.getRealizedProfitLoss()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    /**
     * TEST 8: Should parse CSV with multiple different assets
     * 
     * Scenario: CSV contains operations for PETR4, VALE3, and ITUB4
     * Expected: Returns map with 3 assets, each correctly consolidated
     */
    @Test
    @DisplayName("Should parse CSV with multiple different assets")
    void shouldParseCsvWithMultipleDifferentAssets() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"\n" +
            "16/01/2024,compra,à vista,D+2,Clear Corretora,VALE3,50,\"R$ 60,00\",\"R$ 3.000,00\"\n" +
            "17/01/2024,compra,à vista,D+2,Clear Corretora,ITUB4,200,\"R$ 25,00\",\"R$ 5.000,00\"";
        
        Path csvFile = createTempCsvFile("multiple_assets.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should return map with 3 assets")
            .hasSize(3)
            .containsKeys("PETR4", "VALE3", "ITUB4");
        
        assertThat(assets.get("PETR4").getQuantity()).isEqualTo(100);
        assertThat(assets.get("VALE3").getQuantity()).isEqualTo(50);
        assertThat(assets.get("ITUB4").getQuantity()).isEqualTo(200);
    }

    /**
     * TEST 9: Should parse CSV with fractional market ticker (F suffix)
     * 
     * Scenario: CSV contains PETR4F (fractional) and PETR4 (spot)
     * Expected: Both are consolidated into single PETR4 asset (F is removed)
     */
    @Test
    @DisplayName("Should consolidate fractional and spot market operations")
    void shouldConsolidateFractionalAndSpotMarket() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,fracionário,D+2,Clear Corretora,PETR4F,50,\"R$ 10,00\",\"R$ 500,00\"\n" +
            "20/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 12,00\",\"R$ 1.200,00\"";
        
        Path csvFile = createTempCsvFile("fractional_consolidation.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should consolidate PETR4F and PETR4 into single asset")
            .hasSize(1)
            .containsKey("PETR4");
        
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getOperations())
            .as("Should have 2 operations")
            .hasSize(2);
        
        // Total: 50@10 + 100@12 = 150 shares
        // Avg price: (500 + 1200) / 150 = 11.33...
        assertThat(petr4.getQuantity()).isEqualTo(150);
        assertThat(petr4.getAveragePrice())
            .isEqualByComparingTo(new BigDecimal("11.3333333333"));
    }

    // ========================================
    // PART 3: CURRENCY FORMATTING TESTS
    // ========================================

    /**
     * TEST 10: Should parse price with R$ prefix
     * 
     * Scenario: Price field contains "R$ 1.234,56"
     * Expected: Parsed as BigDecimal 1234.56
     */
    @Test
    @DisplayName("Should parse price with R$ prefix and Brazilian formatting")
    void shouldParsePriceWithBrazilianFormatting() throws Exception {
        // ARRANGE - Brazilian format: R$ 1.234,56 (thousands separator = dot, decimal = comma)
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 1.234,56\",\"R$ 123.456,00\"";
        
        Path csvFile = createTempCsvFile("brazilian_currency.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getAveragePrice())
            .as("Should parse R$ 1.234,56 as 1234.56")
            .isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    /**
     * TEST 11: Should not parse price without 2 quotes
     * 
     * Scenario: Price field is not wrapped in quotes
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should parse price without quotes")
    void shouldParsePriceWithoutQuotes() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,10,50,1050.00";
        
        Path csvFile = createTempCsvFile("price_no_quotes.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Line with no quotes price amount should throw CsvParseException")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Invalid price")
            .hasMessageContaining("expected to be enclosed in quotes")
            .hasMessageContaining("Amount received: \"10\"");

    }

    /**
     * TEST 12: Should parse price with spaces
     * 
     * Scenario: Price field contains spaces (e.g., "R$ 10,00 ")
     * Expected: Spaces are trimmed and price is parsed correctly
     */
    @Test
    @DisplayName("Should parse price with extra spaces")
    void shouldParsePriceWithSpaces() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\" R$ 10,00 \",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("price_with_spaces.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getAveragePrice())
            .as("Should parse ' R$ 10,00 ' as 10.00")
            .isEqualByComparingTo(new BigDecimal("10.00"));
    }

    // ========================================
    // PART 4: DATE PARSING TESTS
    // ========================================

    /**
     * TEST 13: Should parse date in dd/MM/yyyy format
     * 
     * Scenario: Date is in Brazilian format (15/01/2024)
     * Expected: Parsed as LocalDate(2024, 1, 15)
     */
    @Test
    @DisplayName("Should parse date in Brazilian format (dd/MM/yyyy)")
    void shouldParseDateInBrazilianFormat() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "31/12/2023,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("date_parsing.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        Operation operation = assets.get("PETR4").getOperations().get(0);
        assertThat(operation.getDate())
            .as("Should parse 31/12/2023 correctly")
            .isEqualTo(LocalDate.of(2023, 12, 31));
    }

    // ========================================
    // PART 5: OPERATION TYPE TESTS
    // ========================================

    /**
     * TEST 14: Should parse all operation types correctly
     * 
     * Scenario: CSV contains compra, venda, and posição operations
     * Expected: Each is mapped to correct OperationType enum
     */
    @Test
    @DisplayName("Should parse all operation types (compra, venda, posição)")
    void shouldParseAllOperationTypes() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "01/01/2024,posição,à vista,D+2,Clear Corretora,PETR4,50,\"R$ 8,00\",\"R$ 400,00\"\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"\n" +
            "20/02/2024,venda,à vista,D+2,Clear Corretora,PETR4,75,\"R$ 15,00\",\"R$ 1.125,00\"";
        
        Path csvFile = createTempCsvFile("operation_types.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        List<Operation> operations = assets.get("PETR4").getOperations();
        assertThat(operations).hasSize(3);
        
        assertThat(operations.get(0).getType())
            .as("First operation should be POSITION")
            .isEqualTo(OperationType.POSITION);
        
        assertThat(operations.get(1).getType())
            .as("Second operation should be BUY")
            .isEqualTo(OperationType.BUY);
        
        assertThat(operations.get(2).getType())
            .as("Third operation should be SELL")
            .isEqualTo(OperationType.SELL);
    }

    // ========================================
    // PART 6: MALFORMED CSV TESTS
    // ========================================

    /**
     * TEST 15: Should skip malformed lines (less than 9 fields)
     * 
     * Scenario: CSV contains a line with only 5 fields
     * Expected: Line is skipped, other valid lines are parsed
     */
    @Test
    @DisplayName("Should skip malformed lines with insufficient fields")
    void shouldSkipMalformedLines() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"\n" +
            "20/02/2024,compra,à vista,D+2,Clear Corretora\n" +  // Malformed: only 5 fields
            "25/02/2024,compra,à vista,D+2,Clear Corretora,VALE3,50,\"R$ 60,00\",\"R$ 3.000,00\"";
        
        Path csvFile = createTempCsvFile("malformed_lines.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should parse 2 valid lines, skip 1 malformed line")
            .hasSize(2)
            .containsKeys("PETR4", "VALE3");
        
        assertThat(assets.get("PETR4").getOperations()).hasSize(1);
        assertThat(assets.get("VALE3").getOperations()).hasSize(1);
    }

    /**
     * TEST 16: Should throw CsvParseException for invalid header
     * 
     * Scenario: CSV file starts with invalid header
     * Expected: CsvParseException thrown immediately
     */
    @Test
    @DisplayName("Should throw CsvParseException for invalid header in file")
    void shouldThrowExceptionForInvalidHeaderInFile() throws Exception {
        // ARRANGE
        String csvContent = 
            "Wrong,Header,Format\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("invalid_header.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should throw CsvParseException for invalid header")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Invalid header");
    }

    /**
     * TEST 17: Should throw CsvParseException for non-existent file
     * 
     * Scenario: File path does not exist
     * Expected: CsvParseException with "Error reading CSV file" message
     */
    @Test
    @DisplayName("Should throw CsvParseException for non-existent file")
    void shouldThrowExceptionForNonExistentFile() {
        // ARRANGE
        String nonExistentPath = "/path/to/non/existent/file.csv";
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(nonExistentPath))
            .as("Should throw CsvParseException for non-existent file")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Error reading CSV file");
    }

    /**
     * TEST 18: Should throw CsvParseException for invalid date format
     * 
     * Scenario: Date field has wrong format (yyyy-MM-dd instead of dd/MM/yyyy)
     * Expected: CsvParseException with "Error parsing CSV content"
     */
    @Test
    @DisplayName("Should throw CsvParseException for invalid date format")
    void shouldThrowExceptionForInvalidDateFormat() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "2024-01-15,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";  // Wrong format
        
        Path csvFile = createTempCsvFile("invalid_date.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should throw CsvParseException for invalid date format")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Error parsing CSV content");
    }

    /**
     * TEST 19: Should throw CsvParseException for invalid quantity
     * 
     * Scenario: Quantity field is not a valid integer
     * Expected: CsvParseException with "Error parsing CSV content"
     */
    @Test
    @DisplayName("Should throw CsvParseException for invalid quantity")
    void shouldThrowExceptionForInvalidQuantity() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,ABC,\"R$ 10,00\",\"R$ 1.000,00\"";  // "ABC" instead of number
        
        Path csvFile = createTempCsvFile("invalid_quantity.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should throw CsvParseException for invalid quantity")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Error parsing CSV content");
    }

    /**
     * TEST 20: Should throw CsvParseException for invalid price
     * 
     * Scenario: Price field is not a valid number
     * Expected: CsvParseException with "Error parsing CSV content"
     */
    @Test
    @DisplayName("Should throw CsvParseException for invalid price")
    void shouldThrowExceptionForInvalidPrice() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,INVALID,\"R$ 1.000,00\"";  // "INVALID" instead of number
        
        Path csvFile = createTempCsvFile("invalid_price.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should throw CsvParseException for invalid price")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Error parsing CSV content");
    }

    /**
     * TEST 21: Should throw CsvParseException for invalid operation type
     * 
     * Scenario: Operation type is not "compra", "venda", or "posição"
     * Expected: CsvParseException (wrapping IllegalArgumentException from OperationType.fromPortuguese)
     */
    @Test
    @DisplayName("Should throw CsvParseException for invalid operation type")
    void shouldThrowExceptionForInvalidOperationType() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,aluguel,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";  // "aluguel" is invalid
        
        Path csvFile = createTempCsvFile("invalid_operation_type.csv", csvContent);
        
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should throw CsvParseException for invalid operation type")
            .isInstanceOf(CsvParseException.class)
            .hasMessageContaining("Error parsing CSV content");
    }

    // ========================================
    // PART 7: EMPTY FILE TESTS
    // ========================================

    /**
     * TEST 22: Should return empty map for CSV with only header
     * 
     * Scenario: CSV file contains only header, no data rows
     * Expected: Returns empty map (no assets)
     */
    @Test
    @DisplayName("Should return empty map for CSV with only header")
    void shouldReturnEmptyMapForCsvWithOnlyHeader() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor";
        
        Path csvFile = createTempCsvFile("only_header.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should return empty map for CSV with only header")
            .isEmpty();
    }

    /**
     * TEST 23: Empty file should return empty asset map
     * 
     * Scenario: CSV file is completely empty (no header, no data)
     * Expected: An empty map is returned (no assets)
     */
    @Test
    @DisplayName("Should return an empty asset map (no assets) for completely empty file")
    void shouldReturnEmptyMapForEmptyFile() throws Exception {
        // ARRANGE
        String csvContent = "";
        Path csvFile = createTempCsvFile("empty.csv", csvContent);
        
        // ACT & ASSERT
        assertThat(CsvParser.parseTradesFromCsv(csvFile.toString()))
            .as("Should return empty map for completely empty file")
            .isEmpty();
            
    }

    // ========================================
    // PART 8: QUOTED FIELDS WITH COMMAS
    // ========================================

    /**
     * TEST 24: Should handle quoted fields containing commas
     * 
     * Scenario: Institution name contains comma (e.g., "Clear Corretora, CTVM")
     * Expected: Field is parsed correctly (comma inside quotes is not a delimiter)
     */
    @Test
    @DisplayName("Should handle quoted fields containing commas")
    void shouldHandleQuotedFieldsWithCommas() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,\"Clear Corretora, CTVM\",PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("quoted_with_commas.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getInstitution())
            .as("Institution name with comma should be parsed correctly")
            .isEqualTo("\"Clear Corretora, CTVM\"");
    }

    // ========================================
    // PART 9: TICKER CASE SENSITIVITY
    // ========================================

    /**
     * TEST 25: Should convert ticker to uppercase
     * 
     * Scenario: CSV contains lowercase ticker "petr4"
     * Expected: Ticker is converted to uppercase "PETR4"
     * 
     * Note: Operation constructor calls assetCode.toUpperCase()
     */
    @Test
    @DisplayName("Should convert ticker to uppercase")
    void shouldConvertTickerToUppercase() throws Exception {
        // ARRANGE
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "15/01/2024,compra,à vista,D+2,Clear Corretora,petr4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        
        Path csvFile = createTempCsvFile("lowercase_ticker.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should contain uppercase ticker")
            .containsKey("PETR4")
            .doesNotContainKey("petr4");
    }

    // ========================================
    // PART 10: LARGE FILE PERFORMANCE TEST
    // ========================================

    /**
     * TEST 26: Should handle large CSV file efficiently
     * 
     * Scenario: CSV contains 1000 operations
     * Expected: Parses successfully in reasonable time (< 5 seconds)
     * 
     * This is a basic performance test to ensure the parser scales.
     */
    @Test
    @DisplayName("Should handle large CSV file with 1000 operations")
    void shouldHandleLargeCsvFile() throws Exception {
        // ARRANGE
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n");
        
        // Generate 1000 operations
        for (int i = 1; i <= 1000; i++) {
            int day = (i % 28) + 1;  // Days 1-28
            int month = ((i / 28) % 12) + 1;  // Months 1-12
            String ticker = "ASSET" + (i % 10);  // 10 different assets
            csvContent.append(String.format("%02d/%02d/2024,compra,à vista,D+2,Clear Corretora,%s,100,\"R$ 10,00\",\"R$ 1.000,00\"\n",
                day, month, ticker));
        }
        
        Path csvFile = createTempCsvFile("large_file.csv", csvContent.toString());
        
        // ACT
        long startTime = System.currentTimeMillis();
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        long endTime = System.currentTimeMillis();
        
        // ASSERT
        assertThat(assets)
            .as("Should parse 1000 operations into 10 assets")
            .hasSize(10);
        
        long executionTime = endTime - startTime;
        assertThat(executionTime)
            .as("Should parse 1000 operations in less than 5 seconds")
            .isLessThan(5000);
        
        // Verify each asset has 100 operations
        assets.values().forEach(asset -> 
            assertThat(asset.getOperations())
                .as("Each asset should have 100 operations")
                .hasSize(100)
        );
    }

    // ========================================
    // PART 11: REAL-WORLD BROKER CSV FORMATS
    // ========================================

    /**
     * TEST 27: Should parse real Clear Corretora CSV format
     * 
     * This test uses a realistic CSV format exported from Clear broker.
     * Ensures compatibility with actual broker exports.
     */
    @Test
    @DisplayName("Should parse real Clear Corretora CSV format")
    void shouldParseRealClearCorretoraFormat() throws Exception {
        // ARRANGE - Realistic format from Clear
        String csvContent = 
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\n" +
            "02/01/2024,compra,à vista,D+2,Clear Corretora - Grupo XP,PETR4,100,\"R$ 38,45\",\"R$ 3.845,00\"\n" +
            "03/01/2024,compra,fracionário,D+2,Clear Corretora - Grupo XP,VALE3F,15,\"R$ 72,89\",\"R$ 1.093,35\"\n" +
            "05/01/2024,venda,à vista,D+2,Clear Corretora - Grupo XP,PETR4,50,\"R$ 39,12\",\"R$ 1.956,00\"";
        
        Path csvFile = createTempCsvFile("clear_format.csv", csvContent);
        
        // ACT
        Map<String, Asset> assets = CsvParser.parseTradesFromCsv(csvFile.toString());
        
        // ASSERT
        assertThat(assets)
            .as("Should parse Clear format correctly")
            .hasSize(2)
            .containsKeys("PETR4", "VALE3");
        
        Asset petr4 = assets.get("PETR4");
        assertThat(petr4.getQuantity()).isEqualTo(50);  // 100 bought, 50 sold
        assertThat(petr4.getInstitution()).isEqualTo("Clear Corretora - Grupo XP");
        
        Asset vale3 = assets.get("VALE3");
        assertThat(vale3.getQuantity()).isEqualTo(15);
        assertThat(vale3.getAveragePrice()).isEqualByComparingTo(new BigDecimal("72.89"));
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Helper method to create temporary CSV file for testing.
     * 
     * @param fileName Name of the CSV file
     * @param content Content to write to the file
     * @return Path to the created file
     * @throws IOException if file creation fails
     */
    private Path createTempCsvFile(String fileName, String content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, content);
        return filePath;
    }
}