package com.estevam.calculacoes.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TickerUtils Tests")
class TickerUtilsTest {

    @Test
    @DisplayName("Deve limpar ticker com sufixo F")
    void deveLimparTickerComF() {
        String tickerComF = "PETR4F";
        String tickerLimpo = TickerUtils.cleanTicker(tickerComF);

        assertThat(tickerLimpo)
        .as("Ticker após limpeza do F")
        .isEqualTo("PETR4");

    }

    @Test
    @DisplayName("Deve manter ticker sem sufixo F")
    void deveManterTickerSemF() {
        String tickerSemF = "VALE3";
        String tickerLimpo = TickerUtils.cleanTicker(tickerSemF);

        assertThat(tickerLimpo)
        .as("Ticker sem alteração (não possui F)")
        .isEqualTo("VALE3");

    }

    @Test
    @DisplayName("Deve limpar somente o último F de tickers com múltiplos F")
    void deveLimparSomenteUltimoF() {
        String tickerComF = "AFCF4F";
        String tickerLimpo = TickerUtils.cleanTicker(tickerComF);

        assertThat(tickerLimpo)
        .as("Ticker após limpeza do último F")
        .isEqualTo("AFCF4");

    }
    
}
