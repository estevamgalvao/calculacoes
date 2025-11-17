package com.estevam.calculacoes.portfolio.dto;

import com.estevam.calculacoes.asset.Asset;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO representing a portfolio summary response.
 */
@Data
public class PortfolioSummary {
    private List<Asset> positions;
    private BigDecimal totalInvested;
    private BigDecimal totalRealizedProfitLoss;

    public PortfolioSummary(Map<String, Asset> assets) {
        this.positions = assets.values().stream()
                .collect(Collectors.toList());
        
        this.totalInvested = assets.values().stream()
                .map(Asset::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalRealizedProfitLoss = assets.values().stream()
                .map(Asset::getRealizedProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}