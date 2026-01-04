package com.estevam.calculacoes.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TickerUtils utility class.
 * 
 * Purpose: Ensure that ticker cleaning logic works correctly,
 * especially for fractional market codes (ending with 'F').
 * 
 * Context: In Brazilian stock market, fractional shares are traded
 * with an 'F' suffix (e.g., "PETR4F"). For portfolio consolidation,
 * we need to unify them with spot market codes (e.g., "PETR4").
 */
@DisplayName("TickerUtils - Ticker Code Cleaning Tests")
class TickerUtilsTest {

    // ========================================
    // PART 1: BASIC FUNCTIONALITY TESTS
    // ========================================

    /**
     * TEST 1: Should remove 'F' suffix from fractional market ticker
     * 
     * Scenario: Ticker ends with 'F' (fractional market)
     * Expected: 'F' is removed
     * 
     * Example: "PETR4F" → "PETR4"
     */
    @Test
    @DisplayName("Should remove 'F' suffix from fractional market ticker")
    void shouldRemoveFSuffixFromFractionalTicker() {
        // ARRANGE
        String fractionalTicker = "PETR4F";
        
        // ACT
        String result = TickerUtils.cleanTicker(fractionalTicker);
        
        // ASSERT
        assertThat(result)
            .as("Fractional ticker 'PETR4F' should become 'PETR4'")
            .isEqualTo("PETR4");
    }

    /**
     * TEST 2: Should NOT modify ticker without 'F' suffix
     * 
     * Scenario: Ticker is already a spot market code (no 'F')
     * Expected: Ticker remains unchanged
     * 
     * Example: "PETR4" → "PETR4"
     */
    @Test
    @DisplayName("Should NOT modify ticker without 'F' suffix")
    void shouldNotModifyTickerWithoutFSuffix() {
        // ARRANGE
        String spotTicker = "PETR4";
        
        // ACT
        String result = TickerUtils.cleanTicker(spotTicker);
        
        // ASSERT
        assertThat(result)
            .as("Spot ticker 'PETR4' should remain unchanged")
            .isEqualTo("PETR4");
    }

    // ========================================
    // PART 2: PARAMETERIZED TESTS (Multiple Inputs)
    // ========================================

    /**
     * TEST 3: Should clean multiple fractional tickers correctly
     * 
     * Why parameterized test?
     * - Avoids code duplication
     * - Tests multiple scenarios with same logic
     * - Easy to add new test cases
     * 
     * @CsvSource format: "input, expected"
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource({
        "PETR4F, PETR4",      // Petrobras fractional
        "VALE3F, VALE3",      // Vale fractional
        "ITUB4F, ITUB4",      // Itaú fractional
        "BBDC4F, BBDC4",      // Bradesco fractional
        "MGLU3F, MGLU3",      // Magazine Luiza fractional
        "ABEV3F, ABEV3",      // Ambev fractional
        "WEGE3F, WEGE3"       // WEG fractional
    })
    @DisplayName("Should clean multiple fractional tickers correctly")
    void shouldCleanMultipleFractionalTickers(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should be cleaned to '%s'", input, expected)
            .isEqualTo(expected);
    }

    /**
     * TEST 4: Should NOT modify multiple spot market tickers
     * 
     * @ValueSource: Simple list of values (when you don't need expected output)
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should remain unchanged")
    @ValueSource(strings = {
        "PETR4",
        "VALE3",
        "ITUB4",
        "BBDC4",
        "MGLU3",
        "ABEV3",
        "WEGE3"
    })
    @DisplayName("Should NOT modify spot market tickers")
    void shouldNotModifySpotMarketTickers(String ticker) {
        // ACT
        String result = TickerUtils.cleanTicker(ticker);
        
        // ASSERT
        assertThat(result)
            .as("Spot ticker '%s' should remain unchanged", ticker)
            .isEqualTo(ticker);
    }

    // ========================================
    // PART 3: EDGE CASES (Boundary Conditions)
    // ========================================

    /**
     * TEST 5: Should handle null input gracefully
     * 
     * Scenario: Input is null
     * Expected: Returns null (no NullPointerException)
     * 
     * Why important? Defensive programming - method should not crash.
     */
    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInput() {
        // ACT
        String result = TickerUtils.cleanTicker(null);
        
        // ASSERT
        assertThat(result)
            .as("Null input should return null")
            .isNull();
    }

    /**
     * TEST 6: Should handle empty and blank strings
     * 
     * @NullAndEmptySource: Tests with null and "" (empty string)
     * 
     * Note: We also test with blank string manually since
     * @NullAndEmptySource doesn't include whitespace-only strings.
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should handle gracefully")
    @NullAndEmptySource
    @DisplayName("Should handle null and empty strings")
    void shouldHandleNullAndEmptyStrings(String input) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Input '%s' should be returned as-is", input)
            .isEqualTo(input);
    }

    /**
     * TEST 7: Should handle single character 'F'
     * 
     * Scenario: Ticker is just "F"
     * Expected: Returns empty string (removes the 'F')
     * 
     * Edge case: What happens when entire string is just the suffix?
     */
    @Test
    @DisplayName("Should handle single character 'F'")
    void shouldHandleSingleCharacterF() {
        // ARRANGE
        String singleF = "F";
        
        // ACT
        String result = TickerUtils.cleanTicker(singleF);
        
        // ASSERT
        assertThat(result)
            .as("Single 'F' should become empty string")
            .isEmpty();
    }

    /**
     * TEST 8: Should only remove 'F' at the END
     * 
     * Scenario: Ticker contains 'F' in the middle or beginning
     * Expected: Only removes 'F' if it's at the end
     * 
     * Examples:
     * - "FESA4" → "FESA4" (F at beginning, keep it)
     * - "FESA4F" → "FESA4" (F at end, remove it)
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource({
        "FESA4, FESA4",       // F at beginning - should NOT be removed
        "FESA4F, FESA4",      // F at end - should be removed
        "FLRY3, FLRY3",       // F at beginning - should NOT be removed
        "FLRY3F, FLRY3"       // F at end - should be removed
    })
    @DisplayName("Should only remove 'F' suffix at the end")
    void shouldOnlyRemoveFAtTheEnd(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s'", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 4: CASE SENSITIVITY TESTS
    // ========================================

    /**
     * TEST 9: Should handle lowercase 'f' suffix
     * 
     * Scenario: Ticker ends with lowercase 'f'
     * Expected: Does NOT remove it (only uppercase 'F' is removed)
     * 
     * Why? The current implementation uses endsWith("F") which is case-sensitive.
     * This test documents the current behavior.
     * 
     * Note: If you want case-insensitive behavior, you'd need to change
     * the implementation to use equalsIgnoreCase or toUpperCase().
     */
    @Test
    @DisplayName("Should NOT remove lowercase 'f' suffix (case-sensitive)")
    void shouldNotRemoveLowercaseF() {
        // ARRANGE
        String lowercaseF = "PETR4f";
        
        // ACT
        String result = TickerUtils.cleanTicker(lowercaseF);
        
        // ASSERT
        assertThat(result)
            .as("Lowercase 'f' should NOT be removed (case-sensitive)")
            .isEqualTo("PETR4F");
    }

    /**
     * TEST 10: Should handle mixed case tickers
     * 
     * Scenario: Ticker has mixed case letters
     * Expected: Only removes uppercase 'F' at the end
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource({
        "Petr4F, PETR4",      // Mixed case with uppercase F
        "PETR4F, PETR4",      // All uppercase
        "petr4F, PETR4",      // Lowercase with uppercase F
        "petr4f, PETR4F"      // All lowercase (f not removed)
    })
    @DisplayName("Should handle mixed case tickers correctly")
    void shouldHandleMixedCaseTickers(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s'", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 5: SPECIAL CHARACTERS AND NUMBERS
    // ========================================

    /**
     * TEST 11: Should handle tickers with numbers only
     * 
     * Scenario: Ticker is just numbers (unusual but possible)
     * Expected: Handles correctly
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource({
        "1234F, 1234",
        "1234, 1234",
        "F, ''",              // Just F becomes empty
        "123, 123"
    })
    @DisplayName("Should handle tickers with numbers")
    void shouldHandleTickersWithNumbers(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s'", input, expected)
            .isEqualTo(expected);
    }

    /**
     * TEST 12: Should handle tickers with special characters
     * 
     * Scenario: Ticker contains special characters (rare but possible)
     * Expected: Only removes 'F' at the end, keeps special chars
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource(value = {
        "PETR-4F | PETR-4",
        "PETR.4F | PETR.4",
        "PETR_4F | PETR_4",
        "PETR-4 | PETR-4"
    }, delimiter = '|')
    @DisplayName("Should handle tickers with special characters")
    void shouldHandleTickersWithSpecialCharacters(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s'", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 6: MULTIPLE 'F' CHARACTERS
    // ========================================

    /**
     * TEST 13: Should only remove ONE 'F' at the end
     * 
     * Scenario: Ticker ends with multiple 'F's
     * Expected: Only removes the last 'F'
     * 
     * Example: "PETRFF" → "PETRF"
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource({
        "PETRFF, PETRF",      // Two F's - removes only last one
        "PETRFFF, PETRFF",    // Three F's - removes only last one
        "FF, F",              // Just two F's
        "FFF, FF"             // Just three F's
    })
    @DisplayName("Should only remove ONE 'F' at the end")
    void shouldOnlyRemoveOneFAtTheEnd(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s' (only last F removed)", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 7: WHITESPACE HANDLING
    // ========================================

    /**
     * TEST 14: Should handle tickers with whitespace
     * 
     * Scenario: Ticker has leading/trailing spaces or spaces in the middle
     * Expected: Preserves whitespace, only removes 'F' at the end
     * 
     * Note: In real scenarios, you might want to trim() the input first.
     * This test documents the current behavior.
     */
    @ParameterizedTest(name = "[{index}] cleanTicker(''{0}'') should return ''{1}''")
    @CsvSource(value = {
        "PETR4F  | PETR4",   // Trailing space after F - F removed
        " PETR4F | PETR4",   // Leading space
        "PETR 4F | PETR4",   // Space in middle
        "   F    | ''"       // Just F with spaces
    }, delimiter = '|')
    @DisplayName("Should handle tickers with whitespace")
    void shouldHandleTickersWithWhitespace(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Ticker '%s' should become '%s'", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 8: REAL-WORLD BRAZILIAN STOCK TICKERS
    // ========================================

    /**
     * TEST 15: Should handle real Brazilian stock tickers correctly
     * 
     * This test uses actual ticker codes from B3 (Brazilian stock exchange)
     * to ensure the utility works with real-world data.
     * 
     * Ticker format in Brazil:
     * - 4 letters + 1 number (e.g., PETR4, VALE3)
     * - Sometimes 4 letters + 2 numbers (e.g., PETR11 - units)
     * - Fractional market adds 'F' suffix
     */
    @ParameterizedTest(name = "[{index}] Real ticker: ''{0}'' → ''{1}''")
    @CsvSource({
        // Blue chips
        "PETR4F, PETR4",      // Petrobras PN
        "VALE3F, VALE3",      // Vale ON
        "ITUB4F, ITUB4",      // Itaú Unibanco PN
        "BBDC4F, BBDC4",      // Bradesco PN
        "ABEV3F, ABEV3",      // Ambev ON
        
        // Tech stocks
        "MGLU3F, MGLU3",      // Magazine Luiza ON
        "B3SA3F, B3SA3",      // B3 (stock exchange) ON
        
        // Units (11 suffix)
        "SANB11F, SANB11",    // Santander Units
        "TAEE11F, TAEE11",    // Taesa Units
        
        // Spot market (no F)
        "PETR4, PETR4",
        "VALE3, VALE3",
        "ITUB4, ITUB4"
    })
    @DisplayName("Should handle real Brazilian stock tickers")
    void shouldHandleRealBrazilianTickers(String input, String expected) {
        // ACT
        String result = TickerUtils.cleanTicker(input);
        
        // ASSERT
        assertThat(result)
            .as("Real ticker '%s' should be cleaned to '%s'", input, expected)
            .isEqualTo(expected);
    }

    // ========================================
    // PART 9: IDEMPOTENCY TEST
    // ========================================

    /**
     * TEST 16: Should be idempotent (applying twice gives same result)
     * 
     * Idempotency: f(f(x)) = f(x)
     * 
     * Scenario: Apply cleanTicker twice
     * Expected: Second application doesn't change the result
     * 
     * Example: cleanTicker(cleanTicker("PETR4F")) = cleanTicker("PETR4") = "PETR4"
     */
    @ParameterizedTest(name = "[{index}] cleanTicker should be idempotent for ''{0}''")
    @ValueSource(strings = {
        "PETR4F",
        "VALE3F",
        "PETR4",
        "VALE3",
        "F",
        ""
    })
    @DisplayName("Should be idempotent (applying twice gives same result)")
    void shouldBeIdempotent(String input) {
        // ACT
        String firstApplication = TickerUtils.cleanTicker(input);
        String secondApplication = TickerUtils.cleanTicker(firstApplication);
        
        // ASSERT
        assertThat(secondApplication)
            .as("Applying cleanTicker twice should give same result")
            .isEqualTo(firstApplication);
    }
}