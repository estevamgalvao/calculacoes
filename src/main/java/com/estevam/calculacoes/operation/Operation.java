package com.estevam.calculacoes.operation;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class Operation {
    
    private LocalDate date;
    private String assetCode;
    private OperationType type;
    private String marketType;
    private int quantity;
    private BigDecimal price;

    public Operation(LocalDate date, String assetCode, OperationType type, String marketType, int quantity, BigDecimal price) {
        this.date = date;
        this.assetCode = assetCode.toUpperCase();
        this.type = type;
        this.marketType = marketType;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String toString() {
        return "Operation{" +
                "date=" + date +
                ", assetCode='" + assetCode + '\'' +
                ", type=" + type +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }

}
