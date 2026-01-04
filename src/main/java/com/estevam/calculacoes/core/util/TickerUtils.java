package com.estevam.calculacoes.core.util;

/**
 * Utility class for handling stock ticker codes.
 */
public final class TickerUtils {

    private TickerUtils() {
        // Prevent instantiation
    }

    /**
     * Removes the 'F' suffix from fractional market codes to unify with spot market. Returns cleaned Uppercase ticker.
     * Example: "PETR4F" -> "PETR4"
     */
    public static String cleanTicker(String ticker) {
        if (ticker == null) {
            return null;
        }
        ticker = ticker.replace(" ", ""); // Remove all whitespace
        if (ticker.endsWith("F")) {
            return ticker.substring(0, ticker.length() - 1).toUpperCase();
        }
        return ticker.toUpperCase();
    }
}