package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.parser.exception.CsvParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CsvNormalizer class.
 *
 * Purpose: Ensure CSV normalization logic correctly handles:
 * - Structural cleanup (BOM, line endings, non-breaking spaces)
 * - Delimiter detection (comma, semicolon, tab)
 * - Comma-delimited files already in canonical format (passthrough)
 * - Fractional quantity fix for comma-delimited files (split field merge)
 * - Semicolon-delimited conversion to canonical format
 * - Tab-delimited conversion to canonical format
 * - Decimal field quoting (price, value)
 * - Fractional quantity quoting on non-comma formats
 * - Malformed lines (wrong field count)
 * - Empty or null content
 *
 * Context: CsvNormalizer is the entry point for all CSV content before
 * parsing. It guarantees that CsvParser always receives comma-delimited
 * content with quoted decimal fields, regardless of the source format.
 * Supported sources: Google Sheets (comma + quotes), Microsoft Excel (semicolon),
 * and tab-delimited exports.
 */
@DisplayName("CsvNormalizer - CSV Normalization Tests")
class CsvNormalizerTest {

    private static final String VALID_HEADER =
            "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento," +
            "Instituição,Código de Negociação,Quantidade,Preço,Valor";

    private static final String VALID_HEADER_SEMICOLON =
            "Data do Negócio;Tipo de Movimentação;Mercado;Prazo/Vencimento;" +
            "Instituição;Código de Negociação;Quantidade;Preço;Valor";

    private static final String VALID_HEADER_TAB =
            "Data do Negócio\tTipo de Movimentação\tMercado\tPrazo/Vencimento\t" +
            "Instituição\tCódigo de Negociação\tQuantidade\tPreço\tValor";

    // ====
    // PART 1: STRUCTURAL CLEANUP TESTS
    // ====

    /**
     * TEST 1: Should remove BOM character from the beginning of content
     *
     * Scenario: Content starts with UTF-8 BOM (\uFEFF), common in Windows exports
     * Expected: BOM is removed, content is otherwise unchanged
     */
    @Test
    @DisplayName("Should remove BOM character from content")
    void shouldRemoveBomCharacter() throws CsvParseException {
        // ARRANGE
        String contentWithBom = "\uFEFF" + VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(contentWithBom);

        // ASSERT
        assertThat(result)
                .as("BOM character should be removed from normalized content")
                .doesNotStartWith("\uFEFF")
                .startsWith("Data do Negócio");
    }

    /**
     * TEST 2: Should normalize Windows line endings to Unix format
     *
     * Scenario: Content uses Windows line endings (\r\n)
     * Expected: All \r\n are replaced with \n
     */
    @Test
    @DisplayName("Should normalize Windows line endings (\\r\\n) to Unix (\\n)")
    void shouldNormalizeWindowsLineEndings() throws CsvParseException {
        // ARRANGE
        String contentWithCrLf = VALID_HEADER + "\r\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(contentWithCrLf);

        // ASSERT
        assertThat(result)
                .as("Windows line endings should be replaced with Unix line endings")
                .doesNotContain("\r\n")
                .contains("\n");
    }

    /**
     * TEST 3: Should normalize old Mac line endings to Unix format
     *
     * Scenario: Content uses old Mac line endings (\r only)
     * Expected: All \r are replaced with \n
     */
    @Test
    @DisplayName("Should normalize old Mac line endings (\\r) to Unix (\\n)")
    void shouldNormalizeOldMacLineEndings() throws CsvParseException {
        // ARRANGE
        String contentWithCr = VALID_HEADER + "\r" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(contentWithCr);

        // ASSERT
        assertThat(result)
                .as("Old Mac line endings should be replaced with Unix line endings")
                .doesNotContain("\r")
                .contains("\n");
    }

    /**
     * TEST 4: Should replace non-breaking spaces with regular spaces
     *
     * Scenario: Content contains non-breaking spaces (\u00A0), common in copy-paste from web
     * Expected: All \u00A0 are replaced with regular spaces
     */
    @Test
    @DisplayName("Should replace non-breaking spaces (\\u00A0) with regular spaces")
    void shouldReplaceNonBreakingSpaces() throws CsvParseException {
        // ARRANGE - Non-breaking space inside a price field
        String contentWithNbsp = VALID_HEADER + "\n" +
                "15/01/2024,compra,à\u00A0vista,D+2,Clear Corretora,PETR4,100,\"R$\u00A010,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(contentWithNbsp);

        // ASSERT
        assertThat(result)
                .as("Non-breaking spaces should be replaced with regular spaces")
                .doesNotContain("\u00A0");
    }

    /**
     * TEST 5: Should apply all structural cleanups simultaneously
     *
     * Scenario: Content has BOM + Windows line endings + non-breaking spaces
     * Expected: All three are cleaned in a single normalize call
     */
    @Test
    @DisplayName("Should apply BOM removal, line ending normalization, and non-breaking space replacement together")
    void shouldApplyAllStructuralCleanupsSimultaneously() throws CsvParseException {
        // ARRANGE
        String dirtyContent = "\uFEFF" + VALID_HEADER + "\r\n" +
                "15/01/2024,compra,à\u00A0vista,D+2,Clear Corretora,PETR4,100,\"R$\u00A010,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(dirtyContent);

        // ASSERT
        assertThat(result)
                .as("All structural issues should be cleaned simultaneously")
                .doesNotStartWith("\uFEFF")
                .doesNotContain("\r")
                .doesNotContain("\u00A0");
    }

    // ====
    // PART 2: DELIMITER DETECTION TESTS
    // ====

    /**
     * TEST 6: Should detect comma as delimiter
     *
     * Scenario: Header contains 8 commas (9 columns)
     * Expected: Detected delimiter is comma
     */
    @Test
    @DisplayName("Should detect comma as delimiter")
    void shouldDetectCommaDelimiter() throws CsvParseException {
        // ARRANGE & ACT
        char delimiter = CsvNormalizer.detectDelimiter(VALID_HEADER);

        // ASSERT
        assertThat(delimiter)
                .as("Should detect comma as delimiter for Google Sheets format")
                .isEqualTo(',');
    }

    /**
     * TEST 7: Should detect semicolon as delimiter
     *
     * Scenario: Header contains 8 semicolons (9 columns)
     * Expected: Detected delimiter is semicolon
     */
    @Test
    @DisplayName("Should detect semicolon as delimiter")
    void shouldDetectSemicolonDelimiter() throws CsvParseException {
        // ARRANGE & ACT
        char delimiter = CsvNormalizer.detectDelimiter(VALID_HEADER_SEMICOLON);

        // ASSERT
        assertThat(delimiter)
                .as("Should detect semicolon as delimiter for Excel format")
                .isEqualTo(';');
    }

    /**
     * TEST 8: Should detect tab as delimiter
     *
     * Scenario: Header contains 8 tabs (9 columns)
     * Expected: Detected delimiter is tab
     */
    @Test
    @DisplayName("Should detect tab as delimiter")
    void shouldDetectTabDelimiter() throws CsvParseException {
        // ARRANGE & ACT
        char delimiter = CsvNormalizer.detectDelimiter(VALID_HEADER_TAB);

        // ASSERT
        assertThat(delimiter)
                .as("Should detect tab as delimiter for tab-delimited format")
                .isEqualTo('\t');
    }

    /**
     * TEST 9: Should throw CsvParseException when delimiter cannot be detected
     *
     * Scenario: Header has no recognizable delimiter pattern
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException when delimiter cannot be detected")
    void shouldThrowExceptionWhenDelimiterCannotBeDetected() {
        // ARRANGE - Header with pipe delimiter (unsupported)
        String pipeHeader = "Data do Negócio|Tipo de Movimentação|Mercado|Prazo/Vencimento|" +
                "Instituição|Código de Negociação|Quantidade|Preço|Valor";

        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.detectDelimiter(pipeHeader))
                .as("Unsupported delimiter should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Unable to detect CSV delimiter");
    }

    // ====
    // PART 3: COMMA-DELIMITED PASSTHROUGH TESTS
    // ====

    /**
     * TEST 10: Should pass through comma-delimited content unchanged (except structural cleanup)
     *
     * Scenario: Content is already in canonical format (comma + quotes)
     * Expected: Content is returned as-is after structural cleanup
     */
    @Test
    @DisplayName("Should pass through comma-delimited content in canonical format")
    void shouldPassThroughCommaDelimitedContent() throws CsvParseException {
        // ARRANGE
        String canonicalContent = VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(canonicalContent);

        // ASSERT
        assertThat(result)
                .as("Canonical comma-delimited content should pass through unchanged")
                .isEqualTo(canonicalContent);
    }

    /**
     * TEST 11: Should pass through comma-delimited content with multiple data lines
     *
     * Scenario: Multiple rows in canonical format
     * Expected: All rows preserved unchanged
     */
    @Test
    @DisplayName("Should pass through comma-delimited content with multiple data lines")
    void shouldPassThroughCommaDelimitedContentWithMultipleLines() throws CsvParseException {
        // ARRANGE
        String canonicalContent = VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"\n" +
                "20/02/2024,venda,à vista,D+2,Clear Corretora,PETR4,50,\"R$ 15,00\",\"R$ 750,00\"\n" +
                "01/03/2024,compra,à vista,D+2,Clear Corretora,VALE3,200,\"R$ 72,89\",\"R$ 14.578,00\"";

        // ACT
        String result = CsvNormalizer.normalize(canonicalContent);

        // ASSERT
        assertThat(result)
                .as("All rows should be preserved in passthrough")
                .isEqualTo(canonicalContent);
    }

    /**
     * TEST 12: Should fix fractional quantity split across two fields in comma-delimited content
     *
     * Scenario: Quantity "246,66" was split by the comma delimiter into two fields,
     *           resulting in 10 fields instead of 9 (Google Sheets export edge case)
     * Expected: The two fields are merged and wrapped in quotes: "246,66"
     */
    @Test
    @DisplayName("Should merge fractional quantity split across two fields in comma-delimited content")
    void shouldMergeFractionalQuantitySplitInCommaDelimited() throws CsvParseException {
        // ARRANGE - 10 fields: quantity "246,66" was split into "246" and "66"
        String contentWithSplitQuantity = VALID_HEADER + "\n" +
                "31/12/2023,posição,fracionário,D+2,XP Investimentos,MCHF11,246,66,\"R$ 9,18\",\"R$ 2.148,12\"";

        // ACT
        String result = CsvNormalizer.normalize(contentWithSplitQuantity);

        // ASSERT
        assertThat(result)
                .as("Fractional quantity should be merged and quoted")
                .contains("\"246,66\"")
                .doesNotContain(",246,66,");
    }

    /**
     * TEST 13: Should throw CsvParseException for comma-delimited line with unexpected field count
     *
     * Scenario: A data line has 8 fields (too few) in comma-delimited content
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for comma-delimited line with unexpected field count")
    void shouldThrowExceptionForCommaDelimitedLineWithUnexpectedFieldCount() {
        // ARRANGE - Only 8 fields (missing last column)
        String malformedContent = VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\"";

        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize(malformedContent))
                .as("Line with wrong field count should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Malformed CSV line");
    }

    // ====
    // PART 4: SEMICOLON-DELIMITED CONVERSION TESTS
    // ====

    /**
     * TEST 14: Should convert semicolon-delimited header to comma-delimited
     *
     * Scenario: Header uses semicolons (Excel export)
     * Expected: Header is converted to comma-delimited
     */
    @Test
    @DisplayName("Should convert semicolon-delimited header to comma-delimited")
    void shouldConvertSemicolonHeader() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100;R$ 10,00;R$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);

        // ASSERT
        assertThat(result.split("\n")[0])
                .as("Header should be converted to comma-delimited")
                .isEqualTo(VALID_HEADER);
    }

    /**
     * TEST 15: Should wrap price field in quotes when converting from semicolon format
     *
     * Scenario: Price "R$ 10,00" in semicolon format has no quotes
     * Expected: After conversion, price is wrapped in quotes: "R$ 10,00"
     */
    @Test
    @DisplayName("Should wrap price field in quotes when converting from semicolon format")
    void shouldWrapPriceInQuotesWhenConvertingFromSemicolon() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100;R$ 10,00;R$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);

        // ASSERT
        assertThat(result)
                .as("Price field should be wrapped in quotes after semicolon conversion")
                .contains("\"R$ 10,00\"");
    }

    /**
     * TEST 16: Should wrap value field in quotes when converting from semicolon format
     *
     * Scenario: Value "R$ 1.000,00" in semicolon format has no quotes
     * Expected: After conversion, value is wrapped in quotes: "R$ 1.000,00"
     */
    @Test
    @DisplayName("Should wrap value field in quotes when converting from semicolon format")
    void shouldWrapValueInQuotesWhenConvertingFromSemicolon() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100;R$ 10,00;R$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);

        // ASSERT
        assertThat(result)
                .as("Value field should be wrapped in quotes after semicolon conversion")
                .contains("\"R$ 1.000,00\"");
    }

    /**
     * TEST 17: Should wrap fractional quantity in quotes when converting from semicolon format
     *
     * Scenario: Quantity "246,66" in semicolon format contains a comma
     * Expected: After conversion, fractional quantity is wrapped in quotes: "246,66"
     */
    @Test
    @DisplayName("Should wrap fractional quantity in quotes when converting from semicolon format")
    void shouldWrapFractionalQuantityInQuotesWhenConvertingFromSemicolon() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "31/12/2023;posição;fracionário;D+2;XP Investimentos;MCHF11;246,66;R$ 9,18;R$ 2.148,12";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);

        // ASSERT
        assertThat(result)
                .as("Fractional quantity should be wrapped in quotes after semicolon conversion")
                .contains("\"246,66\"");
    }

    /**
     * TEST 18: Should not wrap integer quantity in quotes when converting from semicolon format
     *
     * Scenario: Quantity "100" in semicolon format is a plain integer
     * Expected: After conversion, integer quantity is NOT wrapped in quotes
     */
    @Test
    @DisplayName("Should not wrap integer quantity in quotes when converting from semicolon format")
    void shouldNotWrapIntegerQuantityInQuotesWhenConvertingFromSemicolon() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100;R$ 10,00;R$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);
        String dataLine = result.split("\n")[1];

        // ASSERT
        // Quantity is at index 6 after splitting by comma (respecting quotes)
        String[] fields = dataLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        assertThat(fields[6])
                .as("Integer quantity should not be wrapped in quotes")
                .isEqualTo("100");
    }

    /**
     * TEST 19: Should convert multiple semicolon-delimited data lines
     *
     * Scenario: CSV has header + 3 data lines in semicolon format
     * Expected: All lines are correctly converted to canonical format
     */
    @Test
    @DisplayName("Should convert multiple semicolon-delimited data lines")
    void shouldConvertMultipleSemicolonDelimitedLines() throws CsvParseException {
        // ARRANGE
        String semicolonContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100;R$ 10,00;R$ 1.000,00\n" +
                "20/02/2024;venda;à vista;D+2;Clear Corretora;PETR4;50;R$ 15,00;R$ 750,00\n" +
                "01/03/2024;compra;à vista;D+2;Clear Corretora;VALE3;200;R$ 72,89;R$ 14.578,00";

        // ACT
        String result = CsvNormalizer.normalize(semicolonContent);
        String[] lines = result.split("\n");

        // ASSERT
        assertThat(lines)
                .as("Should produce header + 3 data lines")
                .hasSize(4);

        assertThat(lines[0])
                .as("Header should be comma-delimited")
                .isEqualTo(VALID_HEADER);

        // All data lines should contain quoted price fields
        for (int i = 1; i < lines.length; i++) {
            assertThat(lines[i])
                    .as("Data line %d should contain quoted decimal fields", i)
                    .containsPattern("\"R\\$.*,\\d{2}\"");
        }
    }

    /**
     * TEST 20: Should throw CsvParseException for semicolon-delimited line with wrong field count
     *
     * Scenario: A data line has 7 fields instead of 9 in semicolon format
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for semicolon-delimited line with wrong field count")
    void shouldThrowExceptionForSemicolonLineWithWrongFieldCount() {
        // ARRANGE - Only 7 fields (missing price and value)
        String malformedContent = VALID_HEADER_SEMICOLON + "\n" +
                "15/01/2024;compra;à vista;D+2;Clear Corretora;PETR4;100";

        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize(malformedContent))
                .as("Semicolon line with wrong field count should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Malformed CSV line");
    }

    // ====
    // PART 5: TAB-DELIMITED CONVERSION TESTS
    // ====

    /**
     * TEST 21: Should convert tab-delimited header to comma-delimited
     *
     * Scenario: Header uses tabs as delimiter
     * Expected: Header is converted to comma-delimited
     */
    @Test
    @DisplayName("Should convert tab-delimited header to comma-delimited")
    void shouldConvertTabDelimitedHeader() throws CsvParseException {
        // ARRANGE
        String tabContent = VALID_HEADER_TAB + "\n" +
                "15/01/2024\tcompra\tà vista\tD+2\tClear Corretora\tPETR4\t100\tR$ 10,00\tR$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(tabContent);

        // ASSERT
        assertThat(result.split("\n")[0])
                .as("Tab-delimited header should be converted to comma-delimited")
                .isEqualTo(VALID_HEADER);
    }

    /**
     * TEST 22: Should wrap price and value in quotes when converting from tab format
     *
     * Scenario: Price and value fields in tab format contain commas but no quotes
     * Expected: After conversion, both fields are wrapped in quotes
     */
    @Test
    @DisplayName("Should wrap price and value in quotes when converting from tab format")
    void shouldWrapDecimalFieldsInQuotesWhenConvertingFromTab() throws CsvParseException {
        // ARRANGE
        String tabContent = VALID_HEADER_TAB + "\n" +
                "15/01/2024\tcompra\tà vista\tD+2\tClear Corretora\tPETR4\t100\tR$ 10,00\tR$ 1.000,00";

        // ACT
        String result = CsvNormalizer.normalize(tabContent);

        // ASSERT
        assertThat(result)
                .as("Price field should be wrapped in quotes after tab conversion")
                .contains("\"R$ 10,00\"")
                .contains("\"R$ 1.000,00\"");
    }

    /**
     * TEST 23: Should wrap fractional quantity in quotes when converting from tab format
     *
     * Scenario: Quantity "246,66" in tab format contains a comma
     * Expected: After conversion, fractional quantity is wrapped in quotes
     */
    @Test
    @DisplayName("Should wrap fractional quantity in quotes when converting from tab format")
    void shouldWrapFractionalQuantityInQuotesWhenConvertingFromTab() throws CsvParseException {
        // ARRANGE
        String tabContent = VALID_HEADER_TAB + "\n" +
                "31/12/2023\tposição\tfracionário\tD+2\tXP Investimentos\tMCHF11\t246,66\tR$ 9,18\tR$ 2.148,12";

        // ACT
        String result = CsvNormalizer.normalize(tabContent);

        // ASSERT
        assertThat(result)
                .as("Fractional quantity should be wrapped in quotes after tab conversion")
                .contains("\"246,66\"");
    }

    /**
     * TEST 24: Should throw CsvParseException for tab-delimited line with wrong field count
     *
     * Scenario: A data line has 5 fields instead of 9 in tab format
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for tab-delimited line with wrong field count")
    void shouldThrowExceptionForTabLineWithWrongFieldCount() {
        // ARRANGE - Only 5 fields
        String malformedContent = VALID_HEADER_TAB + "\n" +
                "15/01/2024\tcompra\tà vista\tD+2\tClear Corretora";

        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize(malformedContent))
                .as("Tab line with wrong field count should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Malformed CSV line");
    }

    // ====
    // PART 6: NORMALIZED CSV RECORD TESTS
    // ====

    /**
     * TEST 25: Should return String with correct content
     *
     * Scenario: Valid canonical content is normalized
     * Expected: String record wraps the content correctly
     */
    @Test
    @DisplayName("Should return String record with correct content")
    void shouldReturnStringRecordWithCorrectContent() throws CsvParseException {
        // ARRANGE
        String canonicalContent = VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";

        // ACT
        String result = CsvNormalizer.normalize(canonicalContent);

        // ASSERT
        assertThat(result)
                .as("Should return a non-null String instance")
                .isNotNull();

        assertThat(result)
                .as("String content should match the normalized string")
                .isEqualTo(canonicalContent);
    }


    // DELETED TESTS 26 AND 27: These tests were removed because they were redundant with the more specific content validation tests in PARTS 3, 4, and 5. 
    // The content validation is already thoroughly covered in those sections, so these general content tests did not add additional value.

    // ====
    // PART 7: EMPTY AND NULL CONTENT TESTS
    // ====

    /**
     * TEST 28: Should throw CsvParseException for null string content
     *
     * Scenario: normalize(String) is called with null
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for null string content")
    void shouldThrowExceptionForNullStringContent() {
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize((String) null))
                .as("Null string content should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("CSV content is empty");
    }

    /**
     * TEST 29: Should throw CsvParseException for blank string content
     *
     * Scenario: normalize(String) is called with blank string
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for blank string content")
    void shouldThrowExceptionForBlankStringContent() {
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize("   "))
                .as("Blank string content should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("CSV content is empty");
    }

    /**
     * TEST 30: Should throw CsvParseException for null byte array content
     *
     * Scenario: normalize(byte[]) is called with null
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for null byte array content")
    void shouldThrowExceptionForNullByteArrayContent() {
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize((byte[]) null))
                .as("Null byte array content should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("CSV content is empty");
    }

    /**
     * TEST 31: Should throw CsvParseException for empty byte array content
     *
     * Scenario: normalize(byte[]) is called with empty array
     * Expected: CsvParseException with descriptive message
     */
    @Test
    @DisplayName("Should throw CsvParseException for empty byte array content")
    void shouldThrowExceptionForEmptyByteArrayContent() {
        // ACT & ASSERT
        assertThatThrownBy(() -> CsvNormalizer.normalize(new byte[0]))
                .as("Empty byte array content should throw CsvParseException")
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("CSV content is empty");
    }

    // ====
    // PART 8: BYTE ARRAY INPUT TESTS
    // ====

    /**
     * TEST 32: Should normalize content from byte array (UTF-8)
     *
     * Scenario: normalize(byte[]) is called with valid UTF-8 bytes
     * Expected: Content is normalized correctly, same as normalize(String)
     */
    @Test
    @DisplayName("Should normalize content from byte array (UTF-8)")
    void shouldNormalizeContentFromByteArray() throws CsvParseException {
        // ARRANGE
        String canonicalContent = VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        byte[] bytes = canonicalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // ACT
        String result = CsvNormalizer.normalize(bytes);

        // ASSERT
        assertThat(result)
                .as("Byte array normalization should produce same result as string normalization")
                .isEqualTo(canonicalContent);
    }

    /**
     * TEST 33: Should remove BOM from byte array content
     *
     * Scenario: Byte array starts with UTF-8 BOM bytes (0xEF, 0xBB, 0xBF)
     * Expected: BOM is removed after normalization
     */
    @Test
    @DisplayName("Should remove BOM from byte array content")
    void shouldRemoveBomFromByteArray() throws CsvParseException {
        // ARRANGE - UTF-8 BOM is represented as \uFEFF when decoded
        String contentWithBom = "\uFEFF" + VALID_HEADER + "\n" +
                "15/01/2024,compra,à vista,D+2,Clear Corretora,PETR4,100,\"R$ 10,00\",\"R$ 1.000,00\"";
        byte[] bytes = contentWithBom.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // ACT
        String result = CsvNormalizer.normalize(bytes);

        // ASSERT
        assertThat(result)
                .as("BOM should be removed from byte array content")
                .doesNotStartWith("\uFEFF")
                .startsWith("Data do Negócio");
    }

    // ====
    // PART 9: REAL-WORLD FORMAT TESTS
    // ====

    /**
     * TEST 34: Should normalize real Google Sheets export format
     *
     * Scenario: Full CSV as exported by Google Sheets (comma + quotes + BOM + CRLF)
     * Expected: Content is cleaned and passed through as canonical format
     */
    @Test
    @DisplayName("Should normalize real Google Sheets export format")
    void shouldNormalizeRealGoogleSheetsFormat() throws CsvParseException {
        // ARRANGE - Google Sheets: BOM + CRLF + comma + quoted decimals
        String googleSheetsContent = "\uFEFF" +
                "Data do Negócio,Tipo de Movimentação,Mercado,Prazo/Vencimento,Instituição,Código de Negociação,Quantidade,Preço,Valor\r\n" +
                "02/01/2024,compra,à vista,D+2,Clear Corretora - Grupo XP,PETR4,100,\"R$ 38,45\",\"R$ 3.845,00\"\r\n" +
                "03/01/2024,compra,fracionário,D+2,Clear Corretora - Grupo XP,VALE3F,15,\"R$ 72,89\",\"R$ 1.093,35\"";

        // ACT
        String result = CsvNormalizer.normalize(googleSheetsContent);

        // ASSERT
        assertThat(result)
                .as("Google Sheets format should be cleaned and preserved as canonical")
                .doesNotStartWith("\uFEFF")
                .doesNotContain("\r")
                .contains("\"R$ 38,45\"")
                .contains("\"R$ 72,89\"");
    }

    /**
     * TEST 35: Should normalize real Microsoft Excel export format
     *
     * Scenario: Full CSV as exported by Microsoft Excel (semicolon + no quotes)
     * Expected: Content is converted to canonical format with quoted decimal fields
     */
    @Test
    @DisplayName("Should normalize real Microsoft Excel export format")
    void shouldNormalizeRealMicrosoftExcelFormat() throws CsvParseException {
        // ARRANGE - Excel: semicolon delimiter, no quotes around decimals
        String excelContent =
                "Data do Negócio;Tipo de Movimentação;Mercado;Prazo/Vencimento;Instituição;Código de Negociação;Quantidade;Preço;Valor\n" +
                "02/01/2024;compra;à vista;D+2;Clear Corretora - Grupo XP;PETR4;100;R$ 38,45;R$ 3.845,00\n" +
                "03/01/2024;compra;fracionário;D+2;Clear Corretora - Grupo XP;VALE3F;15;R$ 72,89;R$ 1.093,35";

        // ACT
        String result = CsvNormalizer.normalize(excelContent);
        String[] lines = result.split("\n");

        // ASSERT
        assertThat(lines[0])
                .as("Header should be converted to comma-delimited")
                .isEqualTo(VALID_HEADER);

        assertThat(result)
                .as("Price fields should be quoted after Excel conversion")
                .contains("\"R$ 38,45\"")
                .contains("\"R$ 3.845,00\"")
                .contains("\"R$ 72,89\"")
                .contains("\"R$ 1.093,35\"");
    }

    /**
     * TEST 36: Should normalize Excel export with fractional quantity
     *
     * Scenario: Excel CSV contains a fractional quantity (e.g., 246,66 for FII)
     * Expected: Fractional quantity is wrapped in quotes in canonical format
     */
    @Test
    @DisplayName("Should normalize Excel export with fractional quantity")
    void shouldNormalizeExcelExportWithFractionalQuantity() throws CsvParseException {
        // ARRANGE
        String excelContent =
                "Data do Negócio;Tipo de Movimentação;Mercado;Prazo/Vencimento;Instituição;Código de Negociação;Quantidade;Preço;Valor\n" +
                "31/12/2023;posição;fracionário;D+2;XP Investimentos;MCHF11;246,66;R$ 9,18;R$ 2.148,12";

        // ACT
        String result = CsvNormalizer.normalize(excelContent);

        // ASSERT
        assertThat(result)
                .as("Fractional quantity from Excel should be quoted in canonical format")
                .contains("\"246,66\"")
                .contains("\"R$ 9,18\"")
                .contains("\"R$ 2.148,12\"");
    }

    // ====
    // PART 10: INTERNAL HELPER METHOD TESTS
    // ====

    /**
     * TEST 37: Should remove BOM via removeBom helper
     *
     * Scenario: String starts with BOM character
     * Expected: BOM is removed, rest of string is unchanged
     */
    @Test
    @DisplayName("Should remove BOM via removeBom helper method")
    void shouldRemoveBomViaHelperMethod() {
        // ARRANGE
        String withBom = "\uFEFFHello, World!";

        // ACT
        String result = CsvNormalizer.removeBom(withBom);

        // ASSERT
        assertThat(result)
                .as("removeBom should remove leading BOM character")
                .isEqualTo("Hello, World!");
    }

    /**
     * TEST 38: Should return string unchanged via removeBom when no BOM present
     *
     * Scenario: String does not start with BOM character
     * Expected: String is returned unchanged
     */
    @Test
    @DisplayName("Should return string unchanged via removeBom when no BOM present")
    void shouldReturnUnchangedStringWhenNoBomPresent() {
        // ARRANGE
        String withoutBom = "Hello, World!";

        // ACT
        String result = CsvNormalizer.removeBom(withoutBom);

        // ASSERT
        assertThat(result)
                .as("removeBom should not modify string without BOM")
                .isEqualTo("Hello, World!");
    }

    /**
     * TEST 39: Should normalize line endings via normalizeLineEndings helper
     *
     * Scenario: String contains mixed line endings (\r\n and \r)
     * Expected: All line endings are replaced with \n
     */
    @Test
    @DisplayName("Should normalize mixed line endings via normalizeLineEndings helper")
    void shouldNormalizeMixedLineEndingsViaHelperMethod() {
        // ARRANGE
        String mixedLineEndings = "line1\r\nline2\rline3\nline4";

        // ACT
        String result = CsvNormalizer.normalizeLineEndings(mixedLineEndings);

        // ASSERT
        assertThat(result)
                .as("All line endings should be normalized to \\n")
                .isEqualTo("line1\nline2\nline3\nline4");
    }

    /**
     * TEST 40: Should replace non-breaking spaces via replaceNonBreakingSpaces helper
     *
     * Scenario: String contains non-breaking spaces (\u00A0)
     * Expected: All \u00A0 are replaced with regular spaces
     */
    @Test
    @DisplayName("Should replace non-breaking spaces via replaceNonBreakingSpaces helper")
    void shouldReplaceNonBreakingSpacesViaHelperMethod() {
        // ARRANGE
        String withNbsp = "R$\u00A010,00";

        // ACT
        String result = CsvNormalizer.replaceNonBreakingSpaces(withNbsp);

        // ASSERT
        assertThat(result)
                .as("Non-breaking spaces should be replaced with regular spaces")
                .isEqualTo("R$ 10,00")
                .doesNotContain("\u00A0");
    }
}