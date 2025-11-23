package com.estevam.calculacoes.portfolio.dto;

import com.estevam.calculacoes.asset.Asset;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO representing a portfolio summary response.
 */
@Data
public class PortfolioSummary {
    private List<Asset> positions;
    private BigDecimal totalInvested; // Decided to let BigDecimal with all decimal places and client handle formatting
    private BigDecimal totalRealizedProfitLoss; // Decided to let BigDecimal with all decimal places and client handle formatting

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

    @Override
    public String toString() {
                return "PortfolioSummary{" +
                "positions=" + positions +
                ", totalInvested=" + totalInvested.setScale(2, RoundingMode.HALF_UP) +
                ", totalRealizedProfitLoss=" + totalRealizedProfitLoss.setScale(2, RoundingMode.HALF_UP) +
                '}';
    }

}