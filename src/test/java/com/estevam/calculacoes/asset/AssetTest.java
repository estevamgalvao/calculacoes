package com.estevam.calculacoes.asset;

import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para a classe Asset.
 * 
 * Objetivo: Garantir que os cálculos de preço médio, quantidade,
 * valor total e lucro/prejuízo realizado estejam corretos.
 */
@DisplayName("Asset - Testes de Cálculo de Posição")
class AssetTest {

    // ========================================
    // PARTE 1: SETUP - Preparação dos Testes
    // ========================================
    
    private Asset asset;
    
    /**
     * @BeforeEach é executado ANTES de cada método @Test.
     * 
     * Por quê? Para garantir que cada teste comece com um estado limpo,
     * sem interferência de outros testes (isolamento).
     * 
     * Aqui criamos um Asset "zerado" antes de cada teste.
     */
    @BeforeEach
    void setUp() {
        // Cria um Asset vazio para PETR4
        asset = new Asset("Petrobras PN", "PETR4", "Clear Corretora");
        
        // Neste ponto:
        // - averagePrice = 0
        // - quantity = 0
        // - totalValue = 0
        // - realizedProfitLoss = 0
        // - operations = lista vazia
    }

    // ========================================
    // PARTE 2: TESTES DE COMPRAS SIMPLES
    // ========================================
    
    /**
     * TESTE 1: Compra única
     * 
     * Cenário: Compro 100 ações a R$ 10,00 cada.
     * Esperado:
     * - Preço médio = R$ 10,00
     * - Quantidade = 100
     * - Valor total = R$ 1.000,00
     * - Lucro realizado = R$ 0,00 (ainda não vendi nada)
     */
    @Test
    @DisplayName("Deve calcular corretamente uma compra única")
    void deveCalcularCompraUnica() {
        // ARRANGE (Preparar) - Criar os dados de entrada
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),  // data
            "PETR4",                      // código do ativo
            OperationType.BUY,            // tipo: compra
            "à vista",                    // mercado
            new BigDecimal("100"),        // quantidade
            new BigDecimal("10.00")       // preço unitário
        );
        
        // ACT (Agir) - Executar a ação que queremos testar
        asset.addOperation(compra);
        
        // ASSERT (Verificar) - Conferir se o resultado é o esperado
        // assertThat é do AssertJ - muito mais legível que assertEquals
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio após compra única")  // mensagem se falhar
            .isEqualByComparingTo(new BigDecimal("10.00"));
        
        assertThat(asset.getQuantity())
            .as("Quantidade após compra única")
            .isEqualByComparingTo(new BigDecimal("100"));
        
        assertThat(asset.getTotalValue())
            .as("Valor total investido")
            .isEqualByComparingTo(new BigDecimal("1000.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro realizado (ainda não vendeu)")
            .isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    /**
     * TESTE 2: Múltiplas compras - Cálculo de preço médio
     * 
     * Cenário:
     * 1. Compro 100 ações a R$ 10,00 → total investido = R$ 1.000,00
     * 2. Compro 100 ações a R$ 20,00 → total investido = R$ 3.000,00
     * 
     * Preço médio = 3.000 / 200 = R$ 15,00
     * 
     * Este é um dos cálculos MAIS IMPORTANTES para IR!
     */
    @Test
    @DisplayName("Deve calcular preço médio corretamente em múltiplas compras")
    void deveCalcularPrecoMedioEmMultiplasCompras() {
        // ARRANGE
        Operation compra1 = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        
        Operation compra2 = new Operation(
            LocalDate.of(2024, 2, 20),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("20.00")
        );
        
        // ACT
        asset.addOperation(compra1);
        asset.addOperation(compra2);
        
        // ASSERT
        // Preço médio = (100 * 10 + 100 * 20) / 200 = 3000 / 200 = 15
        assertThat(asset.getAveragePrice())
            .as("Preço médio após duas compras")
            .isEqualByComparingTo(new BigDecimal("15.00"));
        
        assertThat(asset.getQuantity())
            .as("Quantidade total")
            .isEqualByComparingTo(new BigDecimal("200"));
        
        assertThat(asset.getTotalValue())
            .as("Valor total da posição (200 * 15)")
            .isEqualByComparingTo(new BigDecimal("3000.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Ainda não houve venda")
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ========================================
    // PARTE 3: TESTES DE VENDA - LUCRO/PREJUÍZO
    // ========================================
    
    /**
     * TESTE 3: Venda com lucro
     * 
     * Cenário:
     * 1. Compro 100 a R$ 10 → médio = R$ 10
     * 2. Compro 100 a R$ 20 → médio = R$ 15
     * 3. Vendo 100 a R$ 25
     * 
     * Lucro = (preço de venda - preço médio) * quantidade vendida
     *       = (25 - 15) * 100
     *       = R$ 1.000,00
     * 
     * Após a venda:
     * - Quantidade restante = 100
     * - Preço médio continua R$ 15 (das ações que sobraram)
     */
    @Test
    @DisplayName("Deve calcular lucro realizado corretamente em venda parcial")
    void deveCalcularLucroRealizadoEmVendaParcial() {
        // ARRANGE
        Operation compra1 = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        
        Operation compra2 = new Operation(
            LocalDate.of(2024, 2, 20),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("20.00")
        );
        
        Operation venda = new Operation(
            LocalDate.of(2024, 3, 10),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("25.00")
        );
        
        // ACT
        asset.addOperation(compra1);
        asset.addOperation(compra2);
        asset.addOperation(venda);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Quantidade após venda parcial (200 - 100)")
            .isEqualByComparingTo(new BigDecimal("100"));
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio permanece o mesmo das ações remanescentes")
            .isEqualByComparingTo(new BigDecimal("15.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro realizado: (25 - 15) * 100 = 1000")
            .isEqualByComparingTo(new BigDecimal("1000.00"));
        
        assertThat(asset.getTotalValue())
            .as("Valor da posição restante: 100 * 15 = 1500")
            .isEqualByComparingTo(new BigDecimal("1500.00"));
    }
    
    /**
     * TESTE 4: Venda com prejuízo
     * 
     * Cenário:
     * 1. Compro 100 a R$ 20 → médio = R$ 20
     * 2. Vendo 50 a R$ 15
     * 
     * Prejuízo = (15 - 20) * 50 = -R$ 250,00
     */
    @Test
    @DisplayName("Deve calcular prejuízo realizado corretamente")
    void deveCalcularPrejuizoRealizado() {
        // ARRANGE
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("20.00")
        );
        
        Operation venda = new Operation(
            LocalDate.of(2024, 2, 10),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("50"),
            new BigDecimal("15.00")
        );
        
        // ACT
        asset.addOperation(compra);
        asset.addOperation(venda);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Quantidade restante")
            .isEqualByComparingTo(new BigDecimal("50"));
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio permanece 20")
            .isEqualByComparingTo(new BigDecimal("20.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Prejuízo: (15 - 20) * 50 = -250")
            .isEqualByComparingTo(new BigDecimal("-250.00"));
    }
    
    /**
     * TESTE 5: Venda total (zera posição)
     * 
     * Cenário:
     * 1. Compro 100 a R$ 10
     * 2. Vendo 100 a R$ 15
     * 
     * Resultado:
     * - Quantidade = 0
     * - Lucro = R$ 500
     * - Preço médio = R$ 10 (permanece, mas não importa pois qty = 0)
     */
    @Test
    @DisplayName("Deve zerar posição ao vender tudo")
    void deveZerarPosicaoAoVenderTudo() {
        // ARRANGE
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        
        Operation venda = new Operation(
            LocalDate.of(2024, 2, 10),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("15.00")
        );
        
        // ACT
        asset.addOperation(compra);
        asset.addOperation(venda);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Posição zerada")
            .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro total: (15 - 10) * 100 = 500")
            .isEqualByComparingTo(new BigDecimal("500.00"));
        
        assertThat(asset.getTotalValue())
            .as("Valor total = 0 (não tem mais ações)")
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ========================================
    // PARTE 4: TESTES DE CASOS EXTREMOS (Edge Cases)
    // ========================================
    
    /**
     * TESTE 6: Venda excedendo quantidade disponível
     * 
     * Cenário problemático:
     * 1. Compro 50 a R$ 10
     * 2. Tento vender 100 (mais do que tenho!)
     * 
     * Comportamento esperado:
     * - Vende apenas as 50 disponíveis
     * - Calcula lucro sobre as 50
     * - Zera a posição
     * - Imprime warning no console
     */
    @Test
    @DisplayName("Deve ajustar venda quando excede quantidade disponível")
    void deveAjustarVendaQuandoExcedeQuantidade() {
        // ARRANGE
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("50"),
            new BigDecimal("10.00")
        );
        
        Operation vendaExcessiva = new Operation(
            LocalDate.of(2024, 2, 10),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("100"),  // Tentando vender mais do que tem!
            new BigDecimal("20.00")
        );
        
        // ACT
        asset.addOperation(compra);
        asset.addOperation(vendaExcessiva);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Posição deve ser zerada (não pode ficar negativa)")
            .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio zerado quando posição é zerada")
            .isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro calculado apenas sobre as 50 disponíveis: (20-10)*50 = 500")
            .isEqualByComparingTo(new BigDecimal("500.00"));
    }
    
    /**
     * TESTE 7: Operação do tipo POSITION (posição inicial)
     * 
     * Cenário: Você já tinha ações antes de começar a registrar.
     * Usa POSITION para informar a posição inicial.
     * 
     * 1. POSITION: 100 ações a R$ 12 (preço médio histórico)
     * 2. Compro mais 100 a R$ 18
     * 
     * Novo preço médio = (100*12 + 100*18) / 200 = 3000/200 = R$ 15
     */
    @Test
    @DisplayName("Deve processar operação do tipo POSITION corretamente")
    void deveProcessarOperacaoPosition() {
        // ARRANGE
        Operation posicaoInicial = new Operation(
            LocalDate.of(2023, 12, 31),
            "PETR4",
            OperationType.POSITION,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("12.00")  // preço médio histórico
        );
        
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("18.00")
        );
        
        // ACT
        asset.addOperation(posicaoInicial);
        asset.addOperation(compra);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Quantidade total")
            .isEqualByComparingTo(new BigDecimal("200"));
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio: (100*12 + 100*18)/200 = 15")
            .isEqualByComparingTo(new BigDecimal("15.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Nenhuma venda ainda")
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ========================================
    // PARTE 5: TESTES DE ORDENAÇÃO CRONOLÓGICA
    // ========================================
    
    /**
     * TESTE 8: Operações fora de ordem cronológica
     * 
     * Cenário: Adiciono operações em ordem aleatória.
     * O Asset deve ordená-las por data antes de calcular.
     * 
     * Ordem de adição:
     * 1. Venda em 10/03
     * 2. Compra em 15/01
     * 3. Compra em 20/02
     * 
     * Ordem cronológica correta:
     * 1. Compra em 15/01
     * 2. Compra em 20/02
     * 3. Venda em 10/03
     * 
     * O resultado deve ser o mesmo de adicionar em ordem.
     */
    @Test
    @DisplayName("Deve ordenar operações por data automaticamente")
    void deveOrdenarOperacoesPorData() {
        // ARRANGE - Criando operações em ordem "errada"
        Operation venda = new Operation(
            LocalDate.of(2024, 3, 10),  // Março
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("25.00")
        );
        
        Operation compra1 = new Operation(
            LocalDate.of(2024, 1, 15),  // Janeiro
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        
        Operation compra2 = new Operation(
            LocalDate.of(2024, 2, 20),  // Fevereiro
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("20.00")
        );
        
        // ACT - Adicionando fora de ordem
        asset.addOperation(venda);    // 3ª cronologicamente
        asset.addOperation(compra1);  // 1ª cronologicamente
        asset.addOperation(compra2);  // 2ª cronologicamente
        
        // ASSERT - Resultado deve ser igual ao teste "deveCalcularLucroRealizadoEmVendaParcial"
        assertThat(asset.getQuantity())
            .as("Quantidade final")
            .isEqualByComparingTo(new BigDecimal("100"));
        
        assertThat(asset.getAveragePrice())
            .as("Preço médio")
            .isEqualByComparingTo(new BigDecimal("15.00"));
        
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro realizado")
            .isEqualByComparingTo(new BigDecimal("1000.00"));
        
        // Verificando que as operações foram reordenadas
        assertThat(asset.getOperations())
            .as("Operações devem estar ordenadas por data")
            .extracting(Operation::getDate)
            .containsExactly(
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 2, 20),
                LocalDate.of(2024, 3, 10)
            );
    }

    // ========================================
    // PARTE 6: TESTES DE MÚLTIPLAS OPERAÇÕES (Cenário Real)
    // ========================================
    
    /**
     * TESTE 9: Cenário complexo - Múltiplas compras e vendas
     * 
     * Simula um histórico real de trading:
     * 1. Compra 100 a R$ 10 (médio = 10)
     * 2. Compra 50 a R$ 12 (médio = 10.67)
     * 3. Vende 75 a R$ 15 (lucro parcial)
     * 4. Compra 100 a R$ 8 (novo médio)
     * 5. Vende 50 a R$ 20 (lucro parcial)
     * 
     */
    @Test
    @DisplayName("Deve calcular corretamente cenário complexo com múltiplas operações")
    void deveTratarCenarioComplexoDeOperacoes() {
        // ARRANGE
        Operation op1 = new Operation(
            LocalDate.of(2024, 1, 10),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        // Após op1: qty=100, avg=10, total=1000, profit=0
        
        Operation op2 = new Operation(
            LocalDate.of(2024, 1, 20),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("50"),
            new BigDecimal("12.00")
        );
        // Após op2: qty=150, avg=(1000+600)/150=10.666666666666666, total=1600, profit=0
        
        Operation op3 = new Operation(
            LocalDate.of(2024, 2, 5),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("75"),
            new BigDecimal("15.00")
        );
        // Após op3: qty=75, avg=10.666666666666666, 
        // profit=(15-10.666666666666666)*75 = 4.3333...*75 = 325.00000000000006
        
        Operation op4 = new Operation(
            LocalDate.of(2024, 2, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("8.00")
        );
        // Após op4: qty=175
        // total_invested = 75*10.666666666666666 + 100*8 = 800 + 800 = 1600.0
        // avg = 1600/175 = 9.142857142857142
        
        Operation op5 = new Operation(
            LocalDate.of(2024, 3, 1),
            "PETR4",
            OperationType.SELL,
            "à vista",
            new BigDecimal("50"),
            new BigDecimal("20.00")
        );
        // Após op5: qty=125
        // profit_adicional = (20-9.142857142857142)*50 = 542.8571428571429
        // profit_total = 325.00000000000006 + 542.8571428571429 = 867.8571428571429
        
        // ACT
        asset.addOperation(op1);
        asset.addOperation(op2);
        asset.addOperation(op3);
        asset.addOperation(op4);
        asset.addOperation(op5);
        
        // ASSERT
        assertThat(asset.getQuantity())
            .as("Quantidade final: 100+50-75+100-50 = 125")
            .isEqualByComparingTo(new BigDecimal("125"));
        
        // Preço médio após todas as operações
        BigDecimal expectedAvgPrice = new BigDecimal("9.1428571429"); // 1600/175 antes da última venda
        assertThat(asset.getAveragePrice())
            .as("Preço médio final")
            .isEqualByComparingTo(expectedAvgPrice);
        
        // Lucro realizado total
        // Venda 1: (15 - 10.6666666667) * 75 = 324.999999997500044202070057508535683155059814453125
        // Venda 2: (20 - 9.1428571429) * 50 = 542.8571428549999744461729278555139899253845214843750
        // Total: 867.8571428525000186482429853640496730804443359375000
        BigDecimal expectedProfit = new BigDecimal("867.8571428525");
        assertThat(asset.getRealizedProfitLoss())
            .as("Lucro realizado total")
            .isEqualByComparingTo(expectedProfit);
    }

    // ========================================
    // PARTE 7: TESTES DE PRECISÃO (BigDecimal)
    // ========================================
    
    /**
     * TESTE 10: Precisão de BigDecimal
     * 
     * Testa se não há perda de precisão em cálculos com muitas casas decimais.
     * Importante para valores monetários!
     */
    @Test
    @DisplayName("Deve manter precisão em cálculos com BigDecimal")
    void deveManterPrecisaoEmCalculos() {
        // ARRANGE - Valores com muitas casas decimais
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("333"),
            new BigDecimal("10.333333")
        );
        
        // ACT
        asset.addOperation(compra);
        
        // ASSERT
        BigDecimal expectedTotal = new BigDecimal("10.333333")
            .multiply(new BigDecimal("333"));
        
        assertThat(asset.getTotalValue())
            .as("Valor total com precisão")
            .isEqualByComparingTo(expectedTotal);
    }

    // ========================================
    // PARTE 8: TESTE DO MÉTODO toString()
    // ========================================
    
    /**
     * TESTE 11: Método toString
     * 
     * Garante que o toString não quebra e contém informações básicas.
     * Útil para logs e debugging.
     */
    @Test
    @DisplayName("Método toString deve retornar representação válida")
    void toStringDeveRetornarRepresentacaoValida() {
        // ARRANGE
        Operation compra = new Operation(
            LocalDate.of(2024, 1, 15),
            "PETR4",
            OperationType.BUY,
            "à vista",
            new BigDecimal("100"),
            new BigDecimal("10.00")
        );
        asset.addOperation(compra);
        
        // ACT
        String resultado = asset.toString();
        
        // ASSERT
        assertThat(resultado)
            .as("toString deve conter informações básicas")
            .contains("PETR4")
            .contains("Petrobras PN")
            .contains("Clear Corretora")
            .contains("averagePrice=10.00")
            .containsAnyOf("quantity=100", "quantity=100.0");
    }
}