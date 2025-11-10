package com.estevam.calculacoes.operations;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Operation {
    
    private LocalDate date;
    private String assetCode;
    private String type;
    private int quantity;
    private BigDecimal price;

    public Operation(LocalDate date, String assetCode, String type, int quantity, BigDecimal price) {
        this.date = date;
        this.assetCode = assetCode.toUpperCase();
        this.type = type;
        this.quantity = quantity;
        this.price = price;
    }

}
