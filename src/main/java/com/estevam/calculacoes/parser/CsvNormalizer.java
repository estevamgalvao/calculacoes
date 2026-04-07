package com.estevam.calculacoes.parser;

import com.estevam.calculacoes.parser.exception.CsvParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * Normalizes raw CSV content into canonical format before parsing.
 * 
 * Canonical format:
 * - UTF-8 encoding, no BOM
 * - Comma (,) as delimiter
 * - Decimal fields (price, value) enclosed in quotes
 * - Fractional quantities enclosed in quotes
 * - Normalized line endings (\n)
 * 
 * Supported input formats:
 * - Comma-delimited with quoted decimals (Google Sheets export)
 * - Semicolon-delimited without quotes (Microsoft Excel export)
 * - Tab-delimited without quotes
 */
@Slf4j
public class CsvNormalizer {

    private static final char BOM = '\uFEFF';
    private static final char NON_BREAKING_SPACE = '\u00A0';

    private static final int EXPECTED_COLUMN_COUNT = 9;
    private static final int QUANTITY_INDEX = 6;
    private static final int PRICE_INDEX = 7;
    private static final int VALUE_INDEX = 8;


    /**
     * Normalizes raw CSV content into canonical format.
     *
     * @param rawContent the raw CSV content as string
     * @return String with content in canonical format
     * @throws CsvParseException if the content cannot be normalized
     */
    public static String normalize(String rawContent) throws CsvParseException {
        if (rawContent == null || rawContent.isBlank()) {
            throw new CsvParseException("CSV content is empty", null);
        }

        String content = removeBom(rawContent);
        content = normalizeLineEndings(content);
        content = replaceNonBreakingSpaces(content);

        char delimiter = detectDelimiter(content);
        log.debug("Detected delimiter: '{}'", delimiter == '\t' ? "\\t" : String.valueOf(delimiter));

        if (delimiter == ',') {
            content = normalizeCommaDelimited(content);
        } else {
            content = convertToCanonicalFormat(content, delimiter);
        }

        return content;
    }

    /**
     * Normalizes raw CSV content from byte array into canonical format.
     * Handles encoding by reading as UTF-8.
     *
     * @param rawBytes the raw CSV content as byte array
     * @return String with content in canonical format
     * @throws CsvParseException if the content cannot be normalized
     */
    public static String normalize(byte[] rawBytes) throws CsvParseException {
        if (rawBytes == null || rawBytes.length == 0) {
            throw new CsvParseException("CSV content is empty", null);
        }

        String rawContent = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
        return normalize(rawContent);
    }


    // ==================== Structural Cleanup ====================

    /**
     * Removes UTF-8 BOM character from the beginning of the content.
     */
    static String removeBom(String content) {
        if (!content.isEmpty() && content.charAt(0) == BOM) {
            log.debug("BOM character detected and removed.");
            return content.substring(1);
        }
        return content;
    }

    /**
     * Normalizes line endings to Unix format (\n).
     */
    static String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * Replaces non-breaking spaces (\u00A0) with regular spaces.
     */
    static String replaceNonBreakingSpaces(String content) {
        return content.replace(NON_BREAKING_SPACE, ' ');
    }


    // ==================== Delimiter Detection ====================

    /**
     * Detects the CSV delimiter by analyzing the header line.
     * Counts occurrences of each candidate delimiter and selects the one
     * that produces the expected number of columns (EXPECTED_COLUMN_COUNT - 1 separators).
     *
     * @param content the CSV content
     * @return the detected delimiter character
     * @throws CsvParseException if no valid delimiter is found
     */
    static char detectDelimiter(String content) throws CsvParseException {
        String headerLine = content.split("\n", 2)[0];

        // Count occurrences of each candidate delimiter in the header
        // Header fields don't contain special characters, so direct counting is safe
        int commaCount = countOccurrences(headerLine, ',');
        int semicolonCount = countOccurrences(headerLine, ';');
        int tabCount = countOccurrences(headerLine, '\t');

        int expectedSeparators = EXPECTED_COLUMN_COUNT - 1;

        if (semicolonCount == expectedSeparators) {
            return ';';
        }
        if (tabCount == expectedSeparators) {
            return '\t';
        }
        if (commaCount == expectedSeparators) {
            return ',';
        }

        throw new CsvParseException(
            String.format("Unable to detect CSV delimiter. Found %d commas, %d semicolons, %d tabs in header. Expected %d separators.",
                commaCount, semicolonCount, tabCount, expectedSeparators),
            null
        );
    }

    /**
     * Counts occurrences of a character in a string.
     */
    private static int countOccurrences(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }


    // ==================== Format Conversion ====================

    /**
     * Handles comma-delimited files that are already in canonical format.
     * Detects and fixes fractional quantities that were split across columns.
     *
     * Example: "GOAU4,246,66,\" R$9,00 \",\" R$2.219,94 \"" has 10 fields
     * because 246,66 (fractional quantity) was split by the comma delimiter.
     */
    private static String normalizeCommaDelimited(String content) throws CsvParseException {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (i == 0 || line.trim().isEmpty()) {
                result.append(line).append("\n");
                continue;
            }

            String[] fields = splitCsvLineWithQuotes(line);

            if (fields.length == EXPECTED_COLUMN_COUNT) {
                // Standard line, no fix needed
                result.append(line).append("\n");

            } else if (fields.length == EXPECTED_COLUMN_COUNT + 1) {
                // Likely a fractional quantity split: field[6]=integer part, field[7]=decimal part
                // Check if field[6] and field[7] are numeric to confirm
                if ( fields[QUANTITY_INDEX] != null
                    && fields[QUANTITY_INDEX].trim().matches("-?\\d+(\\.\\d+)?") 
                    && fields[QUANTITY_INDEX + 1] != null
                    && fields[QUANTITY_INDEX + 1].trim().matches("-?\\d+(\\.\\d+)?")) {
                    String mergedLine = mergeFractionalQuantity(fields);
                    result.append(mergedLine).append("\n");
                    log.debug("Line {}: merged fractional quantity fields.", i + 1);
                }
                else {
                    throw new CsvParseException(
                        String.format("Unexpected non-numeric fields at quantity indices in line %d: '%s' and '%s'. Line content: %s",
                            i + 1, fields[QUANTITY_INDEX].trim(), fields[QUANTITY_INDEX + 1].trim(), line),
                        null
                    );
                }


            } else {
                throw new CsvParseException(
                    String.format("Malformed CSV line %d: expected %d fields but found %d. Line content: %s",
                        i + 1, EXPECTED_COLUMN_COUNT, fields.length, line),
                    null
                );
            }
        }

        return removeTrailingNewline(result);
    }

    /**
     * Merges a fractional quantity that was split across two fields.
     * 
     * Input fields (10 elements):  [date, type, market, term, institution, ticker, intPart, decPart, price, value]
     * Output line (9 fields):      date,type,market,term,institution,ticker,"intPart,decPart",price,value
     */
    private static String mergeFractionalQuantity(String[] fields) {
        StringBuilder merged = new StringBuilder();

        for (int j = 0; j < fields.length; j++) {
            if (j == QUANTITY_INDEX) {
                // Merge field[6] and field[7] as quoted fractional quantity
                
                merged.append("\"").append(fields[j]).append(",").append(fields[j + 1]).append("\"");
                j++; // Skip the decimal part field
            } else {
                merged.append(fields[j]);
            }

            if (j < fields.length - 1) {
                merged.append(",");
            }
        }

        return merged.toString();
    }

    /**
     * Converts semicolon or tab-delimited content to canonical comma-delimited format.
     * Adds quotes around decimal fields (price, value) and fractional quantities.
     */
    private static String convertToCanonicalFormat(String content, char delimiter) throws CsvParseException {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().isEmpty()) {
                result.append("\n");
                continue;
            }

            String[] fields = line.split(String.valueOf(delimiter == '\t' ? "\t" : delimiter), -1);

            if (i == 0) {
                // Header line: simple delimiter replacement
                result.append(String.join(",", fields)).append("\n");
                continue;
            }

            if (fields.length != EXPECTED_COLUMN_COUNT) {
                throw new CsvParseException(
                    String.format("Malformed CSV line %d: expected %d fields but found %d. Line content: %s",
                        i + 1, EXPECTED_COLUMN_COUNT, fields.length, line),
                    null
                );
            }

            result.append(convertDataLineToCanonical(fields)).append("\n");
        }

        return removeTrailingNewline(result);
    }

    /**
     * Converts a single data line from non-comma format to canonical format.
     * Quotes fields that contain commas (decimal values) to prevent delimiter conflicts.
     */
    private static String convertDataLineToCanonical(String[] fields) {
        StringBuilder line = new StringBuilder();

        for (int j = 0; j < fields.length; j++) {
            String field = fields[j];

            if (j == QUANTITY_INDEX && field.contains(",")) {
                // Fractional quantity: wrap in quotes to preserve comma
                line.append("\"").append(field.trim()).append("\"");
            } else if (j == PRICE_INDEX || j == VALUE_INDEX) {
                // Price and value: wrap in quotes to preserve decimal comma
                line.append("\"").append(field.trim()).append("\"");
            } else {
                line.append(field);
            }

            if (j < fields.length - 1) {
                line.append(",");
            }
        }

        return line.toString();
    }


    // ==================== Utility ====================

    /**
     * Splits a CSV line respecting quoted fields.
     * Used for comma-delimited lines where fields may contain commas inside quotes.
     */
    private static String[] splitCsvLineWithQuotes(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    /**
     * Removes trailing newline from StringBuilder result.
     */
    private static String removeTrailingNewline(StringBuilder sb) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}