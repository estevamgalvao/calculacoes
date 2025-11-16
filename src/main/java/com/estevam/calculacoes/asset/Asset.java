package com.estevam.calculacoes.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.operation.OperationType;

import lombok.Data;

/**
 * Represents a consolidated asset with all its operations.
 * Automatically recalculates average price, quantity, and realized profit/loss.
 */
@Data
public class Asset {
    private String name;
    private String tradingCode;
    private String institution;
    private BigDecimal averagePrice;
    private int quantity;
    private BigDecimal totalValue;
    private BigDecimal realizedProfitLoss;
    private List<Operation> operations;

    public Asset(String name, String tradingCode, String institution) {
        this.name = name;
        this.tradingCode = tradingCode;
        this.institution = institution;
        this.averagePrice = BigDecimal.ZERO;
        this.quantity = 0;
        this.totalValue = BigDecimal.ZERO;
        this.realizedProfitLoss = BigDecimal.ZERO;
        this.operations = new ArrayList<>();
    }


    /**
     * Adds an operation and recalculates the asset position by date.
     */
    public void addOperation(Operation operation) {
        operations.add(operation);
        operations.sort(Comparator.comparing(Operation::getDate));
        recalculatePositionAndProfit();
    }

    /**
     * Recalculates average price, quantity, total value, and realized profit/loss.
     */
    private void recalculatePositionAndProfit() {
        BigDecimal avgPrice = BigDecimal.ZERO;
        int qty = 0;
        BigDecimal profitLoss = BigDecimal.ZERO;

        for (Operation op : operations) {
            if (op.getType() == OperationType.BUY) {
                // Adjust average price
                BigDecimal totalInvested = avgPrice.multiply(BigDecimal.valueOf(qty));
                totalInvested = totalInvested.add(op.getPrice().multiply(BigDecimal.valueOf(op.getQuantity())));
                qty += op.getQuantity();
                avgPrice = qty > 0 ? totalInvested.divide(BigDecimal.valueOf(qty), 10, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            } else if (op.getType() == OperationType.SELL) {
                // Calculate profit based on average price BEFORE the sale
                if (qty >= op.getQuantity()) {
                    BigDecimal profit = op.getPrice().subtract(avgPrice).multiply(BigDecimal.valueOf(op.getQuantity()));
                    profitLoss = profitLoss.add(profit);
                    qty -= op.getQuantity();
                } else {
                    // Selling more than available - adjust to zero position
                    System.out.println("Sale of " + op.getAssetCode() + " (" + op.getQuantity() + ") exceeds available quantity (" + qty + "). Adjusting to zero position.");
                    BigDecimal profit = op.getPrice().subtract(avgPrice).multiply(BigDecimal.valueOf(qty));
                    profitLoss = profitLoss.add(profit);
                    qty = 0;
                    avgPrice = BigDecimal.ZERO;
                }

            } else if (op.getType() == OperationType.POSITION) {
                // Initial position
                qty += op.getQuantity();
                avgPrice = op.getPrice();
            }
        }

        this.averagePrice = avgPrice;
        this.quantity = qty;
        this.totalValue = this.averagePrice.multiply(BigDecimal.valueOf(this.quantity));
        this.realizedProfitLoss = profitLoss;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "name='" + name + '\'' +
                ", tradingCode='" + tradingCode + '\'' +
                ", institution='" + institution + '\'' +
                ", averagePrice=" + averagePrice.setScale(2, RoundingMode.HALF_UP) +
                ", quantity=" + quantity +
                ", totalValue=" + totalValue.setScale(2, RoundingMode.HALF_UP) +
                ", realizedProfitLoss=" + realizedProfitLoss.setScale(2, RoundingMode.HALF_UP) +
                '}';
    }

    
}
